-- Metadata only: binary media stays at the SDK gateway / configured object storage.
CREATE TABLE IF NOT EXISTS video_sdk_record_task (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  instance_key VARCHAR(200) NOT NULL,
  device_id VARCHAR(200) NOT NULL,
  channel_id VARCHAR(200) NOT NULL,
  task_name VARCHAR(200),
  mode VARCHAR(64) NOT NULL,
  start_time BIGINT,
  end_time BIGINT,
  status VARCHAR(64) NOT NULL,
  progress INTEGER,
  message VARCHAR(1200),
  gateway_job_id VARCHAR(64),
  result_json TEXT,
  created_by VARCHAR(64),
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  next_poll_at BIGINT NOT NULL,
  lease_until BIGINT NOT NULL,
  lease_owner VARCHAR(64),
  cancel_requested INTEGER NOT NULL
);
CREATE INDEX idx_sdk_record_task_scope ON video_sdk_record_task (tenant_id, instance_key, created_at);
CREATE INDEX idx_sdk_record_task_due ON video_sdk_record_task (next_poll_at, lease_until, status);
