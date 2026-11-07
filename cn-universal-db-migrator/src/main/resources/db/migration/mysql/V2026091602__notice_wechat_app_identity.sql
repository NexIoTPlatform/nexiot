ALTER TABLE notice_wechat_openid ADD COLUMN app_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin;
-- OpenID and business keys are case-sensitive identifiers.
ALTER TABLE notice_wechat_openid MODIFY openid VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, MODIFY user_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
CREATE UNIQUE INDEX uk_notice_wechat_app_openid ON notice_wechat_openid (app_id, openid);
CREATE UNIQUE INDEX uk_notice_wechat_app_user ON notice_wechat_openid (app_id, user_key);
