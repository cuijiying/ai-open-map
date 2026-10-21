package com.aomap.geo.service;

import com.aomap.common.geo.Layers;
import com.aomap.common.geo.RegionSelection;
import com.aomap.geo.mapper.TileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

@Service
public class TileService {

    private final TileMapper tiles;

    public TileService(TileMapper tiles) {
        this.tiles = tiles;
    }

    @Transactional
    public byte[] tile(String regions, String region, String layer, int z, int x, int y) {
        List<String> codes;
        try {
            codes = RegionSelection.resolve(regions, region);
        } catch (IllegalArgumentException ex) {
            return new byte[0];
        }
        if (codes.isEmpty() || !Layers.known(layer) || z < 0 || z > 16) {
            return new byte[0];
        }
        int span = 1 << z;
        if (x < 0 || y < 0 || x >= span || y >= span) {
            return new byte[0];
        }
        tiles.prepareRead();
        String encoded = tiles.mvt(codes, layer, z, x, y);
        if (encoded == null || encoded.isBlank()) {
            return new byte[0];
        }
        return Base64.getMimeDecoder().decode(encoded);
    }
}
