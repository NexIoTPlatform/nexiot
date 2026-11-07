-- Derive sequences from retained rows, never from source business counters.
DO $nexiot$
DECLARE s record; maximum bigint;
BEGIN
  FOR s IN
    SELECT seq.oid AS sequence_oid, sn.nspname AS sequence_schema, seq.relname AS sequence_name,
           tn.nspname AS table_schema, t.relname AS table_name, a.attname AS column_name,
           ps.seqstart AS start_value
    FROM pg_class seq JOIN pg_namespace sn ON sn.oid=seq.relnamespace
    JOIN pg_sequence ps ON ps.seqrelid=seq.oid
    JOIN pg_depend d ON d.objid=seq.oid AND d.classid='pg_class'::regclass AND d.deptype IN ('a','i')
    JOIN pg_class t ON t.oid=d.refobjid JOIN pg_namespace tn ON tn.oid=t.relnamespace
    JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=d.refobjsubid
    WHERE seq.relkind='S' AND sn.nspname='unix_tenant_202605'
  LOOP
    EXECUTE format('SELECT max(%I) FROM %I.%I', s.column_name, s.table_schema, s.table_name) INTO maximum;
    PERFORM pg_catalog.setval(s.sequence_oid::regclass, greatest(coalesce(maximum,s.start_value),s.start_value), maximum IS NOT NULL);
  END LOOP;
END $nexiot$;
