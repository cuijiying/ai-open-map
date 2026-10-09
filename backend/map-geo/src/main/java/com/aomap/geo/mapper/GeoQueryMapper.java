package com.aomap.geo.mapper;

import com.aomap.geo.domain.AnalysisRow;
import com.aomap.geo.domain.NameCount;
import com.aomap.geo.domain.SearchRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GeoQueryMapper {

    List<NameCount> countByLayer(@Param("region") String region);

    List<NameCount> stats(@Param("region") String region, @Param("layer") String layer);

    List<SearchRow> search(@Param("region") String region, @Param("pattern") String pattern, @Param("layer") String layer, @Param("limit") int limit);

    AnalysisRow analyze(@Param("region") String region,
                        @Param("layer") String layer,
                        @Param("minLon") double minLon,
                        @Param("minLat") double minLat,
                        @Param("maxLon") double maxLon,
                        @Param("maxLat") double maxLat);
}
