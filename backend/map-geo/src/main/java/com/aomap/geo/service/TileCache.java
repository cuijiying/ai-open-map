package com.aomap.geo.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拖动和缩放会反复请求同一块瓦片。短时缓存挡住这些重复查询，容量有上限。
 */
public final class TileCache {

    private final int maxEntries;
    private final long ttlMillis;
    private final Map<String, Entry> entries;

    public TileCache(int maxEntries, long ttlMillis) {
        if (maxEntries < 1 || ttlMillis < 1) {
            throw new IllegalArgumentException("瓦片缓存容量和存活时间必须大于 0");
        }
        this.maxEntries = maxEntries;
        this.ttlMillis = ttlMillis;
        this.entries = new LinkedHashMap<>(64, 0.75f, true);
    }

    public synchronized byte[] get(String key, long nowMillis) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (nowMillis >= entry.expiresAt) {
            entries.remove(key);
            return null;
        }
        return entry.body;
    }

    public synchronized void put(String key, byte[] body, long nowMillis) {
        if (key == null || body == null) {
            return;
        }
        entries.put(key, new Entry(body, nowMillis + ttlMillis));
        while (entries.size() > maxEntries) {
            String eldest = entries.keySet().iterator().next();
            entries.remove(eldest);
        }
    }

    public synchronized int size() {
        return entries.size();
    }

    private record Entry(byte[] body, long expiresAt) {
    }
}
