package com.aomap.geo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TileMapper {

    String mvt(@Param("region") String region, @Param("layer") String layer, @Param("z") int z, @Param("x") int x, @Param("y") int y);
}
