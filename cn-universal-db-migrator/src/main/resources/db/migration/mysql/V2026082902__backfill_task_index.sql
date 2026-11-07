CREATE INDEX idx_nexiot_backfill_status_updated
  ON nexiot_db_backfill_task (status, updated_at);
