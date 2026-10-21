package com.aomap.common.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionsTest {

    @Test
    void parseCodesUsesSingleRegionWhenRegionsBlank() {
        assertEquals(List.of("anhui"), Regions.parseCodes(null, "anhui"));
        assertEquals(List.of("zhejiang"), Regions.parseCodes("  ", "zhejiang"));
    }

    @Test
    void parseCodesSplitsAndDedupes() {
        assertEquals(List.of("anhui", "zhejiang"), Regions.parseCodes("anhui,zhejiang", "beijing"));
        assertEquals(List.of("anhui", "zhejiang"), Regions.parseCodes("anhui, anhui ,zhejiang", "beijing"));
    }

    @Test
    void parseCodesRejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> Regions.parseCodes("nosuch", "anhui"));
    }
}
