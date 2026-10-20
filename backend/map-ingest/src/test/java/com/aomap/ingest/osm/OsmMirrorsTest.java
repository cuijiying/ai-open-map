package com.aomap.ingest.osm;

import com.aomap.common.geo.Regions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsmMirrorsTest {

    @Test
    void prefersOpenStreetMapFranceThenGeofabrik() {
        List<OsmMirrors.Source> sources = OsmMirrors.sources(Regions.require("anhui"));

        assertEquals("osm-france", sources.get(0).id());
        assertEquals("OSM France", sources.get(0).name());
        assertEquals(
                "https://download.openstreetmap.fr/extracts/asia/china/anhui-latest.osm.pbf",
                sources.get(0).pbfUrl()
        );
        assertEquals(
                "https://download.openstreetmap.fr/extracts/asia/china/anhui.osm.pbf.md5",
                sources.get(0).md5Url()
        );
        assertEquals("https://download.geofabrik.de/asia/china/anhui-latest.osm.pbf", sources.get(1).pbfUrl());
        assertEquals(List.of("geofabrik"), OsmMirrors.resolve(Regions.require("anhui"), "geofabrik").stream().map(OsmMirrors.Source::id).toList());
        assertEquals(2, OsmMirrors.resolve(Regions.require("anhui"), "auto").size());
        assertEquals("OSM France", OsmMirrors.nameOf(sources.get(0).pbfUrl()));
        assertEquals("Geofabrik", OsmMirrors.nameOf(sources.get(1).pbfUrl()));
    }

    @Test
    void mapsProvinceNamesThatDifferOnOpenStreetMapFrance() {
        assertTrue(OsmMirrors.sources(Regions.require("neimenggu")).get(0).pbfUrl()
                .endsWith("/inner_mongolia-latest.osm.pbf"));
        assertTrue(OsmMirrors.sources(Regions.require("xizang")).get(0).pbfUrl()
                .endsWith("/tibet-latest.osm.pbf"));
        assertTrue(OsmMirrors.sources(Regions.require("shaanxi")).get(0).pbfUrl()
                .endsWith("/shaanxi-latest.osm.pbf"));
        assertTrue(OsmMirrors.sources(Regions.require("shanxi")).get(0).pbfUrl()
                .endsWith("/shanxi-latest.osm.pbf"));
    }

    @Test
    void readsIpv4FromAliDnsAnswer() {
        String json = "{\"Status\":0,\"Answer\":["
                + "{\"type\":5,\"data\":\"cluster-moji.openstreetmap.fr.\"},"
                + "{\"type\":1,\"data\":\"45.147.209.254\"}]}";
        assertEquals("45.147.209.254", ReachableClient.ipv4FromDoh(json));
    }
}
