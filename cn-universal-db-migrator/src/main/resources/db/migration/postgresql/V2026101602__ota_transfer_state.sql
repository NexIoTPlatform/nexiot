-- Keep existing whole-package tasks compatible; transfer state is initialized on first dispatch.
ALTER TABLE iot_ota_task_device ADD COLUMN IF NOT EXISTS transfer_state TEXT;
COMMENT ON COLUMN iot_ota_task_device.transfer_state IS 'OTA传输会话状态（JSON），用于分包确认、重试和断点续传。';
