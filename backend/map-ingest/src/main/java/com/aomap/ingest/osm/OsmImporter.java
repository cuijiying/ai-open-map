package com.aomap.ingest.osm;

import com.aomap.ingest.repo.FeatureRepository;
import com.aomap.ingest.repo.FeatureRepository.StagedFeature;
import com.aomap.ingest.service.IngestProgress;
import crosby.binary.osmosis.OsmosisReader;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OsmImporter {

    private static final Logger log = LoggerFactory.getLogger(OsmImporter.class);
    private static final int BATCH = 4000;

    private final FeatureRepository features;

    public OsmImporter(FeatureRepository features) {
        this.features = features;
    }

    public long importFile(Path file, long jobId, boolean includeBuildings, IngestProgress progress) throws IOException {
        features.truncateStage();
        long length = Files.size(file);
        CountingInputStream input = new CountingInputStream(Files.newInputStream(file));
        ParserSink sink = new ParserSink(includeBuildings, length, input, progress);
        try (input) {
            OsmosisReader reader = new OsmosisReader(input);
            reader.setSink(sink);
            reader.run();
        }
        if (sink.failure != null) {
            throw new IllegalStateException("解析 OSM 失败: " + sink.failure.getMessage(), sink.failure);
        }
        long staged = features.countStage();
        log.info("解析完成，缓冲要素 {} 条，跳过 {}", staged, sink.skipped);
        if (staged == 0) {
            throw new IllegalStateException("没有解析到可入库的要素");
        }
        progress.report("IMPORT", 82, "解析完成，共 " + staged + " 条要素，开始写入正式表");
        long stored = features.publish(jobId);
        progress.report("INDEX", 96, "空间索引已建立");
        return stored;
    }

    private final class ParserSink implements Sink {
        private final boolean includeBuildings;
        private final long fileLength;
        private final CountingInputStream input;
        private final IngestProgress progress;
        private final List<StagedFeature> batch = new ArrayList<>(BATCH);
        private Long2LongOpenHashMap nodes = new Long2LongOpenHashMap(4_000_000);
        private long nodeCount;
        private long featureCount;
        private long skipped;
        private int lastPercent = 20;
        private boolean waysStarted;
        private boolean flushed;
        private Exception failure;

        private ParserSink(boolean includeBuildings, long fileLength, CountingInputStream input, IngestProgress progress) {
            this.includeBuildings = includeBuildings;
            this.fileLength = Math.max(fileLength, 1);
            this.input = input;
            this.progress = progress;
            nodes.defaultReturnValue(Long.MIN_VALUE);
        }

        @Override
        public void initialize(Map<String, Object> metaData) {
            progress.report("PARSE", 20, "开始解析 PBF");
        }

        @Override
        public void process(EntityContainer entityContainer) {
            entityContainer.process(new EntityProcessor() {
                @Override
                public void process(org.openstreetmap.osmosis.core.container.v0_6.BoundContainer bound) {
                }

                @Override
                public void process(NodeContainer nodeContainer) {
                    onNode(nodeContainer.getEntity());
                }

                @Override
                public void process(WayContainer wayContainer) {
                    onWay(wayContainer.getEntity());
                }

                @Override
                public void process(RelationContainer relationContainer) {
                }
            });
        }

        @Override
        public void complete() {
            end();
        }

        @Override
        public void close() {
            end();
        }

        private void end() {
            try {
                flush(true);
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = ex;
                }
            }
        }

        private void onNode(Node node) {
            nodes.put(node.getId(), pack(node.getLongitude(), node.getLatitude()));
            nodeCount++;
            Map<String, String> tags = tags(node);
            if (!tags.isEmpty()) {
                OsmTagClassifier.Kind kind = OsmTagClassifier.classifyNode(tags);
                if (kind != null) {
                    accept(node.getId(), "node", kind, OsmTagClassifier.nameOf(tags),
                            OsmTagClassifier.propsJson(tags), point(node.getLongitude(), node.getLatitude()));
                }
            }
            tick("正在读取节点 " + nodeCount);
        }

        private void onWay(Way way) {
            if (!waysStarted) {
                waysStarted = true;
                progress.report("PARSE", Math.max(lastPercent, 35), "节点读取完成，共 " + nodeCount + " 个，开始生成线、面要素");
            }
            Map<String, String> tags = tags(way);
            OsmTagClassifier.Kind kind = tags.isEmpty() ? null : OsmTagClassifier.classifyWay(tags, includeBuildings);
            if (kind != null) {
                List<double[]> coordinates = new ArrayList<>(way.getWayNodes().size());
                boolean complete = true;
                for (WayNode wayNode : way.getWayNodes()) {
                    long packed = nodes.get(wayNode.getNodeId());
                    if (packed == Long.MIN_VALUE) {
                        complete = false;
                        break;
                    }
                    coordinates.add(new double[]{unpackLon(packed), unpackLat(packed)});
                }
                String wkt = null;
                if (complete) {
                    wkt = kind.polygon() ? WktBuilder.polygon(coordinates) : WktBuilder.lineString(coordinates);
                }
                if (wkt == null) {
                    skipped++;
                } else {
                    accept(way.getId(), "way", kind, OsmTagClassifier.nameOf(tags), OsmTagClassifier.propsJson(tags), wkt);
                }
            }
            tick("正在解析，节点 " + nodeCount + "，要素 " + featureCount);
        }

        private void accept(long osmId, String osmType, OsmTagClassifier.Kind kind, String name, String props, String wkt) {
            if (wkt == null) {
                skipped++;
                return;
            }
            batch.add(new StagedFeature(osmId, osmType, kind.layer(), kind.subType(), name == null ? "" : name, props, wkt));
            featureCount++;
            if (batch.size() >= BATCH) {
                flush(false);
            }
        }

        private void tick(String message) {
            int percent = 20 + (int) Math.min(58, input.count * 58 / fileLength);
            if (percent > lastPercent) {
                lastPercent = percent;
                progress.report("PARSE", percent, message);
            }
        }

        private void flush(boolean finished) {
            if (finished) {
                if (flushed) {
                    return;
                }
                flushed = true;
                nodes = null;
            }
            if (batch.isEmpty()) {
                return;
            }
            try {
                features.copy(new ArrayList<>(batch));
                batch.clear();
            } catch (RuntimeException ex) {
                failure = ex;
                throw ex;
            }
        }
    }

    private static String point(double lon, double lat) {
        if (!Double.isFinite(lon) || !Double.isFinite(lat) || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            return null;
        }
        return String.format(java.util.Locale.ROOT, "POINT(%.7f %.7f)", lon, lat);
    }

    private static Map<String, String> tags(Entity entity) {
        Map<String, String> values = new HashMap<>();
        for (Tag tag : entity.getTags()) {
            values.put(tag.getKey(), tag.getValue());
        }
        return values;
    }

    static long pack(double lon, double lat) {
        int lonE7 = (int) Math.round(lon * 1e7);
        int latE7 = (int) Math.round(lat * 1e7);
        return ((long) latE7 << 32) | (lonE7 & 0xFFFFFFFFL);
    }

    static double unpackLon(long packed) {
        return ((int) packed) / 1e7;
    }

    static double unpackLat(long packed) {
        return (int) (packed >> 32) / 1e7;
    }

    private static final class CountingInputStream extends FilterInputStream {
        private long count;

        private CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                count++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            int n = super.read(buffer, off, len);
            if (n > 0) {
                count += n;
            }
            return n;
        }
    }
}
