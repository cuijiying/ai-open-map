package com.aomap.common.geo;

import java.util.List;
import java.util.Set;

public final class Layers {

    public static final List<String> ORDER = List.of(
            "landuse", "water", "building", "waterway", "railway",
            "highway", "boundary", "poi", "place"
    );

    private static final Set<String> ALL = Set.copyOf(ORDER);

    private Layers() {
    }

    public static boolean known(String layer) {
        return layer != null && ALL.contains(layer);
    }

    public static String label(String layer) {
        return switch (layer) {
            case "highway" -> "道路";
            case "railway" -> "铁路";
            case "waterway" -> "水系线";
            case "water" -> "水体";
            case "building" -> "建筑";
            case "landuse" -> "土地利用";
            case "boundary" -> "行政区划";
            case "poi" -> "兴趣点";
            case "place" -> "地名";
            default -> layer;
        };
    }
}
