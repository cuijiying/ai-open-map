package com.aomap.geo.service;

import com.aomap.common.geo.Layers;
import com.aomap.geo.mapper.TileMapper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class TileService {

    private final TileMapper tiles;

    public TileService(TileMapper tiles) {
        this.tiles = tiles;
    }

    public byte[] tile(String layer, int z, int x, int y) {
        if (!Layers.known(layer) || z < 0 || z > 16) {
            return new byte[0];
        }
        int span = 1 << z;
        if (x < 0 || y < 0 || x >= span || y >= span) {
            return new byte[0];
        }
        String encoded = tiles.mvt(layer, z, x, y);
        if (encoded == null || encoded.isBlank()) {
            return new byte[0];
        }
        return Base64.getMimeDecoder().decode(encoded);
    }
}
