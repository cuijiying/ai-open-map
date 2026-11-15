package com.aomap.ingest.service;

import com.aomap.common.geo.Regions;
import com.aomap.common.geo.Regions.Region;
import com.aomap.ingest.config.OsmIngestProperties;
import com.aomap.ingest.model.JobView;
import com.aomap.ingest.model.LogView;
import com.aomap.ingest.model.SourceOption;
import com.aomap.ingest.model.SourceProbe;
import com.aomap.ingest.osm.OsmDownloader;
import com.aomap.ingest.osm.OsmImporter;
import com.aomap.ingest.osm.OsmMirrors;
import com.aomap.ingest.osm.OsmMirrors.Source;
import com.aomap.ingest.repo.FeatureRepository;
import com.aomap.ingest.repo.JobRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final OsmIngestProperties properties;
    private final JobRepository jobs;
    private final FeatureRepository features;
    private final OsmDownloader downloader;
    private final OsmImporter importer;
    private final ExecutorService executor;

    public IngestService(
            OsmIngestProperties properties,
            JobRepository jobs,
            FeatureRepository features,
            OsmDownloader downloader,
            OsmImporter importer
    ) {
        this.properties = properties;
        this.jobs = jobs;
        this.features = features;
        this.downloader = downloader;
        this.importer = importer;
        int pool = Math.max(1, Math.min(properties.getParallelism(), 4));
        this.executor = Executors.newFixedThreadPool(pool, runnable -> {
            Thread thread = new Thread(runnable, "osm-ingest");
            thread.setDaemon(true);
            return thread;
        });
        jobs.failStaleRunning();
    }

    public List<RegionStatus> regions() {
        Map<String, Long> counts = features.countByRegion();
        return Regions.ALL.stream()
                .map(region -> new RegionStatus(
                        region.code(),
                        region.name(),
                        region.lon(),
                        region.lat(),
                        region.zoom(),
                        counts.getOrDefault(region.code(), 0L)
                ))
                .toList();
    }

    public List<SourceOption> sources(String regionCode) {
        Region region = Regions.require(regionCode);
        return OsmMirrors.sources(region).stream()
                .map(source -> new SourceOption(source.id(), source.name(), source.pbfUrl(), noteOf(source.id())))
                .toList();
    }

    public SourceProbe testSource(String regionCode, String sourceId) {
        Region region = Regions.require(regionCode);
        Source source = OsmMirrors.require(region, sourceId);
        return downloader.probe(properties, source);
    }

    public JobView start(String regionCode, String sourceId, String triggerType) {
        String code = regionCode == null || regionCode.isBlank() ? properties.getScheduleRegion() : regionCode;
        Region region = Regions.require(code);
        List<Source> plan = OsmMirrors.resolve(region, sourceId);
        long id = jobs.insertRunning(region.code(), region.name(), plan.get(0).pbfUrl(), triggerType);
        String label = plan.size() == 1 ? plan.get(0).name() : "自动（优先 " + plan.get(0).name() + "）";
        jobs.addLog(id, "DOWNLOAD", "INFO", "任务已排队，区域 " + region.name() + "，下载源 " + label);
        executor.submit(() -> run(id, region, sourceId));
        return jobs.find(id);
    }

    public JobView cancel(long id) {
        JobView existing = jobs.find(id);
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!jobs.cancel(id)) {
            throw new IllegalStateException("任务已结束，不能取消");
        }
        jobs.addLog(id, "FAILED", "INFO", "任务已取消");
        return jobs.find(id);
    }

    public JobView latest() {
        return jobs.latest();
    }

    public JobView get(long id) {
        JobView job = jobs.find(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return job;
    }

    public List<JobView> list(int limit) {
        return jobs.list(limit);
    }

    public List<LogView> logs(long id, long afterId) {
        get(id);
        return jobs.logs(id, afterId);
    }

    private void run(long id, Region region, String sourceId) {
        int[] logged = {-1};
        String[] phase = {""};
        IngestProgress progress = (nextPhase, percent, message) -> {
            if (!jobs.updateProgress(id, nextPhase, percent, message)) {
                throw new CancellationException("任务已取消");
            }
            if (!nextPhase.equals(phase[0]) || percent - logged[0] >= 10) {
                jobs.addLog(id, nextPhase, "INFO", message);
                phase[0] = nextPhase;
                logged[0] = percent;
            }
        };
        try {
            if (!jobs.claim(id)) {
                return;
            }
            Path file = downloader.download(properties, region, sourceId, progress, source -> jobs.updateSource(id, source.pbfUrl()));
            ensureRunning(id);
            jobs.updateFile(id, file.toAbsolutePath().toString(), Files.size(file));
            long count = importer.importFile(file, id, region.code(), properties.isIncludeBuildings(), progress);
            ensureRunning(id);
            Map<String, Long> layers = features.countByLayer(region.code());
            String summary = region.name() + " 数据已入库，共 " + count + " 条要素";
            jobs.addLog(id, "SUCCESS", "INFO", summary);
            jobs.succeed(id, summary, count, layers);
            log.info(summary);
        } catch (CancellationException ex) {
            log.info("入库任务已取消: {}", id);
        } catch (Exception ex) {
            if (isCancel(ex)) {
                log.info("入库任务已取消: {}", id);
                return;
            }
            log.error("入库任务失败", ex);
            String message = rootMessage(ex);
            jobs.addLog(id, "FAILED", "ERROR", message);
            jobs.fail(id, message);
        }
    }

    private void ensureRunning(long id) {
        JobView job = jobs.find(id);
        if (job == null || !"RUNNING".equals(job.status())) {
            throw new CancellationException("任务已取消");
        }
    }

    private static boolean isCancel(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof CancellationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? "任务失败" : message;
    }

    public record RegionStatus(String code, String name, double lon, double lat, double zoom, long featureCount) {
    }

    private static String noteOf(String sourceId) {
        if (OsmMirrors.FRANCE.equals(sourceId)) {
            return "国内网络通常可直连";
        }
        if (OsmMirrors.GEOFABRIK.equals(sourceId)) {
            return "官方每日更新，国内可能超时";
        }
        return "";
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
