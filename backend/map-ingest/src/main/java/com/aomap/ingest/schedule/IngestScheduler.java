package com.aomap.ingest.schedule;

import com.aomap.ingest.config.OsmIngestProperties;
import com.aomap.ingest.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestScheduler.class);

    private final OsmIngestProperties properties;
    private final IngestService ingestService;

    public IngestScheduler(OsmIngestProperties properties, IngestService ingestService) {
        this.properties = properties;
        this.ingestService = ingestService;
    }

    @Scheduled(cron = "${osm.ingest.cron:0 0 3 * * ?}")
    public void nightly() {
        if (!properties.isScheduleEnabled() || properties.getRegions().isEmpty()) {
            return;
        }
        String code = properties.getRegions().get(0).getCode();
        try {
            ingestService.start(code, "SCHEDULE");
            log.info("已触发定时入库: {}", code);
        } catch (IllegalStateException ex) {
            log.info("跳过定时入库: {}", ex.getMessage());
        }
    }
}
