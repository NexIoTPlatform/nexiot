ALTER TABLE notice_wechat_openid ADD COLUMN app_id VARCHAR(128) COLLATE "C";
CREATE UNIQUE INDEX uk_notice_wechat_app_openid ON notice_wechat_openid (app_id, openid);
CREATE UNIQUE INDEX uk_notice_wechat_app_user ON notice_wechat_openid (app_id, user_key);
