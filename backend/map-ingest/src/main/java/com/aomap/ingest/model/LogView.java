package com.aomap.ingest.model;

public record LogView(long id, String phase, String level, String message, String createdAt) {
}
