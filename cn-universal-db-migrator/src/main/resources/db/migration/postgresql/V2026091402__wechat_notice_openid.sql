CREATE TABLE IF NOT EXISTS notice_wechat_openid (
  id BIGSERIAL PRIMARY KEY,
  channel_id BIGINT NOT NULL,
  user_key VARCHAR(128) NOT NULL,
  openid VARCHAR(128) NOT NULL,
  unionid VARCHAR(128),
  status VARCHAR(2) DEFAULT '1',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_notice_wechat_openid_channel_user UNIQUE (channel_id, user_key)
);
