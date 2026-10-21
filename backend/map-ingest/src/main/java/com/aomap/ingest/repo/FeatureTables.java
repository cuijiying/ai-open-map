package com.aomap.ingest.repo;

import com.aomap.common.geo.Regions;

import java.util.regex.Pattern;

/**
 * 每个省一块分区。发布时先写独立缓冲表，再在短事务里换上，避免切片长时间锁住已发布的数据。
 */
public final class FeatureTables {

    private static final Pattern CODE = Pattern.compile("[a-z]{2,32}");

    private FeatureTables() {
    }

    public static String requireCode(String code) {
        String normalized = Regions.require(code).code();
        if (!CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("非法区域代码: " + code);
        }
        return normalized;
    }

    public static String partition(String code) {
        return "osm_feature_p_" + requireCode(code);
    }

    public static String build(String code) {
        return "osm_feature_b_" + requireCode(code);
    }

    public static String retired(String code) {
        return "osm_feature_o_" + requireCode(code);
    }
}
