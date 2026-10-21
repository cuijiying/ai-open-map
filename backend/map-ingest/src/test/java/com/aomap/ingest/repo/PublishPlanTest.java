package com.aomap.ingest.repo;

import com.aomap.common.geo.RegionSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishPlanTest {

    @Test
    void keepsSeveralProvincesInOrder() {
        assertEquals(List.of("zhejiang", "anhui", "guangdong"), RegionSelection.parse("zhejiang, anhui, zhejiang, guangdong"));
        assertEquals(List.of("jiangsu", "shanghai"), RegionSelection.resolve("jiangsu,shanghai", "anhui"));
        assertEquals(List.of(), RegionSelection.resolve("  ", "anhui"));
        assertEquals(List.of("anhui"), RegionSelection.resolve(null, null));
        assertEquals(List.of("zhejiang"), RegionSelection.resolve(null, "Zhejiang"));
        assertThrows(IllegalArgumentException.class, () -> RegionSelection.resolve("not-a-province", null));
    }

    @Test
    void loadWritesAPrivateTable() {
        String table = FeatureTables.build("zhejiang");
        String sql = PublishSql.insertBuild(table);
        assertEquals("osm_feature_b_zhejiang", table);
        assertTrue(sql.contains("INSERT INTO osm_feature_b_zhejiang"));
        assertTrue(sql.contains("FROM osm_feature_stage"));
        assertFalse(sql.contains("DELETE FROM osm_feature"));
        assertFalse(sql.contains("TRUNCATE osm_feature"));
    }

    @Test
    void swapTouchesOnlyTheChosenPartition() {
        List<String> replacing = PublishSql.swap(
                "osm_feature_p_guangdong",
                "osm_feature_b_guangdong",
                "osm_feature_o_guangdong",
                "guangdong",
                true
        );
        String sql = String.join("\n", replacing);
        assertTrue(sql.contains("DETACH PARTITION osm_feature_p_guangdong"));
        assertTrue(sql.contains("ATTACH PARTITION osm_feature_p_guangdong FOR VALUES IN ('guangdong')"));
        assertFalse(sql.contains("TRUNCATE"));
        assertFalse(sql.contains("anhui"));

        String first = String.join("\n", PublishSql.swap(
                "osm_feature_p_xinjiang",
                "osm_feature_b_xinjiang",
                "osm_feature_o_xinjiang",
                "xinjiang",
                false
        ));
        assertFalse(first.contains("DETACH"));
        assertTrue(first.contains("FOR VALUES IN ('xinjiang')"));
    }

    @Test
    void rejectsCodesThatAreNotProvinces() {
        assertEquals("osm_feature_p_xinjiang", FeatureTables.partition("xinjiang"));
        assertThrows(IllegalArgumentException.class, () -> FeatureTables.partition("anhui;drop"));
        assertThrows(IllegalArgumentException.class, () -> FeatureTables.build("../anhui"));
    }
}
