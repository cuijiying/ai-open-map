package com.aomap.geo.tile;

import java.util.List;

public record PrebuildStatus(
        String state,
        int progress,
        String message,
        int maxZoom,
        List<String> regions,
        List<String> layers,
        int tileCount
) {
    public static PrebuildStatus idle() {
        return new PrebuildStatus("IDLE", 0, "还没有预生成全国低缩放瓦片", 6, List.of(), List.of(), 0);
    }

    public static PrebuildStatus running(int progress, String message) {
        return new PrebuildStatus("RUNNING", progress, message, 0, List.of(), List.of(), 0);
    }

    public static PrebuildStatus success(List<String> regions, int maxZoom, List<String> layers, int tileCount, String message) {
        return new PrebuildStatus("SUCCESS", 100, message, maxZoom, regions, layers, tileCount);
    }

    public static PrebuildStatus failed(String message) {
        return new PrebuildStatus("FAILED", 0, message, 0, List.of(), List.of(), 0);
    }
}
