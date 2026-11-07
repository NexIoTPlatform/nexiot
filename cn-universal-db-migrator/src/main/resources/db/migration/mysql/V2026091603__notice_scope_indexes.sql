CREATE INDEX idx_notice_channel_scope ON notice_channel (tenant_id, dept_id, create_time);
CREATE INDEX idx_notice_template_scope ON notice_template (tenant_id, dept_id, create_time);
CREATE INDEX idx_notice_send_record_scope ON notice_send_record (tenant_id, dept_id, send_time);
