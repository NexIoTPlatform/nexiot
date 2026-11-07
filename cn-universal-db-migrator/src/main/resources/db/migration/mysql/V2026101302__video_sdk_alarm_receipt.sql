-- Durable ISUP alarm ingestion.
CREATE TABLE video_sdk_alarm_receipt (
  id varchar(64) NOT NULL,
  tenant_id varchar(100) NOT NULL,
  instance_key varchar(100) NOT NULL,
  device_id varchar(100) NOT NULL,
  channel_id varchar(100) NOT NULL DEFAULT '',
  event_id varchar(200) NOT NULL,
  event_kind varchar(100) NOT NULL,
  image_fingerprint varchar(64) NOT NULL DEFAULT '',
  status varchar(16) NOT NULL DEFAULT 'pending',
  payload_json text NOT NULL,
  result_json text,
  log_persisted integer NOT NULL DEFAULT 0,
  flow_dispatched integer NOT NULL DEFAULT 0,
  attempts integer NOT NULL DEFAULT 0,
  created_at bigint NOT NULL,
  updated_at bigint NOT NULL,
  next_attempt_at bigint NOT NULL DEFAULT 0,
  lease_until bigint NOT NULL DEFAULT 0,
  lease_owner varchar(64) NOT NULL DEFAULT ''
);
ALTER TABLE video_sdk_alarm_receipt ADD CONSTRAINT video_sdk_alarm_receipt_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX uk_video_alarm_receipt_identity ON video_sdk_alarm_receipt (tenant_id,instance_key,device_id,channel_id,event_id,event_kind,image_fingerprint);
CREATE INDEX idx_video_alarm_receipt_due ON video_sdk_alarm_receipt (status,next_attempt_at,lease_until);
CREATE INDEX idx_video_alarm_receipt_cooldown ON video_sdk_alarm_receipt (tenant_id,instance_key,device_id,channel_id,event_kind,created_at);
