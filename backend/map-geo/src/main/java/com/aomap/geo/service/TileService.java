package com.aomap.geo.service;

import com.aomap.common.geo.Layers;
import com.aomap.common.geo.Regions;
import com.aomap.geo.mapper.TileMapper;
import com.aomap.geo.tile.NationalTileStore;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class TileService {

    private final TileMapper tiles;
    private final NationalTileStore nationalTiles;
    private final TileCache cache = new TileCache(400, 30_000);

    public TileService(TileMapper tiles, NationalTileStore nationalTiles) {
        this.tiles = tiles;
        this.nationalTiles = nationalTiles;
    }

    public byte[] tile(String regions, String layer, int z, int x, int y) {
        if (!Layers.known(layer) || z < 0 || z > 16) {
            return new byte[0];
        }
        int span = 1 << z;
        if (x < 0 || y < 0 || x >= span || y >= span) {
            return new byte[0];
        }
        List<String> codes = Regions.acceptedCodes(regions);
        if (codes.isEmpty()) {
            return new byte[0];
        }
        String key = layer + "/" + z + "/" + x + "/" + y + "/" + String.join(",", codes.stream().sorted().toList());
        long now = System.currentTimeMillis();
        byte[] cached = cache.get(key, now);
        if (cached != null) {
            return cached;
        }
        byte[] prepared = preparedTile(codes, layer, z, x, y);
        if (prepared != null) {
            cache.put(key, prepared, now);
            return prepared;
        }
        String encoded = tiles.mvt(codes, layer, z, x, y);
        byte[] body = encoded == null || encoded.isBlank() ? new byte[0] : Base64.getMimeDecoder().decode(encoded);
        cache.put(key, body, now);
        return body;
    }

    private byte[] preparedTile(List<String> codes, String layer, int z, int x, int y) {
        try {
            if (!nationalTiles.matches(codes, layer, z)) {
                return null;
            }
            return nationalTiles.read(layer, z, x, y);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
