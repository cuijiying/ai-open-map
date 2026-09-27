package com.aomap.ingest.repo;

import com.aomap.ingest.domain.LayerCountRow;
import com.aomap.ingest.mapper.FeatureMapper;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FeatureRepository {

    private static final String COPY_SQL = """
            COPY osm_feature_stage (osm_id, osm_type, layer, sub_type, name, props, wkt)
            FROM STDIN WITH (FORMAT csv)
            """;

    private final FeatureMapper features;
    private final DataSource dataSource;

    public FeatureRepository(FeatureMapper features, DataSource dataSource) {
        this.features = features;
        this.dataSource = dataSource;
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

    @Transactional
    public long publish(long jobId) {
        features.disableStatementTimeout();
        features.dropGeomIndex();
        features.dropLayerIndex();
        features.dropNameIndex();
        features.truncateFeatures();
        features.insertFromStage(jobId);
        features.createGeomIndex();
        features.createLayerIndex();
        features.createNameIndex();
        features.analyzeFeatures();
        return features.countFeatures();
    }

    public Map<String, Long> countByLayer() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LayerCountRow row : features.countByLayer()) {
            counts.put(row.getLayer(), row.getCount());
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
