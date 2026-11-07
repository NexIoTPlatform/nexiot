-- Explicit maintenance migration. Pause all writers and take a verified backup first.
-- No extension installation, timestamp repair, key replacement, retention or compression.
DO $nexiot_hypertables$
DECLARE
  target_schema name := current_schema();
  extension_schema name;
  item record;
  time_attribute smallint;
BEGIN
  SELECT n.nspname INTO extension_schema FROM pg_extension e
    JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='timescaledb';
  IF extension_schema IS NULL THEN
    RAISE NOTICE 'TimescaleDB extension is not installed; keeping ordinary PostgreSQL history tables';
    RETURN;
  END IF;
  IF target_schema IS NULL OR target_schema IN ('public','pg_catalog') THEN
    RAISE EXCEPTION 'Set search_path to the NexIoT application schema before converting history tables';
  END IF;
  -- Validate the complete set before converting any table. INCLUDE columns do not count
  -- as uniqueness keys. Existing native PostgreSQL partition parents need a separate plan.
  FOR item IN SELECT c.oid,c.relname,c.relkind FROM pg_class c
      JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=target_schema
      AND c.relkind IN ('r','p') AND c.relname ~ '^ts_iot_device_log(_metadata)?(_[0-9]+)?$'
      ORDER BY c.relname
  LOOP
    IF item.relkind <> 'r' THEN
      RAISE EXCEPTION 'History table %.% is already PostgreSQL partitioned; explicit migration required',target_schema,item.relname;
    END IF;
    SELECT a.attnum INTO time_attribute FROM pg_attribute a WHERE a.attrelid=item.oid
      AND a.attname='create_time' AND NOT a.attisdropped AND a.attnotnull
      AND a.atttypid IN ('timestamp'::regtype,'timestamptz'::regtype);
    IF time_attribute IS NULL THEN
      RAISE EXCEPTION 'History table %.% requires a NOT NULL timestamp create_time',target_schema,item.relname;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_index i WHERE i.indrelid=item.oid AND i.indisunique
        AND NOT time_attribute=ANY((i.indkey::smallint[])[0:i.indnkeyatts-1])) THEN
      RAISE EXCEPTION 'History table %.% has a unique key without create_time; refusing to rewrite it',target_schema,item.relname;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE contype='f'
        AND (conrelid=item.oid OR confrelid=item.oid)) THEN
      RAISE EXCEPTION 'History table %.% has foreign key dependencies; explicit migration required',target_schema,item.relname;
    END IF;
  END LOOP;
  FOR item IN SELECT c.oid,c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname=target_schema AND c.relkind='r'
      AND c.relname ~ '^ts_iot_device_log(_metadata)?(_[0-9]+)?$' ORDER BY c.relname
  LOOP
    IF EXISTS (SELECT 1 FROM timescaledb_information.hypertables
        WHERE hypertable_schema=target_schema AND hypertable_name=item.relname) THEN
      CONTINUE;
    END IF;
    EXECUTE format('SELECT %I.create_hypertable($1, ''create_time'', '
      'chunk_time_interval => INTERVAL ''7 days'', create_default_indexes => FALSE, '
      'migrate_data => TRUE, if_not_exists => TRUE)',extension_schema) USING item.oid::regclass;
  END LOOP;
END $nexiot_hypertables$;
