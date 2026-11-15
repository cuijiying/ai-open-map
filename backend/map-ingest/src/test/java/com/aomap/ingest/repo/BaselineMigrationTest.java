package com.aomap.ingest.repo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineMigrationTest {

    @Test
    void baselineMatchesSchemaSql() throws IOException {
        String schema = read("../../sql/schema.sql");
        String baseline = read("src/main/resources/db/migration/V1__baseline.sql");
        assertTrue(baseline.contains("CREATE TABLE IF NOT EXISTS osm_feature"));
        assertTrue(baseline.contains("CREATE TABLE IF NOT EXISTS ai_model_config"));
        assertEquals(schema, baseline);
    }

    private static String read(String relative) throws IOException {
        return Files.readString(Path.of(relative)).replace("\r\n", "\n").strip();
    }
}
