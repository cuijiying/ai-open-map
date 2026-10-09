package com.aomap.common.geo;

import java.util.List;
import java.util.Optional;

/**
 * Geofabrik 中国省级提取。code 对应
 * https://download.geofabrik.de/asia/china/{code}-latest.osm.pbf
 */
public final class Regions {

    public record Region(
            String code,
            String name,
            String shortName,
            String capital,
            double lon,
            double lat,
            double zoom,
            double capitalLon,
            double capitalLat
    ) {
        public String pbfUrl() {
            return "https://download.geofabrik.de/asia/china/" + code + "-latest.osm.pbf";
        }

        public String md5Url() {
            return pbfUrl() + ".md5";
        }
    }

    public static final List<Region> ALL = List.of(
            region("beijing", "北京市", "北京", "北京", 116.407, 39.904, 9.0, 116.407, 39.904),
            region("tianjin", "天津市", "天津", "天津", 117.201, 39.085, 9.0, 117.201, 39.085),
            region("hebei", "河北省", "河北", "石家庄", 115.50, 38.00, 6.5, 114.515, 38.042),
            region("shanxi", "山西省", "山西", "太原", 112.30, 37.60, 6.5, 112.549, 37.870),
            region("neimenggu", "内蒙古", "内蒙古", "呼和浩特", 113.00, 44.00, 5.0, 111.752, 40.842),
            region("liaoning", "辽宁省", "辽宁", "沈阳", 122.50, 41.30, 6.8, 123.432, 41.804),
            region("jilin", "吉林省", "吉林", "长春", 126.20, 43.70, 6.5, 125.324, 43.817),
            region("heilongjiang", "黑龙江省", "黑龙江", "哈尔滨", 127.80, 47.80, 5.5, 126.535, 45.803),
            region("shanghai", "上海市", "上海", "上海", 121.473, 31.230, 9.0, 121.473, 31.230),
            region("jiangsu", "江苏省", "江苏", "南京", 119.50, 33.00, 6.8, 118.796, 32.060),
            region("zhejiang", "浙江省", "浙江", "杭州", 120.20, 29.20, 7.0, 120.155, 30.274),
            region("anhui", "安徽省", "安徽", "合肥", 117.283, 31.861, 6.5, 117.227, 31.821),
            region("fujian", "福建省", "福建", "福州", 118.30, 26.00, 6.8, 119.296, 26.074),
            region("jiangxi", "江西省", "江西", "南昌", 116.00, 27.60, 6.8, 115.858, 28.683),
            region("shandong", "山东省", "山东", "济南", 118.20, 36.40, 6.8, 117.121, 36.651),
            region("henan", "河南省", "河南", "郑州", 113.70, 33.80, 6.8, 113.625, 34.746),
            region("hubei", "湖北省", "湖北", "武汉", 112.30, 31.00, 6.8, 114.305, 30.593),
            region("hunan", "湖南省", "湖南", "长沙", 111.70, 27.60, 6.8, 112.939, 28.228),
            region("guangdong", "广东省", "广东", "广州", 113.50, 23.40, 6.8, 113.264, 23.129),
            region("guangxi", "广西", "广西", "南宁", 108.80, 23.80, 6.5, 108.366, 22.817),
            region("hainan", "海南省", "海南", "海口", 109.70, 19.20, 7.5, 110.199, 20.044),
            region("chongqing", "重庆市", "重庆", "重庆", 107.70, 30.00, 7.0, 106.551, 29.563),
            region("sichuan", "四川省", "四川", "成都", 103.00, 30.60, 6.0, 104.066, 30.572),
            region("guizhou", "贵州省", "贵州", "贵阳", 106.90, 26.90, 7.0, 106.630, 26.647),
            region("yunnan", "云南省", "云南", "昆明", 101.50, 25.00, 6.0, 102.833, 24.880),
            region("xizang", "西藏", "西藏", "拉萨", 88.00, 31.50, 5.0, 91.172, 29.653),
            region("shaanxi", "陕西省", "陕西", "西安", 108.90, 35.20, 6.5, 108.940, 34.341),
            region("gansu", "甘肃省", "甘肃", "兰州", 100.50, 37.50, 5.5, 103.834, 36.061),
            region("qinghai", "青海省", "青海", "西宁", 96.50, 35.50, 5.5, 101.778, 36.617),
            region("ningxia", "宁夏", "宁夏", "银川", 106.30, 37.30, 7.5, 106.230, 38.487),
            region("xinjiang", "新疆", "新疆", "乌鲁木齐", 85.50, 41.50, 5.0, 87.617, 43.793)
    );

    private Regions() {
    }

    public static Optional<Region> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String wanted = code.trim();
        return ALL.stream().filter(region -> region.code().equalsIgnoreCase(wanted)).findFirst();
    }

    public static Region require(String code) {
        return find(code).orElseThrow(() -> new IllegalArgumentException("未知省份: " + code));
    }

    public static Region orDefault(String code) {
        return find(code).orElse(require("anhui"));
    }

    private static Region region(
            String code, String name, String shortName, String capital,
            double lon, double lat, double zoom, double capitalLon, double capitalLat
    ) {
        return new Region(code, name, shortName, capital, lon, lat, zoom, capitalLon, capitalLat);
    }
}
