package com.aomap.ingest.osm;

import com.aomap.common.geo.Regions.Region;
import com.aomap.ingest.config.OsmIngestProperties;
import com.aomap.ingest.model.SourceProbe;
import com.aomap.ingest.service.IngestProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class OsmDownloader {

    private static final Logger log = LoggerFactory.getLogger(OsmDownloader.class);

    private static final Duration MD5_TIMEOUT = Duration.ofSeconds(20);

    private final ReachableClient http;

    public OsmDownloader() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.http = new ReachableClient(httpClient);
    }

    public SourceProbe probe(OsmIngestProperties properties, OsmMirrors.Source source) {
        long started = System.nanoTime();
        try (ReachableClient.Opened response = http.open(source.md5Url(), properties.getUserAgent(), Duration.ofSeconds(12))) {
            long elapsed = elapsedMs(started);
            if (response.status() == 200) {
                return new SourceProbe(source.id(), source.name(), source.pbfUrl(), true, elapsed, "可连接");
            }
            return new SourceProbe(source.id(), source.name(), source.pbfUrl(), false, elapsed, "HTTP " + response.status());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new SourceProbe(source.id(), source.name(), source.pbfUrl(), false, elapsedMs(started), "测试被中断");
        } catch (Exception ex) {
            String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "连接失败" : ex.getMessage();
            return new SourceProbe(source.id(), source.name(), source.pbfUrl(), false, elapsedMs(started), message);
        }
    }

    public Path download(
            OsmIngestProperties properties,
            Region region,
            String sourceId,
            IngestProgress progress,
            Consumer<OsmMirrors.Source> chosen
    ) throws IOException, InterruptedException {
        Path dir = Path.of(properties.getDataDir());
        Files.createDirectories(dir);
        Path target = dir.resolve(region.code() + "-latest.osm.pbf");
        List<String> failures = new ArrayList<>();
        for (OsmMirrors.Source source : OsmMirrors.resolve(region, sourceId)) {
            if (chosen != null) {
                chosen.accept(source);
            }
            try {
                return downloadFrom(properties, region, progress, target, source);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } catch (Exception ex) {
                String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();
                failures.add(source.name() + "：" + reason);
                log.warn("从 {} 下载失败: {}", source.name(), reason);
            }
        }
        throw new IOException("下载失败，已尝试的数据源都不可用。" + String.join("；", failures));
    }

    private static long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    private Path downloadFrom(
            OsmIngestProperties properties,
            Region region,
            IngestProgress progress,
            Path target,
            OsmMirrors.Source source
    ) throws IOException, InterruptedException {
        String expected = fetchMd5(properties, source.md5Url());
        if (expected != null && Files.exists(target) && expected.equals(md5(target))) {
            progress.report("DOWNLOAD", 20, "本地文件校验通过，跳过下载");
            log.info("本地文件与 {} 一致，跳过下载: {}", source.name(), target);
            return target;
        }

        Path part = target.resolveSibling(target.getFileName() + ".part");
        progress.report("DOWNLOAD", 1, "开始从 " + source.name() + " 下载 " + region.name() + " OSM 数据");
        try (ReachableClient.Opened response = http.open(source.pbfUrl(), properties.getUserAgent(), null)) {
            if (response.status() != 200) {
                throw new IOException("HTTP " + response.status());
            }
            Files.copy(new ProgressStream(response.body(), response.length(), progress), part, StandardCopyOption.REPLACE_EXISTING);
            String actual = md5(part);
            if (expected != null && !expected.equals(actual)) {
                throw new IOException("文件校验失败，MD5 不一致");
            }
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            progress.report("DOWNLOAD", 20, "从 " + source.name() + " 下载完成，大小 " + Files.size(target) + " 字节");
            log.info("OSM 文件已就绪: {} ({})", target, source.pbfUrl());
            return target;
        } catch (Exception ex) {
            Files.deleteIfExists(part);
            throw ex;
        }
    }

    private String fetchMd5(OsmIngestProperties properties, String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            return null;
        }
        try (ReachableClient.Opened response = http.open(url, properties.getUserAgent(), MD5_TIMEOUT)) {
            if (response.status() != 200) {
                log.warn("读取 MD5 失败，HTTP {}，将直接下载 {}", response.status(), url);
                return null;
            }
            String body = new String(response.body().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (body.isBlank()) {
                return null;
            }
            return body.split("\\s+")[0].trim().toLowerCase();
        }
    }

    static String md5(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = input.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder hex = new StringBuilder(32);
            for (byte value : digest.digest()) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IOException("计算 MD5 失败", ex);
        }
    }

    private static final class ProgressStream extends InputStream {
        private final InputStream delegate;
        private final long total;
        private final IngestProgress progress;
        private long readBytes;
        private int lastPercent = 1;

        private ProgressStream(InputStream delegate, long total, IngestProgress progress) {
            this.delegate = delegate;
            this.total = total;
            this.progress = progress;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                advance(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            int count = delegate.read(buffer, off, len);
            if (count > 0) {
                advance(count);
            }
            return count;
        }

        private void advance(int count) {
            readBytes += count;
            if (total <= 0) {
                return;
            }
            int percent = 1 + (int) Math.min(18, readBytes * 18 / total);
            if (percent > lastPercent) {
                lastPercent = percent;
                progress.report("DOWNLOAD", percent, "正在下载 " + readBytes + " / " + total + " 字节");
            }
        }
    }
}
