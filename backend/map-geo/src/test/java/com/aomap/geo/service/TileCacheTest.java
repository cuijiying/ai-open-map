package com.aomap.geo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TileCacheTest {

    @Test
    void returnsTheSameBytesUntilExpiry() {
        TileCache cache = new TileCache(2, 1_000);
        cache.put("a", new byte[]{1}, 0);
        assertArrayEquals(new byte[]{1}, cache.get("a", 999));
        assertNull(cache.get("a", 1_000));
    }

    @Test
    void dropsTheLeastRecentlyUsedEntry() {
        TileCache cache = new TileCache(2, 10_000);
        cache.put("a", new byte[]{1}, 0);
        cache.put("b", new byte[]{2}, 0);
        cache.get("a", 1);
        cache.put("c", new byte[]{3}, 2);
        assertNull(cache.get("b", 3));
        assertArrayEquals(new byte[]{1}, cache.get("a", 3));
        assertEquals(2, cache.size());
    }
}
