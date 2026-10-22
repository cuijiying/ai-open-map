package com.aomap.geo.tile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlippyTest {

    @Test
    void chinaCoverContainsHefeiAndSkipsParis() {
        assertEquals(52, Slippy.xOf(117.28, 6));
        assertEquals(26, Slippy.yOf(31.86, 6));
        List<Slippy.Tile> cover = Slippy.coverChina(6);
        assertEquals(165, cover.size());
        assertTrue(cover.contains(new Slippy.Tile(6, 52, 26)));
        assertTrue(cover.stream().noneMatch(tile -> tile.equals(new Slippy.Tile(6, 32, 22))));
        assertTrue(cover.stream().allMatch(tile -> tile.z() <= 6));
    }
}
