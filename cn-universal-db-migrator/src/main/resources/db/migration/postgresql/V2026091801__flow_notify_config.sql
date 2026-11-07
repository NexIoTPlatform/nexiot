-- 规则级连续失败通知配置
ALTER TABLE iot_flow_definition ADD COLUMN IF NOT EXISTS notify_failure_threshold INTEGER NOT NULL DEFAULT 0;
ALTER TABLE iot_flow_definition ADD COLUMN IF NOT EXISTS notify_template_id VARCHAR(64);
