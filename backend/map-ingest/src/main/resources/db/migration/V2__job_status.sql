-- 入库任务增加排队和取消。同一省份同时只能有一个未结束任务。
ALTER TABLE ingest_job DROP CONSTRAINT IF EXISTS ck_ingest_job_status;
ALTER TABLE ingest_job
    ADD CONSTRAINT ck_ingest_job_status
    CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED'));

DROP INDEX IF EXISTS uq_ingest_job_running_region;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ingest_job_active_region
    ON ingest_job (region_code)
    WHERE status IN ('QUEUED', 'RUNNING');
