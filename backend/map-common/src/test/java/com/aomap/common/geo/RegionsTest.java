package com.aomap.common.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionsTest {

    @Test
    void acceptsSeveralProvincesAndSkipsUnknownCodes() {
        assertEquals(List.of("anhui", "zhejiang"), Regions.acceptedCodes(" anhui, Zhejiang ,missing"));
        assertEquals(List.of(), Regions.acceptedCodes("missing"));
        assertEquals(List.of("anhui"), Regions.acceptedCodes(" "));
    }

    @Test
    void requireCodesRejectsAnUnknownProvince() {
        assertThrows(IllegalArgumentException.class, () -> Regions.requireCodes("anhui,missing"));
        assertEquals(List.of("zhejiang", "anhui"), Regions.requireCodes("zhejiang,anhui,zhejiang"));
        assertEquals("安徽省、浙江省", Regions.describe(List.of("anhui", "zhejiang")));
    }
}
