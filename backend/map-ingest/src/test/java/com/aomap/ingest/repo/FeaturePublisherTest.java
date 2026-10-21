package com.aomap.ingest.repo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FeaturePublisherTest {

    private static final String ADMIN_URL = "jdbc:postgresql://127.0.0.1:5432/postgres";
    private static final String URL = "jdbc:postgresql://127.0.0.1:5432/aomap_feature_test";
    private static boolean databaseReady;
    private static DataSource dataSource;

    @BeforeAll
    static void prepareDatabase() {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, "postgres", "post");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS aomap_feature_test WITH (FORCE)");
            statement.execute("CREATE DATABASE aomap_feature_test");
            databaseReady = true;
        } catch (SQLException ex) {
            databaseReady = false;
        }
        if (databaseReady) {
            DriverManagerDataSource source = new DriverManagerDataSource(URL, "postgres", "post");
            dataSource = source;
        }
    }

    @BeforeEach
    void resetSchema() throws Exception {
        assumeTrue(databaseReady, "本机没有可连接的 PostgreSQL，跳过发布测试");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS public CASCADE");
            statement.execute("CREATE SCHEMA public");
            statement.execute("CREATE EXTENSION postgis");
            statement.execute("CREATE EXTENSION pg_trgm");
        }
    }

    @Test
    void partitionNameRejectsAnythingThatIsNotAProvinceCode() {
        assertEquals("osm_feature_p_anhui", FeaturePublisher.partitionName("anhui"));
        assertThrows(IllegalArgumentException.class, () -> FeaturePublisher.partitionName("anhui;drop"));
    }

    @Test
    void legacyTableBecomesPartitionsAndSwapReplacesOnlyThatProvince() throws Exception {
        legacyTable();
        insertLegacy("anhui", "长江路", "LINESTRING(117.2 31.8, 117.3 31.9)");
        insertLegacy("zhejiang", "延安路", "LINESTRING(120.1 30.2, 120.2 30.3)");
        FeaturePublisher publisher = new FeaturePublisher(dataSource);
        publisher.ensureReady();

        assertEquals("p", relkind("osm_feature"));
        assertEquals(List.of("anhui:长江路", "zhejiang:延安路"), names("anhui", "zhejiang"));

        stage("LINESTRING(117.4 31.7, 117.5 31.75)", "新长江路");
        publisher.materialize(9, "anhui");
        assertEquals(List.of("anhui:长江路", "zhejiang:延安路"), names("anhui", "zhejiang"));
        publisher.swap("anhui");
        assertEquals(List.of("anhui:新长江路", "zhejiang:延安路"), names("anhui", "zhejiang"));

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE osm_feature_stage");
        }
        stage("LINESTRING(117.6 31.6, 117.7 31.65)", "二环");
        publisher.materialize(10, "anhui");
        publisher.swap("anhui");
        assertEquals(List.of("anhui:二环", "zhejiang:延安路"), names("anhui", "zhejiang"));
    }

    @Test
    void schemaScriptUpgradesAnExistingRegularTable() throws Exception {
        legacyTable();
        insertLegacy("anhui", "长江路", "LINESTRING(117.2 31.8, 117.3 31.9)");
        insertLegacy("zhejiang", "延安路", "LINESTRING(120.1 30.2, 120.2 30.3)");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(schemaPath()), StandardCharsets.UTF_8));
        }
        FeaturePublisher publisher = new FeaturePublisher(dataSource);
        publisher.ensureReady();
        assertEquals("p", relkind("osm_feature"));
        assertEquals(List.of("anhui:长江路", "zhejiang:延安路"), names("anhui", "zhejiang"));
    }

    @Test
    void schemaScriptStartsPartitionedAndOtherProvinceStaysReadableDuringLock() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(schemaPath()), StandardCharsets.UTF_8));
        }
        FeaturePublisher publisher = new FeaturePublisher(dataSource);
        publisher.ensureReady();
        assertEquals("p", relkind("osm_feature"));
        assertTrue(relkind("osm_feature_p_anhui") != null);
        assertTrue(relkind("osm_feature_p_zhejiang") != null);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO osm_feature (osm_id, osm_type, layer, name, geom, region_code)
                    VALUES (1, 'way', 'highway', '长江路', ST_GeomFromText('LINESTRING(117.2 31.8, 117.3 31.9)', 4326), 'anhui'),
                           (2, 'way', 'highway', '延安路', ST_GeomFromText('LINESTRING(120.1 30.2, 120.2 30.3)', 4326), 'zhejiang')
                    """);
        }

        try (Connection locker = dataSource.getConnection()) {
            locker.setAutoCommit(false);
            try (Statement statement = locker.createStatement()) {
                statement.execute("LOCK TABLE osm_feature_p_anhui IN ACCESS EXCLUSIVE MODE");
            }
            long started = System.nanoTime();
            try (Connection reader = dataSource.getConnection();
                 Statement statement = reader.createStatement()) {
                statement.execute("SET statement_timeout = '2s'");
                try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM osm_feature WHERE region_code = 'zhejiang'")) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt(1));
                }
            }
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            assertTrue(elapsedMs < 1000, "读取浙江时被安徽分区锁等待了 " + elapsedMs + " 毫秒");
            locker.rollback();
        }
    }

    private static Path schemaPath() {
        Path schema = Path.of("sql", "schema.sql");
        if (!Files.exists(schema)) {
            schema = Path.of("..", "..", "sql", "schema.sql");
        }
        return schema;
    }

    private static void legacyTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE osm_feature (
                        id BIGSERIAL PRIMARY KEY,
                        osm_id BIGINT NOT NULL,
                        osm_type VARCHAR(8) NOT NULL,
                        layer VARCHAR(32) NOT NULL,
                        sub_type VARCHAR(64),
                        name TEXT,
                        props JSONB,
                        geom geometry(Geometry, 4326) NOT NULL,
                        job_id BIGINT,
                        CONSTRAINT ck_osm_feature_type CHECK (osm_type IN ('node', 'way')),
                        CONSTRAINT ck_osm_feature_layer CHECK (layer IN (
                            'highway', 'railway', 'waterway', 'water',
                            'building', 'landuse', 'boundary', 'poi', 'place'
                        ))
                    )
                    """);
            statement.execute("CREATE INDEX idx_osm_feature_geom ON osm_feature USING GIST (geom)");
            statement.execute("CREATE INDEX idx_osm_feature_layer ON osm_feature (layer, sub_type)");
            statement.execute("CREATE INDEX idx_osm_feature_name ON osm_feature USING GIN (name gin_trgm_ops)");
            statement.execute("ALTER TABLE osm_feature ADD COLUMN region_code VARCHAR(32)");
            statement.execute("""
                    CREATE UNLOGGED TABLE osm_feature_stage (
                        osm_id BIGINT,
                        osm_type VARCHAR(8),
                        layer VARCHAR(32),
                        sub_type VARCHAR(64),
                        name TEXT,
                        props TEXT,
                        wkt TEXT
                    )
                    """);
        }
    }

    private static void insertLegacy(String region, String name, String wkt) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO osm_feature (osm_id, osm_type, layer, sub_type, name, geom, region_code)
                    VALUES (1, 'way', 'highway', 'primary', '%s', ST_GeomFromText('%s', 4326), '%s')
                    """.formatted(name, wkt, region));
        }
    }

    private static void stage(String wkt, String name) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO osm_feature_stage (osm_id, osm_type, layer, sub_type, name, props, wkt)
                    VALUES (9, 'way', 'highway', 'motorway', '%s', '', '%s')
                    """.formatted(name, wkt));
        }
    }

    private static String relkind(String name) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT c.relkind
                       FROM pg_class c
                       JOIN pg_namespace n ON n.oid = c.relnamespace
                      WHERE n.nspname = 'public' AND c.relname = '%s'
                     """.formatted(name))) {
            return rows.next() ? rows.getString(1) : null;
        }
    }

    private static List<String> names(String... regions) throws SQLException {
        String in = String.join("','", regions);
        List<String> values = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT region_code || ':' || name FROM osm_feature
                      WHERE region_code IN ('%s')
                      ORDER BY region_code
                     """.formatted(in))) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }
}
