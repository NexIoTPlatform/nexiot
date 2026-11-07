#!/usr/bin/env bash
set -euo pipefail
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="$APP_HOME/run/cn-universal-web.pid"
[ -f "$PID_FILE" ] || { echo "NexIoT is not running in $APP_HOME"; exit 0; }
APP_PID=$(cat "$PID_FILE")
[[ "$APP_PID" =~ ^[0-9]+$ ]] || { echo "Invalid PID file" >&2; exit 1; }
is_own_process() {
  local process_args
  process_args=$(ps -p "$APP_PID" -o args= 2>/dev/null) || return 1
  [[ "$process_args" == *"$APP_HOME/cn-universal-web.jar"* && "$process_args" == *"cn.universal.CnUniversalIoTApplication"* ]]
}
if ! is_own_process; then
  rm -f "$PID_FILE"
  echo "No matching NexIoT process for this installation"
  exit 0
fi
kill "$APP_PID"
for attempt in {1..30}; do
  if ! is_own_process; then
    rm -f "$PID_FILE"
    echo "NexIoT stopped"
    exit 0
  fi
  sleep 1
done
if is_own_process; then kill -9 "$APP_PID"; fi
rm -f "$PID_FILE"
echo "NexIoT stopped after timeout"
