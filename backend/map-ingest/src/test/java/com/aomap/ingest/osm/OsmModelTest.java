package com.aomap.ingest.osm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsmModelTest {

    @Test
    void classifiesRoadsBuildingsAndSkipsProposed() {
        OsmTagClassifier.Kind road = OsmTagClassifier.classifyWay(Map.of("highway", "primary", "name", "长江路"), true);
        assertNotNull(road);
        assertEquals("highway", road.layer());
        assertEquals("primary", road.subType());

        OsmTagClassifier.Kind building = OsmTagClassifier.classifyWay(Map.of("building", "yes"), true);
        assertNotNull(building);
        assertTrue(building.polygon());
        assertNull(OsmTagClassifier.classifyWay(Map.of("building", "yes"), false));
        assertNull(OsmTagClassifier.classifyWay(Map.of("highway", "proposed"), true));
    }

    @Test
    void namedPlaceAndPoiArePointsWithoutNameAreDropped() {
        OsmTagClassifier.Kind place = OsmTagClassifier.classifyNode(Map.of("place", "city", "name:zh", "合肥"));
        assertNotNull(place);
        assertEquals("place", place.layer());
        assertEquals("合肥", OsmTagClassifier.nameOf(Map.of("place", "city", "name:zh", "合肥", "name", "Hefei")));
        assertNull(OsmTagClassifier.classifyNode(Map.of("amenity", "school")));

        OsmTagClassifier.Kind poi = OsmTagClassifier.classifyNode(Map.of("amenity", "school", "name", "安徽大学"));
        assertNotNull(poi);
        assertEquals("poi", poi.layer());
        assertEquals("amenity=school", poi.subType());
    }

    @Test
    void buildsLineAndClosedPolygon() {
        String line = WktBuilder.lineString(List.of(new double[]{117.1, 31.2}, new double[]{117.2, 31.3}));
        assertEquals("LINESTRING(117.1000000 31.2000000,117.2000000 31.3000000)", line);

        String polygon = WktBuilder.polygon(List.of(
                new double[]{117.0, 31.0},
                new double[]{117.1, 31.0},
                new double[]{117.1, 31.1}
        ));
        assertNotNull(polygon);
        assertTrue(polygon.startsWith("POLYGON(("));
        assertTrue(polygon.endsWith("117.0000000 31.0000000))"));
        assertNull(WktBuilder.lineString(List.of(new double[]{117.0, 31.0})));
    }

    @Test
    void packsCoordinatesRoundTrip() {
        long packed = OsmImporter.pack(117.227239, 31.820567);
        assertEquals(117.227239, OsmImporter.unpackLon(packed), 1e-6);
        assertEquals(31.820567, OsmImporter.unpackLat(packed), 1e-6);
    }
}
