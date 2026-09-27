-- AI Open Map 0.1
-- 库：ai_open_map    坐标系：WGS84 (EPSG:4326)
-- 表归属：ingest_job* 与 osm_feature* 由入库服务写入，地图服务只读；
--         ai_model_config 由对话服务读写。

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- 入库任务
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ingest_job (
    id            BIGSERIAL PRIMARY KEY,
    region_code   VARCHAR(32)  NOT NULL,
    region_name   VARCHAR(64)  NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    phase         VARCHAR(16)  NOT NULL,
    progress      INTEGER      NOT NULL DEFAULT 0,
    message       TEXT,
    source_url    TEXT,
    file_path     TEXT,
    file_bytes    BIGINT,
    feature_count BIGINT       DEFAULT 0,
    layer_counts  JSONB,
    trigger_type  VARCHAR(16)  NOT NULL,
    error         TEXT,
    started_at    TIMESTAMPTZ,
    finished_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_ingest_job_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    CONSTRAINT ck_ingest_job_progress CHECK (progress >= 0 AND progress <= 100)
);

COMMENT ON TABLE ingest_job IS 'OSM 下载、解析、入库任务。同一时刻只允许一条 RUNNING 记录';
COMMENT ON COLUMN ingest_job.phase IS 'DOWNLOAD 下载 / PARSE 解析 / IMPORT 入库 / INDEX 建索引 / SUCCESS / FAILED';
COMMENT ON COLUMN ingest_job.trigger_type IS 'MANUAL 页面触发 / SCHEDULE 定时任务';
COMMENT ON COLUMN ingest_job.layer_counts IS '各图层要素数量，例如 {"highway":1200,"poi":80}';

-- 部分唯一索引：status 同为 RUNNING 时只允许一行
CREATE UNIQUE INDEX IF NOT EXISTS uq_ingest_job_running
    ON ingest_job (status)
    WHERE status = 'RUNNING';

CREATE INDEX IF NOT EXISTS idx_ingest_job_created
    ON ingest_job (created_at DESC);

CREATE TABLE IF NOT EXISTS ingest_job_log (
    id         BIGSERIAL PRIMARY KEY,
    job_id     BIGINT       NOT NULL REFERENCES ingest_job (id) ON DELETE CASCADE,
    phase      VARCHAR(16),
    level      VARCHAR(16)  NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE ingest_job_log IS '任务阶段日志，供页面回放下载、解析、入库过程';

CREATE INDEX IF NOT EXISTS idx_ingest_job_log_job
    ON ingest_job_log (job_id, id);

-- ---------------------------------------------------------------------------
-- OSM 要素
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS osm_feature (
    id        BIGSERIAL PRIMARY KEY,
    osm_id    BIGINT       NOT NULL,
    osm_type  VARCHAR(8)   NOT NULL,
    layer     VARCHAR(32)  NOT NULL,
    sub_type  VARCHAR(64),
    name      TEXT,
    props     JSONB,
    geom      geometry(Geometry, 4326) NOT NULL,
    job_id    BIGINT,
    CONSTRAINT ck_osm_feature_type CHECK (osm_type IN ('node', 'way')),
    CONSTRAINT ck_osm_feature_layer CHECK (layer IN (
        'highway', 'railway', 'waterway', 'water',
        'building', 'landuse', 'boundary', 'poi', 'place'
    ))
);

COMMENT ON TABLE osm_feature IS '按图层归一化的 OSM 要素。地图服务按 layer 输出同类型矢量切片';
COMMENT ON COLUMN osm_feature.layer IS 'highway 道路 / railway 铁路 / waterway 水系线 / water 水体 / building 建筑 / landuse 土地利用 / boundary 行政区划 / poi 兴趣点 / place 地名';
COMMENT ON COLUMN osm_feature.sub_type IS '图层内分类，例如 highway=primary、amenity=school';
COMMENT ON COLUMN osm_feature.geom IS 'WGS84 几何。道路、水系、铁路、边界为线，建筑、用地、水体为面，地名和兴趣点为点';
COMMENT ON COLUMN osm_feature.props IS '保留的常用 OSM 标签';

CREATE INDEX IF NOT EXISTS idx_osm_feature_geom
    ON osm_feature USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_osm_feature_layer
    ON osm_feature (layer, sub_type);

CREATE INDEX IF NOT EXISTS idx_osm_feature_name
    ON osm_feature USING GIN (name gin_trgm_ops);

-- 入库缓冲表。解析阶段只写这里，成功后再替换正式表，避免半成品被地图读到。
CREATE UNLOGGED TABLE IF NOT EXISTS osm_feature_stage (
    osm_id   BIGINT,
    osm_type VARCHAR(8),
    layer    VARCHAR(32),
    sub_type VARCHAR(64),
    name     TEXT,
    props    TEXT,
    wkt      TEXT
);

COMMENT ON TABLE osm_feature_stage IS '解析缓冲表，无索引。发布时再写入 osm_feature';

-- ---------------------------------------------------------------------------
-- 对话模型
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ai_model_config (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)   NOT NULL,
    base_url    VARCHAR(512)  NOT NULL,
    api_key     VARCHAR(1024),
    model       VARCHAR(128)  NOT NULL,
    temperature NUMERIC(4, 2) NOT NULL DEFAULT 0.20,
    enabled     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

COMMENT ON TABLE ai_model_config IS 'OpenAI 兼容对话模型。api_key 仅用于本地接入，接口返回时脱敏';
COMMENT ON COLUMN ai_model_config.base_url IS '以 /v1 结尾的兼容地址，例如 https://api.openai.com/v1 或 http://127.0.0.1:11434/v1';
COMMENT ON COLUMN ai_model_config.enabled IS '同时只启用一个模型；未启用时对话走内置指令';

CREATE INDEX IF NOT EXISTS idx_ai_model_enabled
    ON ai_model_config (enabled);
