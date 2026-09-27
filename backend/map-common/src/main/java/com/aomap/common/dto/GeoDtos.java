package com.aomap.common.dto;

public final class GeoDtos {

    private GeoDtos() {
    }

    public record LayerCount(String layer, String label, long count) {
    }

    public record StatItem(String subtype, long count) {
    }

    public record SearchHit(long osmId, String layer, String subtype, String name, double lon, double lat) {
    }

    public record AnalysisResult(String layer, long count, double lengthMeters, double areaSquareMeters) {
    }
}
