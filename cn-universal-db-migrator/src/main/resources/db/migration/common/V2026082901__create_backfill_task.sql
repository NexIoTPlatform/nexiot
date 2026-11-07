-- Shared control table for throttled, resumable large-table backfills.
-- This is metadata only; data movement must be performed by an idempotent worker.
CREATE TABLE IF NOT EXISTS nexiot_db_backfill_task (
  task_key VARCHAR(128) NOT NULL,
  task_version VARCHAR(32) NOT NULL,
  status VARCHAR(24) NOT NULL,
  cursor_value VARCHAR(256),
  processed_rows BIGINT NOT NULL DEFAULT 0,
  batch_size INTEGER NOT NULL DEFAULT 1000,
  throttle_ms INTEGER NOT NULL DEFAULT 100,
  last_error VARCHAR(2000),
  started_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (task_key)
);
