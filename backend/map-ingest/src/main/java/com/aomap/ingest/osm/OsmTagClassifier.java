package com.aomap.ingest.osm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OsmTagClassifier {

    private static final Set<String> SKIP_HIGHWAY = Set.of(
            "proposed", "construction", "abandoned", "raceway", "platform", "corridor",
            "bus_stop", "rest_area", "services", "elevator", "emergency_bay",
            "crossing", "traffic_signals", "mini_roundabout", "turning_circle",
            "passing_place", "milestone", "speed_camera", "stop", "give_way", "street_lamp", "no"
    );

    private static final Set<String> PLACE_TYPES = Set.of(
            "city", "town", "county", "suburb", "village", "hamlet",
            "neighbourhood", "quarter", "borough", "state", "province"
    );

    private static final List<String> POI_KEYS = List.of(
            "amenity", "shop", "tourism", "historic", "office", "healthcare"
    );

    private static final List<String> PROP_KEYS = List.of(
            "name", "name:zh", "highway", "railway", "waterway", "building", "landuse",
            "natural", "amenity", "shop", "tourism", "historic", "office", "leisure",
            "place", "boundary", "admin_level", "ref", "oneway", "bridge", "tunnel", "population"
    );

    private OsmTagClassifier() {
    }

    public record Kind(String layer, String subType, boolean polygon) {
    }

    public static Kind classifyWay(Map<String, String> tags, boolean includeBuildings) {
        String building = tags.get("building");
        if (includeBuildings && building != null && !"no".equals(building)) {
            return new Kind("building", clip(building), true);
        }
        String highway = tags.get("highway");
        if (highway != null && !SKIP_HIGHWAY.contains(highway)) {
            return new Kind("highway", clip(highway), false);
        }
        String railway = tags.get("railway");
        if (railway != null && !"abandoned".equals(railway) && !"proposed".equals(railway) && !"razed".equals(railway)) {
            return new Kind("railway", clip(railway), false);
        }
        String waterway = tags.get("waterway");
        if (waterway != null) {
            if ("riverbank".equals(waterway) || "dock".equals(waterway)) {
                return new Kind("water", clip(waterway), true);
            }
            return new Kind("waterway", clip(waterway), false);
        }
        String natural = tags.get("natural");
        if ("water".equals(natural) || "bay".equals(natural) || "wetland".equals(natural)) {
            return new Kind("water", clip(natural), true);
        }
        String landuse = tags.get("landuse");
        if (landuse != null) {
            return new Kind("landuse", clip(landuse), true);
        }
        String leisure = tags.get("leisure");
        if ("park".equals(leisure) || "garden".equals(leisure) || "pitch".equals(leisure) || "playground".equals(leisure)) {
            return new Kind("landuse", clip(leisure), true);
        }
        if ("administrative".equals(tags.get("boundary"))) {
            return new Kind("boundary", clip(tags.getOrDefault("admin_level", "administrative")), false);
        }
        return null;
    }

    public static Kind classifyNode(Map<String, String> tags) {
        String place = tags.get("place");
        if (place != null && PLACE_TYPES.contains(place)) {
            return named(tags) ? new Kind("place", clip(place), false) : null;
        }
        for (String key : POI_KEYS) {
            String value = tags.get(key);
            if (value != null && !"no".equals(value) && named(tags)) {
                return new Kind("poi", clip(key + "=" + value), false);
            }
        }
        return null;
    }

    public static String nameOf(Map<String, String> tags) {
        for (String key : List.of("name:zh", "name", "name:en", "ref")) {
            String value = tags.get(key);
            if (value != null && !value.isBlank()) {
                String trimmed = value.trim();
                return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
            }
        }
        return null;
    }

    public static String propsJson(Map<String, String> tags) {
        Map<String, String> kept = new LinkedHashMap<>();
        for (String key : PROP_KEYS) {
            String value = tags.get(key);
            if (value != null && !value.isBlank()) {
                kept.put(key, value.length() > 200 ? value.substring(0, 200) : value);
            }
        }
        if (kept.isEmpty()) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : kept.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append('"');
        }
        json.append('}');
        return json.toString();
    }

    private static boolean named(Map<String, String> tags) {
        return nameOf(tags) != null;
    }

    private static String clip(String value) {
        if (value == null || value.isBlank()) {
            return "yes";
        }
        return value.length() > 64 ? value.substring(0, 64) : value;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
