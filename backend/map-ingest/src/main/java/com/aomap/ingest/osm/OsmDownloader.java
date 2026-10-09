package com.aomap.ingest.osm;

import com.aomap.common.geo.Regions.Region;
import com.aomap.ingest.config.OsmIngestProperties;
import com.aomap.ingest.service.IngestProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;

@Component
public class OsmDownloader {

    private static final Logger log = LoggerFactory.getLogger(OsmDownloader.class);

    private final HttpClient httpClient;

    public OsmDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path download(OsmIngestProperties properties, Region region, IngestProgress progress) throws IOException, InterruptedException {
        Path dir = Path.of(properties.getDataDir());
        Files.createDirectories(dir);
        Path target = dir.resolve(region.code() + "-latest.osm.pbf");
        String remoteMd5 = fetchText(properties, region.md5Url());
        String expected = remoteMd5 == null ? null : remoteMd5.split("\\s+")[0].trim().toLowerCase();
        if (expected != null && Files.exists(target) && expected.equals(md5(target))) {
            progress.report("DOWNLOAD", 20, "本地文件校验通过，跳过下载");
            return target;
        }

        Path part = dir.resolve(region.code() + "-latest.osm.pbf.part");
        progress.report("DOWNLOAD", 1, "开始下载 " + region.name() + " OSM 数据");
        HttpRequest request = HttpRequest.newBuilder(URI.create(region.pbfUrl()))
                .header("User-Agent", properties.getUserAgent())
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IllegalStateException("下载失败，HTTP " + response.statusCode());
        }
        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        try (InputStream input = response.body()) {
            Files.copy(new ProgressStream(input, total, progress), part, StandardCopyOption.REPLACE_EXISTING);
        }
        String actual = md5(part);
        if (expected != null && !expected.equals(actual)) {
            Files.deleteIfExists(part);
            throw new IllegalStateException("文件校验失败，MD5 与 Geofabrik 不一致");
        }
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        progress.report("DOWNLOAD", 20, "下载完成，大小 " + Files.size(target) + " 字节");
        log.info("OSM 文件已就绪: {}", target);
        return target;
    }

    private String fetchText(OsmIngestProperties properties, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", properties.getUserAgent())
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return response.body();
        } catch (Exception ex) {
            log.warn("读取 MD5 失败，将直接下载: {}", ex.getMessage());
            return null;
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
