-- Runtime startup errors can contain a complete traceback or HTTP response body.
-- PostgreSQL widens varchar to text without rewriting this unindexed column.
-- Bound the metadata lock wait so a busy database stops the rollout promptly.
SET LOCAL lock_timeout = '10s';
ALTER TABLE IF EXISTS video_ai_algorithm_task
    ALTER COLUMN error_message TYPE TEXT;
