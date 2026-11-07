#!/bin/sh
# Safe initial bootstrap / exact-version no-op. Never replaces an existing schema.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
sha256sum -c SHA256SUMS >/dev/null
seed_digest=$(sha256sum SHA256SUMS | cut -d ' ' -f 1)
{
cat <<'SQL'
\set ON_ERROR_STOP on
BEGIN;
SELECT pg_advisory_xact_lock(728319410);
CREATE TABLE IF NOT EXISTS public.nexiot_bootstrap_history (
  bootstrap_key text PRIMARY KEY,
  content_sha256 text NOT NULL,
  applied_at timestamptz NOT NULL DEFAULT now()
);
SELECT EXISTS(SELECT 1 FROM public.nexiot_bootstrap_history WHERE bootstrap_key='clean-seed-v1') AS already_applied \gset
\if :already_applied
SQL
printf "SELECT content_sha256 = '%s' AS same_content FROM public.nexiot_bootstrap_history WHERE bootstrap_key='clean-seed-v1' \\gset\n" "$seed_digest"
cat <<'SQL'
  \if :same_content
    \echo 'NexIoT schema already initialized with this exact seed; no data changed.'
  \else
    DO $$ BEGIN RAISE EXCEPTION 'Seed version changed on an initialized database. Use an explicit migration, never replay bootstrap.'; END $$;
  \endif
\else
  DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname='unix_tenant_202605') THEN
      RAISE EXCEPTION 'Existing untracked NexIoT schema detected. Refusing to modify it; select a new empty database.';
    END IF;
  END $$;
SQL
for sql_file in 001-schema.sql 010-dictionaries.sql 020-platform-admin.sql 030-platform-config.sql 040-system-components.sql 090-constraints-indexes.sql 095-reset-sequences.sql 097-history-hypertables.sql 099-verify-clean-seed.sql; do
  printf '\\ir %s\n' "$sql_file"
done
printf "INSERT INTO public.nexiot_bootstrap_history(bootstrap_key,content_sha256) VALUES ('clean-seed-v1','%s');\n" "$seed_digest"
cat <<'SQL'
\endif
COMMIT;
SQL
} | psql -X -q -v ON_ERROR_STOP=1
