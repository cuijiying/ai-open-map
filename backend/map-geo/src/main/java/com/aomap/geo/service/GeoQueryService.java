package com.aomap.geo.service;

import com.aomap.common.dto.GeoDtos.AnalysisResult;
import com.aomap.common.dto.GeoDtos.LayerCount;
import com.aomap.common.dto.GeoDtos.SearchHit;
import com.aomap.common.dto.GeoDtos.StatItem;
import com.aomap.common.geo.Layers;
import com.aomap.geo.domain.AnalysisRow;
import com.aomap.geo.domain.NameCount;
import com.aomap.geo.domain.SearchRow;
import com.aomap.geo.mapper.GeoQueryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeoQueryService {

    private final GeoQueryMapper queries;

    public GeoQueryService(GeoQueryMapper queries) {
        this.queries = queries;
    }

    public List<LayerCount> layers() {
        Map<String, Long> counts = new HashMap<>();
        for (NameCount row : queries.countByLayer()) {
            counts.put(row.getName(), row.getCount() == null ? 0L : row.getCount());
        }
        List<LayerCount> result = new ArrayList<>();
        for (String layer : Layers.ORDER) {
            result.add(new LayerCount(layer, Layers.label(layer), counts.getOrDefault(layer, 0L)));
        }
        return result;
    }

    public List<StatItem> stats(String layer) {
        requireLayer(layer);
        List<StatItem> items = new ArrayList<>();
        for (NameCount row : queries.stats(layer)) {
            items.add(new StatItem(row.getName(), row.getCount() == null ? 0L : row.getCount()));
        }
        return items;
    }

    public List<SearchHit> search(String keyword, String layer, int limit) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("请输入要查找的名称");
        }
        if (layer != null && !layer.isBlank() && !Layers.known(layer)) {
            throw new IllegalArgumentException("未知图层: " + layer);
        }
        int size = Math.max(1, Math.min(limit, 30));
        String pattern = likePattern(keyword.trim());
        String layerArg = layer == null || layer.isBlank() ? null : layer;
        List<SearchHit> hits = new ArrayList<>();
        for (SearchRow row : queries.search(pattern, layerArg, size)) {
            hits.add(new SearchHit(
                    row.getOsmId() == null ? 0L : row.getOsmId(),
                    row.getLayer(),
                    row.getSubtype(),
                    row.getName(),
                    row.getLon() == null ? 0 : row.getLon(),
                    row.getLat() == null ? 0 : row.getLat()
            ));
        }
        return hits;
    }

    public AnalysisResult analyze(String layer, double minLon, double minLat, double maxLon, double maxLat) {
        requireLayer(layer);
        if (!(minLon < maxLon) || !(minLat < maxLat)
                || minLon < -180 || maxLon > 180 || minLat < -90 || maxLat > 90) {
            throw new IllegalArgumentException("视野范围无效");
        }
        AnalysisRow row = queries.analyze(layer, minLon, minLat, maxLon, maxLat);
        if (row == null) {
            return new AnalysisResult(layer, 0, 0, 0);
        }
        return new AnalysisResult(
                layer,
                row.getCount() == null ? 0 : row.getCount(),
                row.getLengthMeters() == null ? 0 : row.getLengthMeters(),
                row.getAreaSquareMeters() == null ? 0 : row.getAreaSquareMeters()
        );
    }

    private static void requireLayer(String layer) {
        if (!Layers.known(layer)) {
            throw new IllegalArgumentException("未知图层: " + layer);
        }
    }

    static String likePattern(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
