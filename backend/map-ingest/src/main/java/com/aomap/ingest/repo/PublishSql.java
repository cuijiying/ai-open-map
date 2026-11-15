package com.aomap.ingest.repo;

import com.aomap.common.geo.Layers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 发布语句只碰缓冲表和目标省分区。几何转换和建索引不写正在被切片读取的分区。
 */
public final class PublishSql {

    private PublishSql() {
    }

    public static String drop(String table) {
        return "DROP TABLE IF EXISTS " + table;
    }

    public static String createBuild(String table, String code) {
        String layers = Layers.ORDER.stream().map(layer -> "'" + layer + "'").collect(Collectors.joining(", "));
        return """
                CREATE TABLE %s (
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
                    CONSTRAINT %s_type CHECK (osm_type IN ('node', 'way', 'relation')),
                    CONSTRAINT %s_layer CHECK (layer IN (%s)),
                    CONSTRAINT %s_part CHECK (region_code = '%s')
                )
                """.formatted(table, table, table, layers, table, code);
    }

    public static String insertBuild(String table) {
        return """
                INSERT INTO %s (id, osm_id, osm_type, layer, sub_type, name, props, geom, region_code, job_id)
                SELECT nextval('osm_feature_id_seq'),
                       osm_id,
                       osm_type,
                       layer,
                       sub_type,
                       NULLIF(name, ''),
                       CASE WHEN props IS NULL OR props = '' THEN NULL ELSE CAST(props AS jsonb) END,
                       ST_GeomFromText(wkt, 4326),
                       ?,
                       ?
                  FROM osm_feature_stage
                 WHERE job_id = ?
                   AND wkt IS NOT NULL
                   AND wkt <> ''
                """.formatted(table);
    }

    public static List<String> indexBuild(String table) {
        return List.of(
                "ALTER TABLE " + table + " ADD PRIMARY KEY (id, region_code)",
                "CREATE INDEX " + table + "_geom ON " + table + " USING GIST (geom)",
                "CREATE INDEX " + table + "_layer ON " + table + " (layer, sub_type)",
                "CREATE INDEX " + table + "_name ON " + table + " USING GIN (name gin_trgm_ops)",
                "CREATE INDEX " + table + "_region ON " + table + " (region_code, layer)"
        );
    }

    /**
     * 挂分区时 PostgreSQL 按约束名和表达式树核对父表。
     * 把 pg_get_constraintdef 的结果再执行一遍，会写成另一种等价形式，挂分区时报定义不同。
     * 这里按建表时的 IN 列表重建，名称沿用父表。
     */
    public static String equivalentCheck(String parentDefinition) {
        if (parentDefinition != null && parentDefinition.contains("(layer)")) {
            return "CHECK (layer IN ('highway', 'railway', 'waterway', 'water', 'building', 'landuse', 'boundary', 'poi', 'place'))";
        }
        if (parentDefinition != null && parentDefinition.contains("(osm_type)")) {
            return "CHECK (osm_type IN ('node', 'way', 'relation'))";
        }
        throw new IllegalArgumentException("无法复制检查约束");
    }

    public static List<String> copyCheck(String table, String name, String definition) {
        if (!name.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("无法复制检查约束");
        }
        String check = equivalentCheck(definition);
        return List.of(
                "ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name,
                "ALTER TABLE " + table + " ADD CONSTRAINT " + name + " " + check
        );
    }

    public static List<String> swap(String partition, String build, String retired, String code, boolean replace) {
        List<String> statements = new ArrayList<>();
        statements.add("SET LOCAL lock_timeout = '15s'");
        statements.add("SET LOCAL statement_timeout = '60s'");
        if (replace) {
            statements.add("ALTER TABLE osm_feature DETACH PARTITION " + partition);
            statements.add("ALTER TABLE " + partition + " RENAME TO " + retired);
        }
        statements.add("ALTER TABLE " + build + " RENAME TO " + partition);
        statements.add("ALTER TABLE osm_feature ATTACH PARTITION " + partition + " FOR VALUES IN ('" + code + "')");
        return List.copyOf(statements);
    }
}
