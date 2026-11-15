CREATE TABLE IF NOT EXISTS map_project (
    id            BIGSERIAL PRIMARY KEY,
    org_id        BIGINT       NOT NULL,
    owner_user_id BIGINT       NOT NULL,
    name          VARCHAR(128) NOT NULL,
    state         JSONB        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE map_project IS '组织内的地图项目。state 保存省份、视野、图层和风格';

CREATE INDEX IF NOT EXISTS idx_map_project_org
    ON map_project (org_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS map_share (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT       NOT NULL REFERENCES map_project (id) ON DELETE CASCADE,
    token         VARCHAR(64)  NOT NULL,
    permission    VARCHAR(16)  NOT NULL DEFAULT 'VIEW',
    password_hash VARCHAR(100),
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_map_share_token UNIQUE (token),
    CONSTRAINT ck_map_share_permission CHECK (permission = 'VIEW')
);

COMMENT ON TABLE map_share IS '项目只读分享。token 即访问能力，可设密码和过期时间';

CREATE INDEX IF NOT EXISTS idx_map_share_project
    ON map_share (project_id);
