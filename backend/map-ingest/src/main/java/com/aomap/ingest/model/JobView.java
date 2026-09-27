package com.aomap.ingest.model;

import java.util.Map;

public record JobView(
        long id,
        String regionCode,
        String regionName,
        String status,
        String phase,
        int progress,
        String message,
        String sourceUrl,
        Long fileBytes,
        Long featureCount,
        Map<String, Long> layerCounts,
        String triggerType,
        String startedAt,
        String finishedAt,
        String error
) {
}
