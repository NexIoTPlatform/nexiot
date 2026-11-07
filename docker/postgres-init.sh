#!/bin/sh
set -eu
export PGHOST=127.0.0.1 PGUSER="$POSTGRES_USER" PGDATABASE="$POSTGRES_DB" PGPASSWORD="$POSTGRES_PASSWORD"
# The PostgreSQL entrypoint starts a socket-only temporary server for initialization.
unset PGHOST
sh /nexiot-seed/apply.sh
