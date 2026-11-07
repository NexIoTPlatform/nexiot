-- Persistent motion processes are a projection of durable raw callback receipts.
-- A guard row in the process table serializes each tenant/instance/device/channel
-- scope; it is never returned as a process. Only genuine motion callbacks become
-- members. Evidence supplements link to an existing process without adding count.
CREATE TABLE video_sdk_alarm_process (
  id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(100) NOT NULL,
  instance_key VARCHAR(100) NOT NULL,
  device_id VARCHAR(100) NOT NULL,
  channel_id VARCHAR(100) NOT NULL,
  event_kind VARCHAR(16) NOT NULL DEFAULT 'motion',
  row_kind VARCHAR(16) NOT NULL,
  state VARCHAR(16) NOT NULL,
  current_process_id VARCHAR(64),
  first_time BIGINT NOT NULL DEFAULT 0,
  last_time BIGINT NOT NULL DEFAULT 0,
  first_received_at BIGINT NOT NULL DEFAULT 0,
  last_received_at BIGINT NOT NULL DEFAULT 0,
  event_count BIGINT NOT NULL DEFAULT 0,
  first_event_id VARCHAR(200) NOT NULL DEFAULT '',
  last_event_id VARCHAR(200) NOT NULL DEFAULT '',
  start_observed INTEGER NOT NULL DEFAULT 0,
  stop_observed INTEGER NOT NULL DEFAULT 0,
  closed_at BIGINT NOT NULL DEFAULT 0,
  close_reason VARCHAR(32) NOT NULL DEFAULT '',
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  CONSTRAINT video_sdk_alarm_process_pkey PRIMARY KEY (id),
  CONSTRAINT uk_video_alarm_process_scope UNIQUE (id,tenant_id,instance_key,device_id,channel_id),
  CONSTRAINT ck_video_alarm_process_kind CHECK (event_kind='motion' AND row_kind IN ('guard','process')),
  CONSTRAINT ck_video_alarm_process_state CHECK ((row_kind='guard' AND state='guard') OR (row_kind='process' AND state IN ('active','unknown','ended','unknown_gap'))),
  CONSTRAINT ck_video_alarm_process_count CHECK (event_count>=0 AND first_time>=0 AND last_time>=first_time),
  CONSTRAINT ck_video_alarm_process_flags CHECK (start_observed IN (0,1) AND stop_observed IN (0,1))
);
CREATE INDEX idx_video_alarm_process_query ON video_sdk_alarm_process (tenant_id,instance_key,device_id,channel_id,row_kind,last_time);
CREATE INDEX idx_video_alarm_process_gap ON video_sdk_alarm_process (row_kind,state,last_received_at);

CREATE TABLE video_sdk_alarm_process_member (
  receipt_id VARCHAR(64) NOT NULL,
  process_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(100) NOT NULL,
  instance_key VARCHAR(100) NOT NULL,
  device_id VARCHAR(100) NOT NULL,
  channel_id VARCHAR(100) NOT NULL,
  event_id VARCHAR(200) NOT NULL,
  source_event_id VARCHAR(256),
  evidence_id VARCHAR(64),
  alarm_state VARCHAR(16) NOT NULL,
  alarm_time BIGINT NOT NULL,
  received_at BIGINT NOT NULL,
  active_post_count BIGINT,
  CONSTRAINT video_alarm_process_member_pkey PRIMARY KEY (receipt_id),
  CONSTRAINT fk_video_alarm_member_scope FOREIGN KEY (process_id,tenant_id,instance_key,device_id,channel_id)
    REFERENCES video_sdk_alarm_process (id,tenant_id,instance_key,device_id,channel_id),
  CONSTRAINT ck_video_alarm_member_state CHECK (alarm_state IN ('start','stop','unknown')),
  CONSTRAINT ck_video_alarm_member_count CHECK (active_post_count IS NULL OR active_post_count>=0)
);
CREATE INDEX idx_video_alarm_member_page ON video_sdk_alarm_process_member (process_id,alarm_time,receipt_id);
CREATE INDEX idx_video_alarm_member_event ON video_sdk_alarm_process_member (tenant_id,instance_key,device_id,channel_id,event_id);
