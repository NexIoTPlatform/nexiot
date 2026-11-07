-- Persistent OTA transfer sessions; existing whole-package task data remains intact.
SET @ota_transfer_ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE iot_ota_task_device ADD COLUMN transfer_state LONGTEXT COMMENT ''OTA传输会话状态（JSON），用于分包确认、重试和断点续传。''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'iot_ota_task_device' AND column_name = 'transfer_state'
);
PREPARE ota_transfer_statement FROM @ota_transfer_ddl;
EXECUTE ota_transfer_statement;
DEALLOCATE PREPARE ota_transfer_statement;
