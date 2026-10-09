package com.aomap.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "osm.ingest")
public class OsmIngestProperties {

    private String dataDir = "data/osm";
    private String cron = "0 0 3 * * ?";
    private boolean scheduleEnabled = true;
    private String userAgent = "AIOpenMap/0.1 (local WebGIS)";
    private boolean includeBuildings = true;
    private String scheduleRegion = "anhui";
    private List<Region> regions = new ArrayList<>();

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isIncludeBuildings() {
        return includeBuildings;
    }

    public void setIncludeBuildings(boolean includeBuildings) {
        this.includeBuildings = includeBuildings;
    }

    public String getScheduleRegion() {
        return scheduleRegion;
    }

    public void setScheduleRegion(String scheduleRegion) {
        this.scheduleRegion = scheduleRegion;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }

    public Region require(String code) {
        String wanted = (code == null || code.isBlank()) ? "anhui" : code.trim();
        return regions.stream()
                .filter(region -> wanted.equalsIgnoreCase(region.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未配置区域: " + wanted));
    }

    public static class Region {
        private String code;
        private String name;
        private String url;
        private String md5Url;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMd5Url() {
            return md5Url;
        }

        public void setMd5Url(String md5Url) {
            this.md5Url = md5Url;
        }
    }
}
