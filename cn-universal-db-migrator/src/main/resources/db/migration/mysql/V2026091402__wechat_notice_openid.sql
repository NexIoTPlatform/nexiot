CREATE TABLE IF NOT EXISTS notice_wechat_openid (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  channel_id BIGINT NOT NULL,
  user_key VARCHAR(128) NOT NULL,
  openid VARCHAR(128) NOT NULL,
  unionid VARCHAR(128),
  status VARCHAR(2) DEFAULT '1',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_notice_wechat_openid_channel_user (channel_id, user_key)
);
