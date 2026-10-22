package com.aomap.ingest.mapper;

import com.aomap.ingest.domain.LayerCountRow;
import com.aomap.ingest.domain.RegionCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeatureMapper {

    int deleteStage(@Param("jobId") long jobId);

    long countStage(@Param("jobId") long jobId);

    long countFeatures(@Param("regionCode") String regionCode);

    List<LayerCountRow> countByLayer(@Param("regionCode") String regionCode);

    List<RegionCountRow> countByRegion();
}
