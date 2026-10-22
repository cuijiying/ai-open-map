package com.aomap.ingest.osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WktBuilder {

    private WktBuilder() {
    }

    public static String lineString(List<double[]> lonLat) {
        List<double[]> points = clean(lonLat);
        if (points.size() < 2) {
            return null;
        }
        return "LINESTRING(" + join(points) + ")";
    }

    public static String polygonRings(List<List<double[]>> rings) {
        if (rings == null || rings.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("POLYGON(");
        int written = 0;
        for (List<double[]> ring : rings) {
            String body = ringBody(ring);
            if (body == null) {
                continue;
            }
            if (written > 0) {
                builder.append(',');
            }
            builder.append('(').append(body).append(')');
            written++;
        }
        if (written == 0) {
            return null;
        }
        return builder.append(')').toString();
    }

    public static String multiPolygon(List<List<List<double[]>>> polygons) {
        if (polygons == null || polygons.isEmpty()) {
            return null;
        }
        List<String> bodies = new ArrayList<>();
        for (List<List<double[]>> polygon : polygons) {
            String single = polygonRings(polygon);
            if (single == null || !single.startsWith("POLYGON")) {
                continue;
            }
            bodies.add(single.substring("POLYGON".length()));
        }
        if (bodies.isEmpty()) {
            return null;
        }
        if (bodies.size() == 1) {
            return "POLYGON" + bodies.get(0);
        }
        StringBuilder builder = new StringBuilder("MULTIPOLYGON(");
        for (int i = 0; i < bodies.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(bodies.get(i));
        }
        return builder.append(')').toString();
    }

    public static String polygon(List<double[]> lonLat) {
        List<double[]> points = clean(lonLat);
        if (points.size() < 3) {
            return null;
        }
        double[] first = points.get(0);
        double[] last = points.get(points.size() - 1);
        if (!same(first, last)) {
            points.add(new double[]{first[0], first[1]});
        }
        if (points.size() < 4) {
            return null;
        }
        return "POLYGON((" + join(points) + "))";
    }

    private static List<double[]> clean(List<double[]> lonLat) {
        List<double[]> points = new ArrayList<>(lonLat.size());
        double[] previous = null;
        for (double[] point : lonLat) {
            if (point == null || point.length < 2 || !Double.isFinite(point[0]) || !Double.isFinite(point[1])) {
                continue;
            }
            if (point[0] < -180 || point[0] > 180 || point[1] < -90 || point[1] > 90) {
                continue;
            }
            if (previous != null && same(previous, point)) {
                continue;
            }
            double[] copy = new double[]{point[0], point[1]};
            points.add(copy);
            previous = copy;
        }
        return points;
    }

    private static boolean same(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < 1e-7 && Math.abs(a[1] - b[1]) < 1e-7;
    }

    private static String ringBody(List<double[]> lonLat) {
        List<double[]> points = clean(lonLat);
        if (points.size() < 3) {
            return null;
        }
        double[] first = points.get(0);
        double[] last = points.get(points.size() - 1);
        if (!same(first, last)) {
            points.add(new double[]{first[0], first[1]});
        }
        if (points.size() < 4) {
            return null;
        }
        return join(points);
    }

    private static String join(List<double[]> points) {
        StringBuilder builder = new StringBuilder(points.size() * 24);
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            double[] point = points.get(i);
            builder.append(String.format(Locale.ROOT, "%.7f %.7f", point[0], point[1]));
        }
        return builder.toString();
    }
}
