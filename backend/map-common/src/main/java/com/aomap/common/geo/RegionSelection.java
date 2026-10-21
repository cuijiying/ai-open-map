package com.aomap.common.geo;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 地图一次可以读取多个省级区域。regions 优先于单个 region。
 */
public final class RegionSelection {

    private RegionSelection() {
    }

    public static List<String> resolve(String regions, String region) {
        if (regions != null) {
            List<String> parsed = parse(regions);
            if (parsed.isEmpty() && !regions.isBlank()) {
                throw new IllegalArgumentException("未知省份: " + regions);
            }
            return parsed;
        }
        if (region == null || region.isBlank()) {
            return List.of("anhui");
        }
        return List.of(Regions.require(region).code());
    }

    public static List<String> parse(String raw) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        for (String part : raw.split(",")) {
            Regions.find(part).ifPresent(region -> codes.add(region.code()));
        }
        return List.copyOf(codes);
    }
}
