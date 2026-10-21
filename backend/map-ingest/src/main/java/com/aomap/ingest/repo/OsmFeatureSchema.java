package com.aomap.ingest.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 把已有的普通 osm_feature 收成按省分区的表。转换时先复制到新表，最后才短暂改名，切片在复制期间仍读旧表。
 */
@Component
@Order(0)
public class OsmFeatureSchema implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OsmFeatureSchema.class);

    private final DataSource dataSource;

    public OsmFeatureSchema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureReady();
    }

    public void ensureReady() {
        synchronized (this) {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true);
                String kind = relkind(connection, "osm_feature");
                if (kind == null) {
                    throw new IllegalStateException("缺少 osm_feature，请先执行建表脚本");
                }
                if ("p".equals(kind)) {
                    ensureIndexes(connection);
                    dropLeftover(connection);
                    return;
                }
                if (!"r".equals(kind)) {
                    throw new IllegalStateException("osm_feature 类型无法迁移: " + kind);
                }
                convert(connection);
            } catch (SQLException ex) {
                throw new IllegalStateException("整理要素表失败: " + ex.getMessage(), ex);
            }
        }
    }

    private void convert(Connection connection) throws SQLException {
        log.info("开始把 osm_feature 转为按省分区，转换完成前切片仍读取现有数据");
        boolean original = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL statement_timeout = 0");
            statement.execute("SET LOCAL lock_timeout = '60s'");
            statement.execute("CREATE SEQUENCE IF NOT EXISTS osm_feature_id_seq");
            statement.execute("ALTER SEQUENCE IF EXISTS osm_feature_id_seq OWNED BY NONE");
            statement.execute("DROP TABLE IF EXISTS osm_feature_next CASCADE");
            statement.execute(nextTable());
            List<String> codes = distinctRegions(connection);
            for (String code : codes) {
                if (code == null || !code.matches("[a-z]{2,32}")) {
                    throw new IllegalStateException("无法迁移未知区域代码: " + code);
                }
                String partition = "osm_feature_p_" + code;
                statement.execute("CREATE TABLE " + partition + " PARTITION OF osm_feature_next FOR VALUES IN ('" + code + "')");
                statement.execute("""
                        INSERT INTO osm_feature_next (id, osm_id, osm_type, layer, sub_type, name, props, geom, region_code, job_id)
                        SELECT id, osm_id, osm_type, layer, sub_type, name, props, geom, region_code, job_id
                          FROM osm_feature
                         WHERE region_code = '%s'
                        """.formatted(code));
            }
            statement.execute("CREATE INDEX idx_osm_feature_next_geom ON osm_feature_next USING GIST (geom)");
            statement.execute("CREATE INDEX idx_osm_feature_next_layer ON osm_feature_next (layer, sub_type)");
            statement.execute("CREATE INDEX idx_osm_feature_next_name ON osm_feature_next USING GIN (name gin_trgm_ops)");
            statement.execute("CREATE INDEX idx_osm_feature_next_region ON osm_feature_next (region_code, layer)");
            renameIndex(statement, "idx_osm_feature_geom");
            renameIndex(statement, "idx_osm_feature_layer");
            renameIndex(statement, "idx_osm_feature_name");
            renameIndex(statement, "idx_osm_feature_region");
            statement.execute("ALTER TABLE osm_feature RENAME TO osm_feature_legacy");
            statement.execute("ALTER TABLE osm_feature_next RENAME TO osm_feature");
            statement.execute("ALTER INDEX idx_osm_feature_next_geom RENAME TO idx_osm_feature_geom");
            statement.execute("ALTER INDEX idx_osm_feature_next_layer RENAME TO idx_osm_feature_layer");
            statement.execute("ALTER INDEX idx_osm_feature_next_name RENAME TO idx_osm_feature_name");
            statement.execute("ALTER INDEX idx_osm_feature_next_region RENAME TO idx_osm_feature_region");
            statement.execute("ALTER TABLE osm_feature RENAME CONSTRAINT osm_feature_next_pkey TO pk_osm_feature");
            statement.execute("ALTER SEQUENCE osm_feature_id_seq OWNED BY osm_feature.id");
            try (ResultSet rs = statement.executeQuery("""
                    SELECT setval('osm_feature_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM osm_feature), 1))
                    """)) {
                rs.next();
            }
            connection.commit();
        } catch (RuntimeException | SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(original);
            } catch (SQLException ex) {
                log.warn("归还连接时恢复自动提交失败: {}", ex.getMessage());
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS osm_feature_legacy");
        }
        log.info("osm_feature 已按省分区");
    }

    private static void renameIndex(Statement statement, String name) throws SQLException {
        boolean exists;
        try (ResultSet rs = statement.executeQuery("SELECT to_regclass('public." + name + "')")) {
            rs.next();
            exists = rs.getString(1) != null;
        }
        if (exists) {
            statement.execute("ALTER INDEX " + name + " RENAME TO " + name + "_legacy");
        }
    }

    private static List<String> distinctRegions(Connection connection) throws SQLException {
        List<String> codes = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT DISTINCT region_code FROM osm_feature WHERE region_code IS NOT NULL")) {
            while (rs.next()) {
                codes.add(rs.getString(1));
            }
        }
        return codes;
    }

    private void dropLeftover(Connection connection) throws SQLException {
        if (relkind(connection, "osm_feature_legacy") == null) {
            return;
        }
        long live = count(connection, "osm_feature");
        long old = count(connection, "osm_feature_legacy");
        if (old > 0 && live < old) {
            throw new IllegalStateException("发现未完成的表迁移，已保留 osm_feature_legacy");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS osm_feature_legacy");
        }
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void ensureIndexes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SEQUENCE IF NOT EXISTS osm_feature_id_seq");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_osm_feature_geom ON osm_feature USING GIST (geom)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_osm_feature_layer ON osm_feature (layer, sub_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_osm_feature_name ON osm_feature USING GIN (name gin_trgm_ops)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_osm_feature_region ON osm_feature (region_code, layer)");
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
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static String nextTable() {
        return """
                CREATE TABLE osm_feature_next (
                    id BIGINT NOT NULL DEFAULT nextval('osm_feature_id_seq'),
                    osm_id BIGINT NOT NULL,
                    osm_type VARCHAR(8) NOT NULL,
                    layer VARCHAR(32) NOT NULL,
                    sub_type VARCHAR(64),
                    name TEXT,
                    props JSONB,
                    geom geometry(Geometry, 4326) NOT NULL,
                    region_code VARCHAR(32) NOT NULL,
                    job_id BIGINT,
                    CONSTRAINT osm_feature_next_pkey PRIMARY KEY (id, region_code),
                    CONSTRAINT ck_osm_feature_next_type CHECK (osm_type IN ('node', 'way')),
                    CONSTRAINT ck_osm_feature_next_layer CHECK (layer IN (
                        'highway', 'railway', 'waterway', 'water',
                        'building', 'landuse', 'boundary', 'poi', 'place'
                    ))
                ) PARTITION BY LIST (region_code)
                """;
    }
}
