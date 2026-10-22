package com.aomap.geo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TileMapper {

    String mvt(@Param("regions") List<String> regions, @Param("layer") String layer, @Param("z") int z, @Param("x") int x, @Param("y") int y);

    List<String> publishedRegions();
}
