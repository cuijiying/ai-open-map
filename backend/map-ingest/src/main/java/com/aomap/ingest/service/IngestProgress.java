package com.aomap.ingest.service;

@FunctionalInterface
public interface IngestProgress {
    void report(String phase, int progress, String message);
}
