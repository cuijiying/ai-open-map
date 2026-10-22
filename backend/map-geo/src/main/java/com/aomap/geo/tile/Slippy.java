package com.aomap.geo.tile;

import java.util.ArrayList;
import java.util.List;

/** 经纬度与 XYZ 瓦片编号。中国范围用固定包围盒覆盖，不依赖数据库。 */
public final class Slippy {

    public static final double CHINA_MIN_LON = 73.5;
    public static final double CHINA_MIN_LAT = 18.0;
    public static final double CHINA_MAX_LON = 135.0;
    public static final double CHINA_MAX_LAT = 53.6;

    private Slippy() {
    }

    public record Tile(int z, int x, int y) {
    }

    public static List<Tile> coverChina(int maxZoom) {
        return cover(CHINA_MIN_LON, CHINA_MIN_LAT, CHINA_MAX_LON, CHINA_MAX_LAT, maxZoom);
    }

    public static List<Tile> cover(double minLon, double minLat, double maxLon, double maxLat, int maxZoom) {
        List<Tile> tiles = new ArrayList<>();
        int limit = Math.max(0, maxZoom);
        for (int z = 0; z <= limit; z++) {
            int span = 1 << z;
            int x0 = clamp(xOf(minLon, z), span);
            int x1 = clamp(xOf(maxLon, z), span);
            int yNorth = clamp(yOf(maxLat, z), span);
            int ySouth = clamp(yOf(minLat, z), span);
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int y = Math.min(yNorth, ySouth); y <= Math.max(yNorth, ySouth); y++) {
                    tiles.add(new Tile(z, x, y));
                }
            }
        }
        return tiles;
    }

    public static int xOf(double lon, int z) {
        double n = 1L << z;
        return (int) Math.floor((lon + 180.0) / 360.0 * n);
    }

    public static int yOf(double lat, int z) {
        double n = 1L << z;
        double radians = Math.toRadians(lat);
        double mercator = Math.log(Math.tan(radians) + 1.0 / Math.cos(radians));
        return (int) Math.floor((1.0 - mercator / Math.PI) / 2.0 * n);
    }

    private static int clamp(int value, int span) {
        if (value < 0) {
            return 0;
        }
        if (value >= span) {
            return span - 1;
        }
        return value;
    }
}
