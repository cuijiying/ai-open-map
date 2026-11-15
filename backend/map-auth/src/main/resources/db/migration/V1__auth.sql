CREATE TABLE IF NOT EXISTS auth_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_auth_user_email UNIQUE (email)
);

COMMENT ON TABLE auth_user IS '登录账号。邮箱小写保存';

CREATE TABLE IF NOT EXISTS auth_org (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE auth_org IS '组织。注册时为每个用户创建一个个人组织';

CREATE TABLE IF NOT EXISTS auth_member (
    org_id  BIGINT      NOT NULL REFERENCES auth_org (id),
    user_id BIGINT      NOT NULL REFERENCES auth_user (id),
    role    VARCHAR(16) NOT NULL,
    CONSTRAINT pk_auth_member PRIMARY KEY (org_id, user_id),
    CONSTRAINT ck_auth_member_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

COMMENT ON TABLE auth_member IS '组织成员。OWNER 管理员，EDITOR 可编辑，VIEWER 只读';
