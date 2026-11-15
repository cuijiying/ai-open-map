package com.aomap.geo.service;

import com.aomap.geo.mapper.TileMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 切片语句单独走一个只读事务。
 * 城市一级关闭位图扫描和并行工人：位图扫描会先扫整层道路，并行工人启动就要大约 100 毫秒。
 * 省域瓦片仍用默认计划，并行裁切更快。
 */
@Component
public class TileQuery {

    private final JdbcTemplate jdbc;
    private final TileMapper tiles;

    public TileQuery(JdbcTemplate jdbc, TileMapper tiles) {
        this.jdbc = jdbc;
        this.tiles = tiles;
    }

    @Transactional(readOnly = true)
    public String mvt(List<String> regions, String layer, int z, int x, int y) {
        if (z >= 12) {
            jdbc.execute("SET LOCAL enable_bitmapscan = off");
            jdbc.execute("SET LOCAL max_parallel_workers_per_gather = 0");
            return tiles.mvtCity(regions, layer, z, x, y);
        }
        return tiles.mvt(regions, layer, z, x, y);
    }
}
