-- Keep uk_device_channel (device_id, channel_id); instance-wide uniqueness merges different devices.
-- Conditional DDL also accepts new installations whose bootstrap already uses the non-unique index.
SET @video_channel_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'video_platform_channel'
             AND index_name = 'uk_instance_channel'),
    'ALTER TABLE video_platform_channel DROP INDEX uk_instance_channel, ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT 1');
PREPARE video_channel_statement FROM @video_channel_ddl;
EXECUTE video_channel_statement;
DEALLOCATE PREPARE video_channel_statement;

SET @video_channel_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'video_platform_channel'
             AND index_name = 'idx_instance_channel'),
    'SELECT 1',
    'ALTER TABLE video_platform_channel ADD INDEX idx_instance_channel (instance_key, channel_id, parent_id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE video_channel_statement FROM @video_channel_ddl;
EXECUTE video_channel_statement;
DEALLOCATE PREPARE video_channel_statement;
