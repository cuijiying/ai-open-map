package com.aomap.ingest.repo;

import com.aomap.common.geo.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 把某一省的数据发布到 {@code osm_feature}，同时让切片查询继续读已发布的分区。
 * 几何和索引都建在旁路表 {@code osm_feature_load} 上，最后用一次短事务摘掉旧分区、挂上新分区。
 */
public class FeaturePublisher {

    private static final Logger log = LoggerFactory.getLogger(FeaturePublisher.class);

    private final DataSource dataSource;

    public FeaturePublisher(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void ensureReady() {
        try (Connection connection = dataSource.getConnection()) {
            String kind = relkind(connection, "osm_feature");
            if (kind == null) {
                throw new IllegalStateException("缺少数据表 osm_feature");
            }
            if ("r".equals(kind)) {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    migrate(connection);
                    connection.commit();
                } catch (RuntimeException | SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            } else if (!"p".equals(kind)) {
                throw new IllegalStateException("osm_feature 不是可发布的分区表");
            }
            Set<String> codes = new LinkedHashSet<>();
            Regions.ALL.forEach(region -> codes.add(region.code()));
            codes.addAll(regionCodes(connection, "osm_feature"));
            ensurePartitions(connection, codes);
        } catch (SQLException ex) {
            throw new IllegalStateException("准备要素表失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 把缓冲表里的几何写入旁路表并建好索引。不修改地图正在读的分区。
     */
    public void materialize(long jobId, String regionCode) {
        String code = Regions.require(regionCode).code();
        String token = Long.toHexString(System.nanoTime());
        try (Connection connection = dataSource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = 0");
                    statement.execute("DROP TABLE IF EXISTS osm_feature_load");
                    statement.execute("DROP TABLE IF EXISTS osm_feature_retired");
                    statement.execute("CREATE TABLE osm_feature_load (LIKE osm_feature INCLUDING DEFAULTS INCLUDING CONSTRAINTS)");
                    statement.execute("ALTER TABLE osm_feature_load ADD CONSTRAINT load_pk_" + token + " PRIMARY KEY (id, region_code)");
                    statement.execute("ALTER TABLE osm_feature_load ADD CONSTRAINT ck_osm_feature_load_region CHECK (region_code = '" + code + "')");
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO osm_feature_load (osm_id, osm_type, layer, sub_type, name, props, geom, region_code, job_id)
                        SELECT osm_id,
                               osm_type,
                               layer,
                               sub_type,
                               NULLIF(name, ''),
                               CASE WHEN props IS NULL OR props = '' THEN NULL ELSE CAST(props AS jsonb) END,
                               ST_GeomFromText(wkt, 4326),
                               ?,
                               ?
                          FROM osm_feature_stage
                         WHERE wkt IS NOT NULL
                           AND wkt <> ''
                        """)) {
                    insert.setString(1, code);
                    insert.setLong(2, jobId);
                    insert.executeUpdate();
                }
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE INDEX load_geom_" + token + " ON osm_feature_load USING GIST (geom)");
                    statement.execute("CREATE INDEX load_layer_" + token + " ON osm_feature_load (layer, sub_type)");
                    statement.execute("CREATE INDEX load_name_" + token + " ON osm_feature_load USING GIN (name gin_trgm_ops)");
                    statement.execute("CREATE INDEX load_region_" + token + " ON osm_feature_load (region_code, layer)");
                    statement.execute("ANALYZE osm_feature_load");
                }
                connection.commit();
                log.info("省份 {} 的旁路分区已建好，正式地图尚未替换", code);
            } catch (SQLException ex) {
                rollbackQuietly(connection);
                throw new IllegalStateException("写入省份分区失败: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("写入省份分区失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 用已建好的旁路表替换该省分区。事务里只有摘除、挂接和删除旧分区，其他省不被排他锁挡住。
     */
    public void swap(String regionCode) {
        String code = Regions.require(regionCode).code();
        String partition = partitionName(code);
        try (Connection connection = dataSource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = 0");
                    statement.execute("SET LOCAL lock_timeout = '30s'");
                    if (relkind(connection, partition) == null) {
                        statement.execute("ALTER TABLE osm_feature_load RENAME TO " + partition);
                        statement.execute("ALTER TABLE osm_feature ATTACH PARTITION " + partition + " FOR VALUES IN ('" + code + "')");
                    } else {
                        statement.execute("ALTER TABLE osm_feature DETACH PARTITION " + partition);
                        statement.execute("ALTER TABLE " + partition + " RENAME TO osm_feature_retired");
                        statement.execute("ALTER TABLE osm_feature_load RENAME TO " + partition);
                        statement.execute("ALTER TABLE osm_feature ATTACH PARTITION " + partition + " FOR VALUES IN ('" + code + "')");
                        statement.execute("DROP TABLE osm_feature_retired");
                    }
                }
                connection.commit();
                log.info("省份 {} 的分区已替换", code);
            } catch (SQLException ex) {
                rollbackQuietly(connection);
                throw new IllegalStateException(swapMessage(ex), ex);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(swapMessage(ex), ex);
        }
        analyzeQuietly(partition);
    }

    static String partitionName(String code) {
        if (code == null || !code.matches("[a-z0-9]+") || code.length() > 40) {
            throw new IllegalArgumentException("无法为省份建分区: " + code);
        }
        return "osm_feature_p_" + code;
    }

    private void migrate(Connection connection) throws SQLException {
        log.info("把 osm_feature 改成按省分区，迁移期间切片会短暂等待这一次");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE osm_feature RENAME TO osm_feature_legacy");
        }
        renameDependents(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL statement_timeout = 0");
            statement.execute("CREATE SEQUENCE IF NOT EXISTS osm_feature_id_seq");
            statement.execute("""
                    CREATE TABLE osm_feature (
                        id          BIGINT NOT NULL DEFAULT nextval('osm_feature_id_seq'),
                        osm_id      BIGINT NOT NULL,
                        osm_type    VARCHAR(8) NOT NULL,
                        layer       VARCHAR(32) NOT NULL,
                        sub_type    VARCHAR(64),
                        name        TEXT,
                        props       JSONB,
                        geom        geometry(Geometry, 4326) NOT NULL,
                        region_code VARCHAR(32) NOT NULL,
                        job_id      BIGINT,
                        CONSTRAINT ck_osm_feature_type CHECK (osm_type IN ('node', 'way')),
                        CONSTRAINT ck_osm_feature_layer CHECK (layer IN (
                            'highway', 'railway', 'waterway', 'water',
                            'building', 'landuse', 'boundary', 'poi', 'place'
                        )),
                        PRIMARY KEY (id, region_code)
                    ) PARTITION BY LIST (region_code)
                    """);
            statement.execute("ALTER SEQUENCE osm_feature_id_seq OWNED BY osm_feature.id");
            statement.execute("ALTER TABLE osm_feature_legacy ADD COLUMN IF NOT EXISTS region_code VARCHAR(32)");
            statement.execute("UPDATE osm_feature_legacy SET region_code = 'anhui' WHERE region_code IS NULL");
        }
        Set<String> codes = new LinkedHashSet<>(Regions.ALL.stream().map(Regions.Region::code).toList());
        codes.addAll(regionCodes(connection, "osm_feature_legacy"));
        ensurePartitions(connection, codes);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO osm_feature (id, osm_id, osm_type, layer, sub_type, name, props, geom, region_code, job_id)
                    SELECT id, osm_id, osm_type, layer, sub_type, name, props, geom, region_code, job_id
                      FROM osm_feature_legacy
                    """);
            statement.execute("CREATE INDEX idx_osm_feature_geom ON osm_feature USING GIST (geom)");
            statement.execute("CREATE INDEX idx_osm_feature_layer ON osm_feature (layer, sub_type)");
            statement.execute("CREATE INDEX idx_osm_feature_name ON osm_feature USING GIN (name gin_trgm_ops)");
            statement.execute("CREATE INDEX idx_osm_feature_region ON osm_feature (region_code, layer)");
            statement.execute("""
                    SELECT setval(
                        'osm_feature_id_seq',
                        COALESCE((SELECT MAX(id) FROM osm_feature), 1),
                        (SELECT MAX(id) FROM osm_feature) IS NOT NULL
                    )
                    """);
            statement.execute("DROP TABLE osm_feature_legacy");
            statement.execute("ANALYZE osm_feature");
        }
    }

    private void renameDependents(Connection connection) throws SQLException {
        List<String> constraints = query(connection, """
                SELECT con.conname
                  FROM pg_constraint con
                  JOIN pg_class rel ON rel.oid = con.conrelid
                  JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                 WHERE nsp.nspname = 'public'
                   AND rel.relname = 'osm_feature_legacy'
                """);
        for (String name : constraints) {
            execute(connection, "ALTER TABLE osm_feature_legacy RENAME CONSTRAINT " + ident(name) + " TO " + ident(migratedName(name)));
        }
        List<String> indexes = query(connection, """
                SELECT idx.relname
                  FROM pg_index ix
                  JOIN pg_class tbl ON tbl.oid = ix.indrelid
                  JOIN pg_class idx ON idx.oid = ix.indexrelid
                  JOIN pg_namespace nsp ON nsp.oid = tbl.relnamespace
                 WHERE nsp.nspname = 'public'
                   AND tbl.relname = 'osm_feature_legacy'
                """);
        for (String name : indexes) {
            if (name.endsWith("_mig")) {
                continue;
            }
            execute(connection, "ALTER INDEX " + ident(name) + " RENAME TO " + ident(migratedName(name)));
        }
    }

    private void ensurePartitions(Connection connection, Iterable<String> codes) throws SQLException {
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String partition = partitionName(code);
            if (relkind(connection, partition) != null) {
                continue;
            }
            execute(connection, "CREATE TABLE " + partition + " PARTITION OF osm_feature FOR VALUES IN ('" + code + "')");
        }
    }

    private static Set<String> regionCodes(Connection connection, String table) throws SQLException {
        Set<String> codes = new LinkedHashSet<>();
        if (!columnExists(connection, table, "region_code")) {
            return codes;
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT DISTINCT region_code FROM " + ident(table) + " WHERE region_code IS NOT NULL")) {
            while (rows.next()) {
                codes.add(rows.getString(1));
            }
        }
        return codes;
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = ?
                   AND column_name = ?
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static String relkind(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.relkind
                  FROM pg_class c
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                 WHERE n.nspname = 'public'
                   AND c.relname = ?
                """)) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                return rows.getString(1);
            }
        }
    }

    private static List<String> query(Connection connection, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void analyzeQuietly(String partition) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ANALYZE " + partition);
        } catch (SQLException ex) {
            log.warn("分区 {} 的统计信息没有更新", partition, ex);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException ignored) {
            // 连接关闭时驱动会再回滚一次。
        }
    }

    private static String swapMessage(SQLException ex) {
        if ("55P03".equals(ex.getSQLState())) {
            return "替换该省数据时仍有切片查询占用分区，已保留原来的地图数据";
        }
        return "替换省份数据失败: " + ex.getMessage();
    }

    private static String migratedName(String name) {
        String next = name + "_mig";
        return next.length() <= 63 ? next : next.substring(0, 63);
    }

    private static String ident(String name) {
        if (name == null || !name.matches("[a-z_][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("非法标识符: " + name);
        }
        return name;
    }
}
