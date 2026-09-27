package com.aomap.geo.domain;

public class AnalysisRow {

    private Long count;
    private Double lengthMeters;
    private Double areaSquareMeters;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getLengthMeters() {
        return lengthMeters;
    }

    public void setLengthMeters(Double lengthMeters) {
        this.lengthMeters = lengthMeters;
    }

    public Double getAreaSquareMeters() {
        return areaSquareMeters;
    }

    public void setAreaSquareMeters(Double areaSquareMeters) {
        this.areaSquareMeters = areaSquareMeters;
    }
}
