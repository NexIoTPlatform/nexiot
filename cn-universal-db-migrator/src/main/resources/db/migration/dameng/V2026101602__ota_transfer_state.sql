-- Dameng standalone incremental script; current DatabaseMigrator does not consume this directory.
-- Run once against the existing installation; never replay the Docker initial schema.
ALTER TABLE iot_ota_task_device ADD transfer_state CLOB;
COMMENT ON COLUMN iot_ota_task_device.transfer_state IS 'OTA传输会话状态（JSON），用于分包确认、重试和断点续传。';
