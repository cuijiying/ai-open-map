package com.aomap.ingest.repo;

import com.aomap.ingest.domain.LayerCountRow;
import com.aomap.ingest.domain.RegionCountRow;
import com.aomap.ingest.mapper.FeatureMapper;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FeatureRepository {

    private static final Logger log = LoggerFactory.getLogger(FeatureRepository.class);
    private static final String COPY_SQL = """
            COPY osm_feature_stage (osm_id, osm_type, layer, sub_type, name, props, wkt)
            FROM STDIN WITH (FORMAT csv)
            """;

    private final FeatureMapper features;
    private final DataSource dataSource;
    private final OsmFeatureSchema schema;

    public FeatureRepository(FeatureMapper features, DataSource dataSource, OsmFeatureSchema schema) {
        this.features = features;
        this.dataSource = dataSource;
        this.schema = schema;
    }

    public void truncateStage() {
        features.truncateStage();
    }

    public void copy(List<StagedFeature> rows) {
        if (rows.isEmpty()) {
            return;
        }
        StringBuilder csv = new StringBuilder(rows.size() * 160);
        for (StagedFeature row : rows) {
            csv.append(row.osmId()).append(',')
                    .append(row.osmType()).append(',')
                    .append(csv(row.layer())).append(',')
                    .append(csv(row.subType())).append(',')
                    .append(csv(row.name())).append(',')
                    .append(csv(row.props())).append(',')
                    .append(csv(row.wkt()))
                    .append('\n');
        }
        try (Connection connection = dataSource.getConnection()) {
            CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
            copyManager.copyIn(COPY_SQL, new StringReader(csv.toString()));
        } catch (Exception ex) {
            throw new IllegalStateException("写入解析缓冲表失败: " + ex.getMessage(), ex);
        }
    }

    public long countStage() {
        return features.countStage();
    }

    public long countStored(String regionCode) {
        return features.countFeatures(FeatureTables.requireCode(regionCode));
    }

    public void startBuild(String regionCode) {
        schema.ensureReady();
        String code = FeatureTables.requireCode(regionCode);
        String table = FeatureTables.build(code);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET statement_timeout = 0");
            try {
                statement.execute(PublishSql.drop(FeatureTables.retired(code)));
                statement.execute(PublishSql.drop(table));
                statement.execute("CREATE SEQUENCE IF NOT EXISTS osm_feature_id_seq");
                statement.execute(PublishSql.createBuild(table, code));
            } finally {
                statement.execute("RESET statement_timeout");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("创建发布缓冲失败: " + ex.getMessage(), ex);
        }
    }

    public long copyIntoBuild(long jobId, String regionCode) {
        String code = FeatureTables.requireCode(regionCode);
        String table = FeatureTables.build(code);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = 0");
                }
                long count;
                try (PreparedStatement statement = connection.prepareStatement(PublishSql.insertBuild(table))) {
                    statement.setString(1, code);
                    statement.setLong(2, jobId);
                    count = statement.executeUpdate();
                }
                connection.commit();
                return count;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                restoreAutoCommit(connection);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("写入发布缓冲失败: " + ex.getMessage(), ex);
        }
    }

    public void indexBuild(String regionCode) {
        String table = FeatureTables.build(regionCode);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = 0");
                    for (String sql : PublishSql.indexBuild(table)) {
                        statement.execute(sql);
                    }
                    statement.execute("ANALYZE " + table);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                restoreAutoCommit(connection);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("为发布缓冲建索引失败: " + ex.getMessage(), ex);
        }
    }

    public void publishBuild(String regionCode) {
        String code = FeatureTables.requireCode(regionCode);
        IllegalStateException last = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                swapOnce(code);
                return;
            } catch (IllegalStateException ex) {
                last = ex;
                if (!isLockTimeout(ex) || attempt == 4) {
                    throw ex;
                }
                sleep(attempt);
            }
        }
        throw last == null ? new IllegalStateException("切换省份数据失败") : last;
    }

    public void abortBuild(String regionCode) {
        try {
            String table = FeatureTables.build(regionCode);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute(PublishSql.drop(table));
            }
        } catch (RuntimeException | SQLException ex) {
            log.warn("清理发布缓冲失败: {}", ex.getMessage());
        }
    }

    private void swapOnce(String code) {
        String partition = FeatureTables.partition(code);
        String build = FeatureTables.build(code);
        String retired = FeatureTables.retired(code);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean replace = partitionExists(connection, partition);
                try (Statement statement = connection.createStatement()) {
                    for (String sql : PublishSql.swap(partition, build, retired, code, replace)) {
                        statement.execute(sql);
                    }
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw new IllegalStateException("切换省份数据失败: " + ex.getMessage(), ex);
            } finally {
                restoreAutoCommit(connection);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("切换省份数据失败: " + ex.getMessage(), ex);
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ANALYZE " + partition);
            statement.execute(PublishSql.drop(retired));
        } catch (SQLException ex) {
            log.warn("省份 {} 已切换，整理统计信息失败: {}", code, ex.getMessage());
        }
    }

    private static boolean partitionExists(Connection connection, String partition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                      FROM pg_class child
                      JOIN pg_inherits inh ON inh.inhrelid = child.oid
                      JOIN pg_class parent ON parent.oid = inh.inhparent
                     WHERE parent.relname = 'osm_feature'
                       AND child.relname = ?
                )
                """)) {
            statement.setString(1, partition);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private static void restoreAutoCommit(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ex) {
            log.warn("归还连接时恢复自动提交失败: {}", ex.getMessage());
        }
    }

    private static boolean isLockTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sql && "55P03".equals(sql.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void sleep(int attempt) {
        try {
            Thread.sleep(400L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("切换省份数据被中断");
        }
    }

    public Map<String, Long> countByLayer(String regionCode) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LayerCountRow row : features.countByLayer(regionCode)) {
            counts.put(row.getLayer(), row.getCount());
        }
        return counts;
    }

    public Map<String, Long> countByRegion() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (RegionCountRow row : features.countByRegion()) {
            counts.put(row.getRegionCode(), row.getCount() == null ? 0L : row.getCount());
        }
        return counts;
    }

    static String csv(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    public record StagedFeature(
            long osmId,
            String osmType,
            String layer,
            String subType,
            String name,
            String props,
            String wkt
    ) {
    }
}
