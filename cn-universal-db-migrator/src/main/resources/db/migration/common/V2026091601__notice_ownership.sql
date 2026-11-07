-- Expand only. Run backfill-notice before deploying the updated notice module.
ALTER TABLE notice_channel ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE notice_channel ADD COLUMN dept_id BIGINT;
ALTER TABLE notice_template ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE notice_template ADD COLUMN dept_id BIGINT;
ALTER TABLE notice_send_record ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE notice_send_record ADD COLUMN dept_id BIGINT;
ALTER TABLE notice_wechat_openid ADD COLUMN tenant_id VARCHAR(64);
ALTER TABLE notice_wechat_openid ADD COLUMN dept_id BIGINT;
ALTER TABLE notice_send_record ADD COLUMN channel_type VARCHAR(64);
INSERT INTO nexiot_db_backfill_task (task_key, task_version, status, cursor_value, batch_size, throttle_ms, updated_at) VALUES ('notice_scope_notice_channel', '20260916', 'PENDING', '0', 1000, 100, CURRENT_TIMESTAMP);
INSERT INTO nexiot_db_backfill_task (task_key, task_version, status, cursor_value, batch_size, throttle_ms, updated_at) VALUES ('notice_scope_notice_template', '20260916', 'PENDING', '0', 1000, 100, CURRENT_TIMESTAMP);
INSERT INTO nexiot_db_backfill_task (task_key, task_version, status, cursor_value, batch_size, throttle_ms, updated_at) VALUES ('notice_scope_notice_send_record', '20260916', 'PENDING', '0', 1000, 100, CURRENT_TIMESTAMP);
INSERT INTO nexiot_db_backfill_task (task_key, task_version, status, cursor_value, batch_size, throttle_ms, updated_at) VALUES ('notice_scope_notice_wechat_openid', '20260916', 'PENDING', '0', 1000, 100, CURRENT_TIMESTAMP);
