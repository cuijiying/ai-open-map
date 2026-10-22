package com.aomap.geo.tile;

import com.aomap.geo.mapper.TileMapper;
import com.aomap.geo.tile.NationalTileStore.Manifest;
import com.aomap.geo.tile.Slippy.Tile;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TileWarmupService {

    private static final Logger log = LoggerFactory.getLogger(TileWarmupService.class);
    private static final int DEFAULT_MAX_ZOOM = 6;
    private static final int ALLOWED_MAX_ZOOM = 8;
    private static final List<String> CANDIDATE_LAYERS = List.of(
            "highway", "railway", "waterway", "water", "boundary", "place"
    );

    private final TileMapper tiles;
    private final NationalTileStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "national-tiles");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile PrebuildStatus status;

    public TileWarmupService(TileMapper tiles, NationalTileStore store) {
        this.tiles = tiles;
        this.store = store;
    }

    public PrebuildStatus start(int maxZoom) {
        if (maxZoom < 0 || maxZoom > ALLOWED_MAX_ZOOM) {
            throw new IllegalArgumentException("预生成缩放只能是 0 到 " + ALLOWED_MAX_ZOOM);
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("已有全国瓦片预生成任务在执行");
        }
        status = PrebuildStatus.running(0, "开始预生成全国低缩放瓦片");
        executor.submit(() -> run(maxZoom));
        return status;
    }

    public PrebuildStatus status() {
        if (running.get()) {
            return status;
        }
        PrebuildStatus current = status;
        if (current != null) {
            return current;
        }
        Manifest manifest = store.current();
        if (manifest == null || manifest.regions() == null) {
            return PrebuildStatus.idle();
        }
        int count = manifest.tileCount();
        List<String> regions = manifest.regions();
        List<String> layers = manifest.layers() == null ? List.of() : manifest.layers();
        return PrebuildStatus.success(regions, manifest.maxZoom(), layers, count,
                "已有预生成瓦片 " + count + " 块，缩放 0–" + manifest.maxZoom());
    }

    private void run(int maxZoom) {
        try {
            List<String> regions = publishedRegions();
            if (regions.isEmpty()) {
                status = PrebuildStatus.failed("还没有已发布的省份，无法预生成全国瓦片");
                return;
            }
            List<String> layers = includedLayers(maxZoom);
            List<TileJob> jobs = plan(layers, maxZoom);
            store.beginRebuild();
            int total = Math.max(jobs.size(), 1);
            int written = 0;
            for (int i = 0; i < jobs.size(); i++) {
                TileJob job = jobs.get(i);
                byte[] body = decode(tiles.mvt(regions, job.layer(), job.tile().z(), job.tile().x(), job.tile().y()));
                if (body.length > 0) {
                    store.write(job.layer(), job.tile().z(), job.tile().x(), job.tile().y(), body);
                    written++;
                }
                if (i == jobs.size() - 1 || i % 20 == 0) {
                    int progress = Math.min(99, (i + 1) * 100 / total);
                    status = PrebuildStatus.running(progress,
                            "正在生成 " + job.layer() + " " + job.tile().z() + "/" + job.tile().x() + "/" + job.tile().y());
                }
            }
            store.save(new Manifest(regions, maxZoom, layers, written));
            status = PrebuildStatus.success(regions, maxZoom, layers, written, "已写入 " + written + " 块低缩放瓦片");
            log.info("全国瓦片预生成完成，省份 {}，写入 {} 块，最大缩放 {}", regions, written, maxZoom);
        } catch (RuntimeException | IOException ex) {
            log.warn("全国瓦片预生成失败: {}", ex.getMessage());
            status = PrebuildStatus.failed(ex.getMessage() == null ? "预生成失败" : ex.getMessage());
        } finally {
            running.set(false);
        }
    }

    public static int defaultMaxZoom() {
        return DEFAULT_MAX_ZOOM;
    }

    private List<String> publishedRegions() {
        List<String> rows = tiles.publishedRegions();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    static List<String> includedLayers(int maxZoom) {
        List<String> layers = new ArrayList<>();
        for (String layer : CANDIDATE_LAYERS) {
            if (minZoom(layer) <= maxZoom) {
                layers.add(layer);
            }
        }
        return layers;
    }

    static int minZoom(String layer) {
        return switch (layer) {
            case "railway" -> 8;
            case "waterway" -> 7;
            case "water" -> 6;
            case "boundary" -> 4;
            default -> 0;
        };
    }

    private static List<TileJob> plan(List<String> layers, int maxZoom) {
        List<Tile> cover = Slippy.coverChina(maxZoom);
        List<TileJob> jobs = new ArrayList<>();
        for (String layer : layers) {
            int from = minZoom(layer);
            for (Tile tile : cover) {
                if (tile.z() >= from) {
                    jobs.add(new TileJob(layer, tile));
                }
            }
        }
        return jobs;
    }

    private static byte[] decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new byte[0];
        }
        return Base64.getMimeDecoder().decode(encoded);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private record TileJob(String layer, Tile tile) {
    }
}
