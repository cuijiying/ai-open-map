package com.aomap.ingest.mapper;

import com.aomap.ingest.domain.IngestJobRecord;
import com.aomap.ingest.domain.JobInsert;
import com.aomap.ingest.domain.JobLogRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IngestJobMapper {

    int failStaleRunning();

    int insertRunning(JobInsert job);

    int updateProgress(@Param("id") long id, @Param("phase") String phase,
                       @Param("progress") int progress, @Param("message") String message);

    int updateFile(@Param("id") long id, @Param("path") String path, @Param("bytes") long bytes);

    int succeed(@Param("id") long id, @Param("message") String message,
                @Param("featureCount") long featureCount, @Param("layerCounts") String layerCounts);

    int fail(@Param("id") long id, @Param("message") String message);

    int insertLog(@Param("jobId") long jobId, @Param("phase") String phase,
                  @Param("level") String level, @Param("message") String message);

    IngestJobRecord findById(@Param("id") long id);

    IngestJobRecord findLatest();

    List<IngestJobRecord> list(@Param("limit") int limit);

    List<JobLogRecord> logs(@Param("jobId") long jobId, @Param("afterId") long afterId);
}
