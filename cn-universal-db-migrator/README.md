# NexIoT production database migrator

This is an explicit, one-shot Flyway runner. The Web application does not run migrations during startup.
MySQL and PostgreSQL are supported. Dameng is intentionally excluded from the automatic path for now.

## Required environment

`DB_MIGRATOR_URL`, `DB_MIGRATOR_USERNAME`, and `DB_MIGRATOR_PASSWORD` must be injected by the deployment system.
Credentials must not be placed in Git, application properties, command-line arguments, or image layers.
PostgreSQL `migrate` refuses missing/empty history without executing historical DDL; explicitly baseline the
verified installed schema first. Current Docker startup still does not invoke this runner.

Optional settings:

* `DB_MIGRATOR_HISTORY_TABLE` (default `flyway_schema_history`)
Required for `baseline`: `DB_MIGRATOR_BASELINE_VERSION`, explicitly set to the verified already installed schema version.
There is no implicit baseline version for the baseline command. Current latest community bootstrap is `2026110701`;
older environments must use their actual installed migration checkpoint.

## Commands

```text
java -jar cn-universal-db-migrator.jar validate
DB_MIGRATOR_BASELINE_VERSION=<verified-installed-version> DB_MIGRATOR_CONFIRM_BASELINE=YES java -jar cn-universal-db-migrator.jar baseline
java -jar cn-universal-db-migrator.jar migrate
```

Run `baseline` exactly once for an existing database at the release which introduces this module. Review the
database/schema and take a tested backup first. For a new database, run the normal schema bootstrap first, then
baseline it; this module is not a replacement for the initial installation SQL.

The PostgreSQL Docker bootstrap includes an empty official `flyway_schema_history` table. A confirmed
`baseline` now checks and locks that table, recreates only this empty official metadata shape, then lets Flyway
write the baseline. It never removes nonempty history, custom history tables, extra columns, custom triggers/indexes or explicit table/column ACLs.
Use the default history table consistently after baseline; do not switch table names to bypass applied history.
`DB_MIGRATOR_BASELINE_VERSION` must match the already installed release, not a version containing pending changes.

`migrate` validates checksums and then applies all versions after the baseline. Flyway's database lock prevents
two deployment jobs from running at the same time. The runner uses a separate short-lived connection and does not
consume the Web application's Druid pool.

Docker currently uses clean-seed initialization only. Its initial schema defines the algorithm task's
`error_message` as `TEXT`; the backend image and customer startup do not include or invoke this runner.
Automatic Docker migrations remain disabled until PostgreSQL and MySQL migration acceptance is complete.

## Migration rules

* Update the Docker one-click bootstrap SQL in `docker/database/` first, including checksums, manifest,
  export and verification logic; also add an incremental migration here for existing databases. Verify
  both paths produce the same schema. Never replay bootstrap SQL on an existing database. This rule
  applies to table, column, index and constraint changes; see the repository root `README.md`.
* Never edit an applied migration. Add a new `VyyyyMMddNN__description.sql` file.
* PostgreSQL `V2026100901` widens `video_ai_algorithm_task.error_message` to `TEXT` so runtime tracebacks can be
  saved together with the task's failure status. It bounds metadata lock waits to 10 seconds and preserves rows.
* PostgreSQL `V2026101301` adds industrial point-template drafts/protocol filtering and the template-center menu.
  It preserves legacy CONFIG snapshots, binds POINTS templates to a product/model, carries model/points/groups together,
  and inherits permissions from equivalent existing industrial operations. Historical migrations remain unchanged; the community seed preserves their schema.
* PostgreSQL `V2026101402` completes the same point-template schema and permissions for installations whose history
  has passed `V2026101301` without installing it. It also validates and preserves an already completed installation.
  A forward script does not resolve unrelated ignored historical migrations: review the actual history and release
  resource set and pass `validate` before execution. Do not rebaseline, change old checksums or enable `outOfOrder`.
* `V2026101801` restores the industrial connection-probe button under device management. It inherits only active
  device-edit button grants for roles and packages, preserves a compatible existing probe menu ID, and does not
  grant access solely because a role is an administrator. PostgreSQL/MySQL scripts run through this runner;
  the Dameng script remains a standalone incremental operation.
* Keep shared SQL in `db/migration/common`; put dialect-specific SQL in `mysql` or `postgresql`.
* Before enabling automatic deployment, test every applicable SQL against its target PostgreSQL/MySQL version:
  apply it once, apply it again, interrupt it after partial progress and verify a safe retry. Compare schema,
  indexes, existing business rows, accounts and duplicate seed rows; a successful exit alone is insufficient.
* Flyway history prevents replay of successful versioned migrations. Existing historical files are not all
  safe to execute directly twice. Do not rewrite published migrations to add guards; use reviewed forward fixes.
  Existence guards must check the expected definition, not silently accept an incompatible existing object.
* This database uses logical `tenant_id` isolation. There is one history table per database, not one per tenant.
* Keep structural migrations short. Large-table updates, deletes, type rewrites, and backfills belong in a resumable
  worker using `nexiot_db_backfill_task`, with primary-key range pagination, bounded batches, throttling, and metrics.
* PostgreSQL `CREATE INDEX CONCURRENTLY` migrations must have `-- flyway:executeInTransaction=false` and be isolated
  in their own version. MySQL online DDL must state its algorithm/lock behavior after testing on the target version.
* Do not use `clean`, `outOfOrder`, or `mixed` migrations in production. Failed migrations are repaired forward;
  do not delete history rows by hand.

## Release gate

```text
build -> backup/PITR check -> migrator validate -> migrator migrate -> health checks -> roll application
```

The application rollout must wait for `migrate` to exit successfully. On failure, stop the rollout, preserve the
migration logs, inspect lock/replication/connection health, and apply a reviewed forward fix. For online index or
backfill work, abort when lock wait, replication lag, CPU, I/O, or API latency crosses the deployment threshold.

## Community scope

`V2026110701__open_source_menu_scope.sql` removes unavailable plugin menus and their grants. It preserves existing accounts and business rows. Fresh installations use the matching filtered seed in `docker/database/postgres`. PostgreSQL 18.4 is verified; MySQL and Dameng scope scripts have not been run against live databases in this acceptance.
