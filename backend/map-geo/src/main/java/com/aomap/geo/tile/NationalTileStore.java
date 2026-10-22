package com.aomap.geo.tile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 全国低缩放瓦片落在 data/tiles/national。清单里的省份集合必须和请求一致才允许直接读文件。
 */
@Component
public class NationalTileStore {

    public record Manifest(
            @JsonProperty("regions") List<String> regions,
            @JsonProperty("maxZoom") int maxZoom,
            @JsonProperty("layers") List<String> layers,
            @JsonProperty("tileCount") int tileCount
    ) {
    }

    private final Path root;
    private final ObjectMapper json = new ObjectMapper();
    private volatile Manifest manifest;
    private volatile long manifestStamp = Long.MIN_VALUE;

    @Autowired
    public NationalTileStore(@Value("${app.tiles.dir:data/tiles}") String dir) {
        this(Path.of(dir));
    }

    NationalTileStore(Path root) {
        this.root = root;
    }

    public Path nationalRoot() {
        return root.resolve("national");
    }

    public void beginRebuild() throws IOException {
        manifest = null;
        manifestStamp = Long.MIN_VALUE;
        Path dir = nationalRoot();
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(dir);
    }

    public void write(String layer, int z, int x, int y, byte[] body) throws IOException {
        if (body == null || body.length == 0) {
            return;
        }
        Path target = file(layer, z, x, y);
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, body);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public byte[] read(String layer, int z, int x, int y) {
        Path target = file(layer, z, x, y);
        if (!Files.isRegularFile(target)) {
            return null;
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            return null;
        }
    }

    public void save(Manifest value) throws IOException {
        Path dir = nationalRoot();
        Files.createDirectories(dir);
        Path target = manifestFile();
        Path temporary = target.resolveSibling("manifest.json.tmp");
        json.writeValue(temporary.toFile(), value);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        manifest = value;
        manifestStamp = Files.getLastModifiedTime(target).toMillis();
    }

    public Manifest current() {
        Path file = manifestFile();
        if (!Files.isRegularFile(file)) {
            manifest = null;
            manifestStamp = Long.MIN_VALUE;
            return null;
        }
        try {
            long stamp = Files.getLastModifiedTime(file).toMillis();
            Manifest cached = manifest;
            if (cached != null && stamp == manifestStamp) {
                return cached;
            }
            Manifest loaded = json.readValue(file.toFile(), Manifest.class);
            manifest = loaded;
            manifestStamp = stamp;
            return loaded;
        } catch (IOException ex) {
            return null;
        }
    }

    public boolean matches(List<String> codes, String layer, int z) {
        Manifest current = current();
        if (current == null || current.regions() == null || current.layers() == null) {
            return false;
        }
        if (z < 0 || z > current.maxZoom() || !current.layers().contains(layer)) {
            return false;
        }
        List<String> requested = codes.stream().sorted().distinct().toList();
        List<String> built = current.regions().stream().sorted().distinct().toList();
        return requested.equals(built);
    }

    private Path file(String layer, int z, int x, int y) {
        return nationalRoot().resolve(layer).resolve(Integer.toString(z)).resolve(Integer.toString(x)).resolve(y + ".pbf");
    }

    private Path manifestFile() {
        return nationalRoot().resolve("manifest.json");
    }
}
