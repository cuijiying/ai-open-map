package com.aomap.geo.tile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileWarmupPlanTest {

    @Test
    void skipsLayersThatStayEmptyAtTheRequestedZoom() {
        assertEquals(List.of("highway", "water", "boundary", "place"), TileWarmupService.includedLayers(6));
        assertTrue(TileWarmupService.includedLayers(8).contains("railway"));
        assertTrue(TileWarmupService.includedLayers(8).contains("waterway"));
    }
}
