package com.aomap.ingest.model;

public record SourceProbe(String id, String name, String url, boolean ok, long elapsedMs, String message) {
}
