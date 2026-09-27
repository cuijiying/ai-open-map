package com.aomap.ingest.domain;

import java.sql.Timestamp;

public class IngestJobRecord {

    private Long id;
    private String regionCode;
    private String regionName;
    private String status;
    private String phase;
    private Integer progress;
    private String message;
    private String sourceUrl;
    private Long fileBytes;
    private Long featureCount;
    private String layerCounts;
    private String triggerType;
    private Timestamp startedAt;
    private Timestamp finishedAt;
    private String error;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Long getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(Long fileBytes) {
        this.fileBytes = fileBytes;
    }

    public Long getFeatureCount() {
        return featureCount;
    }

    public void setFeatureCount(Long featureCount) {
        this.featureCount = featureCount;
    }

    public String getLayerCounts() {
        return layerCounts;
    }

    public void setLayerCounts(String layerCounts) {
        this.layerCounts = layerCounts;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
