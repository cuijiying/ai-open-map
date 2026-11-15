-- 模型配置增加接口类型。chat 走对话补全，responses 走 Responses。已有配置默认仍是对话补全。
ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS protocol VARCHAR(16) NOT NULL DEFAULT 'chat';
COMMENT ON COLUMN ai_model_config.protocol IS 'chat 为对话补全 /chat/completions，responses 为 Responses /responses';
