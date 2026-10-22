package com.aomap.geo.tile;

import com.aomap.geo.tile.NationalTileStore.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NationalTileStoreTest {

    @TempDir
    Path temp;

    @Test
    void servesOnlyTheRegionSetRecordedInTheManifest() throws Exception {
        NationalTileStore store = new NationalTileStore(temp);
        store.beginRebuild();
        store.write("highway", 6, 52, 26, new byte[]{1, 2, 3});
        store.write("highway", 6, 1, 1, new byte[0]);
        assertNull(store.read("highway", 6, 1, 1));
        store.save(new Manifest(List.of("anhui", "zhejiang"), 6, List.of("highway", "place"), 1));

        assertArrayEquals(new byte[]{1, 2, 3}, store.read("highway", 6, 52, 26));
        assertTrue(store.matches(List.of("zhejiang", "anhui"), "highway", 6));
        assertFalse(store.matches(List.of("anhui"), "highway", 6));
        assertFalse(store.matches(List.of("anhui", "zhejiang", "jiangsu"), "highway", 4));
        assertFalse(store.matches(List.of("anhui", "zhejiang"), "water", 6));
        assertFalse(store.matches(List.of("anhui", "zhejiang"), "highway", 7));

        store.beginRebuild();
        assertNull(store.current());
        assertFalse(store.matches(List.of("anhui", "zhejiang"), "highway", 6));
    }
}
