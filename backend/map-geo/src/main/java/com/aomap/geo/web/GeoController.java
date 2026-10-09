package com.aomap.geo.web;

import com.aomap.common.api.ApiResult;
import com.aomap.common.dto.GeoDtos.AnalysisResult;
import com.aomap.common.dto.GeoDtos.LayerCount;
import com.aomap.common.dto.GeoDtos.SearchHit;
import com.aomap.common.dto.GeoDtos.StatItem;
import com.aomap.geo.service.GeoQueryService;
import com.aomap.geo.service.TileService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
public class GeoController {

    private static final MediaType MVT = MediaType.parseMediaType("application/vnd.mapbox-vector-tile");

    private final TileService tiles;
    private final GeoQueryService queries;

    public GeoController(TileService tiles, GeoQueryService queries) {
        this.tiles = tiles;
        this.queries = queries;
    }

    @GetMapping(value = "/api/tiles/{layer}/{z}/{x}/{y}.pbf")
    public ResponseEntity<byte[]> tile(@PathVariable String layer, @PathVariable int z,
                                       @PathVariable int x, @PathVariable int y,
                                       @RequestParam(defaultValue = "anhui") String region) {
        return ResponseEntity.ok()
                .contentType(MVT)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic())
                .body(tiles.tile(region, layer, z, x, y));
    }

    @GetMapping("/api/geo/layers")
    public ApiResult<List<LayerCount>> layers(@RequestParam(defaultValue = "anhui") String region) {
        return ApiResult.ok(queries.layers(region));
    }

    @GetMapping("/api/geo/stats")
    public ApiResult<List<StatItem>> stats(@RequestParam(defaultValue = "anhui") String region,
                                           @RequestParam String layer) {
        return ApiResult.ok(queries.stats(region, layer));
    }

    @GetMapping("/api/geo/search")
    public ApiResult<List<SearchHit>> search(@RequestParam(defaultValue = "anhui") String region,
                                             @RequestParam String keyword,
                                             @RequestParam(required = false) String layer,
                                             @RequestParam(defaultValue = "8") int limit) {
        return ApiResult.ok(queries.search(region, keyword, layer, limit));
    }

    @GetMapping("/api/geo/analyze")
    public ApiResult<AnalysisResult> analyze(@RequestParam(defaultValue = "anhui") String region,
                                             @RequestParam String layer,
                                             @RequestParam double minLon,
                                             @RequestParam double minLat,
                                             @RequestParam double maxLon,
                                             @RequestParam double maxLat) {
        return ApiResult.ok(queries.analyze(region, layer, minLon, minLat, maxLon, maxLat));
    }
}
