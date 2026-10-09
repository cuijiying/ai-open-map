package com.aomap.ai.service;

import com.aomap.common.api.ApiResult;
import com.aomap.common.dto.GeoDtos.AnalysisResult;
import com.aomap.common.dto.GeoDtos.LayerCount;
import com.aomap.common.dto.GeoDtos.SearchHit;
import com.aomap.common.dto.GeoDtos.StatItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class GeoApi {

    private final RestClient client;

    public GeoApi(@Value("${app.geo.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<LayerCount> layers(String region) {
        String uri = UriComponentsBuilder.fromPath("/api/geo/layers").queryParam("region", region).build().toUriString();
        return body(client.get().uri(uri), new ParameterizedTypeReference<ApiResult<List<LayerCount>>>() {
        });
    }

    public List<StatItem> stats(String region, String layer) {
        String uri = UriComponentsBuilder.fromPath("/api/geo/stats").queryParam("region", region).queryParam("layer", layer).build().toUriString();
        return body(client.get().uri(uri), new ParameterizedTypeReference<ApiResult<List<StatItem>>>() {
        });
    }

    public List<SearchHit> search(String region, String keyword, String layer) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/geo/search")
                .queryParam("region", region)
                .queryParam("keyword", keyword)
                .queryParam("limit", 8);
        if (layer != null && !layer.isBlank()) {
            builder.queryParam("layer", layer);
        }
        return body(client.get().uri(builder.build().toUriString()), new ParameterizedTypeReference<ApiResult<List<SearchHit>>>() {
        });
    }

    public AnalysisResult analyze(String region, String layer, double minLon, double minLat, double maxLon, double maxLat) {
        String uri = UriComponentsBuilder.fromPath("/api/geo/analyze")
                .queryParam("region", region)
                .queryParam("layer", layer)
                .queryParam("minLon", minLon)
                .queryParam("minLat", minLat)
                .queryParam("maxLon", maxLon)
                .queryParam("maxLat", maxLat)
                .build().toUriString();
        return body(client.get().uri(uri), new ParameterizedTypeReference<ApiResult<AnalysisResult>>() {
        });
    }

    private <T> T body(RestClient.RequestHeadersSpec<?> spec, ParameterizedTypeReference<ApiResult<T>> type) {
        ApiResult<T> result;
        try {
            result = spec.retrieve().body(type);
        } catch (Exception ex) {
            throw new IllegalStateException("地图服务暂不可用：" + ex.getMessage());
        }
        if (result == null || result.code() != 0) {
            throw new IllegalStateException(result == null ? "地图服务暂不可用" : result.message());
        }
        return result.data();
    }
}
