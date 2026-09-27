package com.aomap.ingest.repo;

import com.aomap.ingest.domain.IngestJobRecord;
import com.aomap.ingest.domain.JobInsert;
import com.aomap.ingest.mapper.IngestJobMapper;
import com.aomap.ingest.model.JobView;
import com.aomap.ingest.model.LogView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class JobRepository {

    private static final TypeReference<Map<String, Long>> COUNT_TYPE = new TypeReference<>() {
    };

    private final IngestJobMapper jobs;
    private final ObjectMapper objectMapper;

    public JobRepository(IngestJobMapper jobs, ObjectMapper objectMapper) {
        this.jobs = jobs;
        this.objectMapper = objectMapper;
    }

    public void failStaleRunning() {
        jobs.failStaleRunning();
    }

    public long insertRunning(String regionCode, String regionName, String sourceUrl, String triggerType) {
        JobInsert job = new JobInsert();
        job.setRegionCode(regionCode);
        job.setRegionName(regionName);
        job.setSourceUrl(sourceUrl);
        job.setTriggerType(triggerType);
        try {
            jobs.insertRunning(job);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("已有下载入库任务正在执行");
        }
        if (job.getId() == null) {
            throw new IllegalStateException("创建任务失败");
        }
        return job.getId();
    }

    public void updateProgress(long id, String phase, int progress, String message) {
        jobs.updateProgress(id, phase, Math.max(0, Math.min(progress, 100)), message);
    }

    public void updateFile(long id, String path, long bytes) {
        jobs.updateFile(id, path, bytes);
    }

    public void succeed(long id, String message, long featureCount, Map<String, Long> layerCounts) {
        jobs.succeed(id, message, featureCount, toJson(layerCounts));
    }

    public void fail(long id, String message) {
        String text = message == null ? "任务失败" : message;
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        jobs.fail(id, text);
    }

    public void addLog(long jobId, String phase, String level, String message) {
        jobs.insertLog(jobId, phase, level, message);
    }

    public JobView find(long id) {
        return toView(jobs.findById(id));
    }

    public JobView latest() {
        return toView(jobs.findLatest());
    }

    public List<JobView> list(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return jobs.list(size).stream().map(this::toView).toList();
    }

    public List<LogView> logs(long jobId, long afterId) {
        return jobs.logs(jobId, afterId).stream()
                .map(row -> new LogView(row.getId(), row.getPhase(), row.getLevel(), row.getMessage(), instant(row.getCreatedAt())))
                .toList();
    }

    private JobView toView(IngestJobRecord row) {
        if (row == null) {
            return null;
        }
        return new JobView(
                row.getId(),
                row.getRegionCode(),
                row.getRegionName(),
                row.getStatus(),
                row.getPhase(),
                row.getProgress() == null ? 0 : row.getProgress(),
                row.getMessage(),
                row.getSourceUrl(),
                row.getFileBytes(),
                row.getFeatureCount(),
                parseCounts(row.getLayerCounts()),
                row.getTriggerType(),
                instant(row.getStartedAt()),
                instant(row.getFinishedAt()),
                row.getError()
        );
    }

    private Map<String, Long> parseCounts(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, COUNT_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Long> layerCounts) {
        try {
            return objectMapper.writeValueAsString(layerCounts == null ? Map.of() : layerCounts);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
