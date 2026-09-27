package com.aomap.ai.dialogue;

import java.util.List;
import java.util.Map;

public record MapAction(
        String type,
        List<String> layers,
        Map<String, List<String>> filters,
        List<Double> center,
        Double zoom,
        String label,
        List<Marker> points
) {
    public record Marker(String name, String layer, String subtype, double lon, double lat) {
    }
}
