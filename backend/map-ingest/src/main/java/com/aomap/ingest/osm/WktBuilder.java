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
