package com.aomap.ingest.mapper;

import com.aomap.ingest.domain.LayerCountRow;
import com.aomap.ingest.domain.RegionCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeatureMapper {

    int truncateStage();

    long countStage();

    int disableStatementTimeout();

    int deleteRegion(@Param("regionCode") String regionCode);

    int insertFromStage(@Param("jobId") long jobId, @Param("regionCode") String regionCode);

    int analyzeFeatures();

    long countFeatures(@Param("regionCode") String regionCode);

    List<LayerCountRow> countByLayer(@Param("regionCode") String regionCode);

    List<RegionCountRow> countByRegion();
}
