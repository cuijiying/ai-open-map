package com.aomap.ingest.osm;

import com.aomap.common.geo.Regions.Region;

import java.util.List;
import java.util.Map;

/**
 * 省级 OSM PBF 下载源。OSM France 从国内可直连，Geofabrik 是官方源。
 */
public final class OsmMirrors {

    public static final String FRANCE = "osm-france";
    public static final String GEOFABRIK = "geofabrik";
    public static final String AUTO = "auto";

    public record Source(String id, String name, String pbfUrl, String md5Url) {
    }

    private static final Map<String, String> FRANCE_SLUG = Map.of(
            "neimenggu", "inner_mongolia",
            "xizang", "tibet"
    );

    private OsmMirrors() {
    }

    public static List<Source> sources(Region region) {
        String france = FRANCE_SLUG.getOrDefault(region.code(), region.code());
        return List.of(
                new Source(
                        FRANCE,
                        "OSM France",
                        "https://download.openstreetmap.fr/extracts/asia/china/" + france + "-latest.osm.pbf",
                        "https://download.openstreetmap.fr/extracts/asia/china/" + france + ".osm.pbf.md5"
                ),
                new Source(
                        GEOFABRIK,
                        "Geofabrik",
                        region.pbfUrl(),
                        region.md5Url()
                )
        );
    }

    public static List<Source> resolve(Region region, String sourceId) {
        if (sourceId == null || sourceId.isBlank() || AUTO.equalsIgnoreCase(sourceId)) {
            return sources(region);
        }
        return List.of(require(region, sourceId));
    }

    public static Source require(Region region, String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("请选择下载源");
        }
        return sources(region).stream()
                .filter(source -> source.id().equalsIgnoreCase(sourceId.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知下载源: " + sourceId));
    }

    public static String nameOf(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        if (url.contains("openstreetmap.fr")) {
            return "OSM France";
        }
        if (url.contains("geofabrik.de")) {
            return "Geofabrik";
        }
        return "其他源";
    }
}
