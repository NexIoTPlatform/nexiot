-- Channel IDs are unique within a device (uk_device_channel), not a platform instance.
-- Keep instance-level lookup indexed while allowing two devices to report the same channel ID.
-- Apply before rolling out the device-scoped channel sync, then resync affected platforms.
DROP INDEX IF EXISTS uk_instance_channel;
CREATE INDEX IF NOT EXISTS idx_instance_channel
    ON video_platform_channel (instance_key, channel_id, parent_id);
