package com.aomap.ingest.mapper;

import com.aomap.ingest.domain.LayerCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeatureMapper {

    int truncateStage();

    long countStage();

    int disableStatementTimeout();

    int dropGeomIndex();

    int dropLayerIndex();

    int dropNameIndex();

    int truncateFeatures();

    int insertFromStage(@Param("jobId") long jobId);

    int createGeomIndex();

    int createLayerIndex();

    int createNameIndex();

    int analyzeFeatures();

    long countFeatures();

    List<LayerCountRow> countByLayer();
}
