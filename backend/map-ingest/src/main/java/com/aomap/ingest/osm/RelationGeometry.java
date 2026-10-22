package com.aomap.ingest.osm;

import java.util.ArrayList;
import java.util.List;

/**
 * 把关系成员里的道路线段收成面。只处理已经分类为面的 outer / inner，不处理路线关系。
 */
public final class RelationGeometry {

    private RelationGeometry() {
    }

    public static String areaWkt(List<List<double[]>> outerSegments, List<List<double[]>> innerSegments) {
        List<List<double[]>> outers = chain(outerSegments);
        if (outers.isEmpty()) {
            return null;
        }
        List<List<double[]>> inners = chain(innerSegments);
        List<List<List<double[]>>> polygons = new ArrayList<>();
        for (List<double[]> outer : outers) {
            List<List<double[]>> rings = new ArrayList<>();
            rings.add(outer);
            polygons.add(rings);
        }
        for (List<double[]> inner : inners) {
            int owner = ownerOf(outers, inner.get(0)[0], inner.get(0)[1]);
            if (owner >= 0) {
                polygons.get(owner).add(inner);
            }
        }
        return WktBuilder.multiPolygon(polygons);
    }

    static List<List<double[]>> chain(List<List<double[]>> segments) {
        List<List<double[]>> pending = new ArrayList<>();
        if (segments != null) {
            for (List<double[]> segment : segments) {
                List<double[]> cleaned = clean(segment);
                if (cleaned.size() >= 2) {
                    pending.add(cleaned);
                }
            }
        }
        List<List<double[]>> rings = new ArrayList<>();
        while (!pending.isEmpty()) {
            List<double[]> ring = new ArrayList<>(pending.remove(0));
            boolean grew = true;
            while (grew && !closed(ring)) {
                grew = false;
                for (int i = 0; i < pending.size(); i++) {
                    if (attach(ring, pending.get(i))) {
                        pending.remove(i);
                        grew = true;
                        break;
                    }
                }
            }
            if (closed(ring)) {
                rings.add(ring);
            }
        }
        return rings;
    }

    private static int ownerOf(List<List<double[]>> outers, double lon, double lat) {
        int best = -1;
        double bestArea = Double.POSITIVE_INFINITY;
        for (int i = 0; i < outers.size(); i++) {
            if (!contains(outers.get(i), lon, lat)) {
                continue;
            }
            double area = Math.abs(ringArea(outers.get(i)));
            if (area < bestArea) {
                bestArea = area;
                best = i;
            }
        }
        return best;
    }

    private static boolean contains(List<double[]> ring, double lon, double lat) {
        boolean inside = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            double xi = ring.get(i)[0];
            double yi = ring.get(i)[1];
            double xj = ring.get(j)[0];
            double yj = ring.get(j)[1];
            boolean crosses = (yi > lat) != (yj > lat)
                    && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi;
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double ringArea(List<double[]> ring) {
        double sum = 0;
        for (int i = 0; i < ring.size() - 1; i++) {
            double[] a = ring.get(i);
            double[] b = ring.get(i + 1);
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return sum / 2;
    }

    private static boolean attach(List<double[]> ring, List<double[]> segment) {
        double[] head = ring.get(0);
        double[] tail = ring.get(ring.size() - 1);
        double[] start = segment.get(0);
        double[] end = segment.get(segment.size() - 1);
        if (same(tail, start)) {
            ring.addAll(segment.subList(1, segment.size()));
            return true;
        }
        if (same(tail, end)) {
            for (int i = segment.size() - 2; i >= 0; i--) {
                ring.add(segment.get(i));
            }
            return true;
        }
        if (same(head, end)) {
            List<double[]> merged = new ArrayList<>(segment);
            merged.addAll(ring.subList(1, ring.size()));
            ring.clear();
            ring.addAll(merged);
            return true;
        }
        if (same(head, start)) {
            List<double[]> merged = new ArrayList<>();
            for (int i = segment.size() - 1; i >= 1; i--) {
                merged.add(segment.get(i));
            }
            merged.addAll(ring);
            ring.clear();
            ring.addAll(merged);
            return true;
        }
        return false;
    }

    private static List<double[]> clean(List<double[]> segment) {
        List<double[]> cleaned = new ArrayList<>();
        if (segment == null) {
            return cleaned;
        }
        for (double[] point : segment) {
            if (point == null || point.length < 2 || !Double.isFinite(point[0]) || !Double.isFinite(point[1])) {
                continue;
            }
            if (!cleaned.isEmpty() && same(cleaned.get(cleaned.size() - 1), point)) {
                continue;
            }
            cleaned.add(new double[]{point[0], point[1]});
        }
        return cleaned;
    }

    private static boolean closed(List<double[]> ring) {
        return ring.size() >= 4 && same(ring.get(0), ring.get(ring.size() - 1));
    }

    private static boolean same(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 1e-7 && Math.abs(a[1] - b[1]) < 1e-7;
    }
}
