-- Reviewed V2026110301: no tenant credentials or identity bindings are seeded.
CREATE TABLE IF NOT EXISTS iot_wechat_login_config (
  tenant_id VARCHAR(100) COLLATE "C" NOT NULL,
  account_name VARCHAR(100),
  app_id VARCHAR(128) COLLATE "C",
  app_secret TEXT,
  callback_token TEXT,
  encoding_aes_key TEXT,
  account_qr_url VARCHAR(2048),
  capability_mode VARCHAR(20) NOT NULL DEFAULT 'MESSAGE_CODE',
  login_mode VARCHAR(20) NOT NULL DEFAULT 'BOUND_USER',
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  binding_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  demo_user_union_id VARCHAR(100) COLLATE "C",
  expire_seconds INTEGER NOT NULL DEFAULT 180,
  config_version BIGINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT iot_wechat_login_config_pkey PRIMARY KEY (tenant_id),
  CONSTRAINT ck_wechat_login_capability CHECK (capability_mode IN ('AUTO','SCENE_QR','MESSAGE_CODE')),
  CONSTRAINT ck_wechat_login_mode CHECK (login_mode IN ('BOUND_USER','DEMO_ACCOUNT')),
  CONSTRAINT ck_wechat_login_expiry CHECK (expire_seconds BETWEEN 60 AND 600),
  CONSTRAINT ck_wechat_login_version CHECK (config_version >= 1)
);

CREATE TABLE IF NOT EXISTS iot_user_wechat_binding (
  id VARCHAR(32) COLLATE "C" NOT NULL,
  tenant_id VARCHAR(100) COLLATE "C" NOT NULL,
  app_id VARCHAR(128) COLLATE "C" NOT NULL,
  openid VARCHAR(128) COLLATE "C" NOT NULL,
  user_union_id VARCHAR(100) COLLATE "C" NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT iot_user_wechat_binding_pkey PRIMARY KEY (id),
  CONSTRAINT uk_wechat_binding_identity UNIQUE (tenant_id,app_id,openid),
  CONSTRAINT uk_wechat_binding_account UNIQUE (tenant_id,app_id,user_union_id)
);
CREATE INDEX IF NOT EXISTS idx_wechat_binding_unsubscribe ON iot_user_wechat_binding (app_id,openid);
