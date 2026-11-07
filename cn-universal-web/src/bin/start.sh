#!/usr/bin/env bash
set -euo pipefail
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="$APP_HOME/run/cn-universal-web.pid"
RUNTIME_ENV="${NEXIOT_RUNTIME_ENV_FILE:-$APP_HOME/config/runtime.env}"
if [ -f "$RUNTIME_ENV" ]; then
  set -a
  source "$RUNTIME_ENV"
  set +a
fi
mkdir -p "$APP_HOME/run" "$APP_HOME/logs"
is_own_process() {
  local process_args
  process_args=$(ps -p "$1" -o args= 2>/dev/null) || return 1
  [[ "$process_args" == *"$APP_HOME/cn-universal-web.jar"* && "$process_args" == *"cn.universal.CnUniversalIoTApplication"* ]]
}
if [ -f "$PID_FILE" ] && is_own_process "$(cat "$PID_FILE")"; then
  echo "NexIoT is already running: $(cat "$PID_FILE")"
  exit 0
fi
[ -f "$APP_HOME/cn-universal-web.jar" ] || { echo "Missing application JAR" >&2; exit 1; }
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
cd "$APP_HOME"
# JAVA_OPT contains JVM arguments; it is expanded as words and is never evaluated as shell code.
nohup "$JAVA_BIN" ${JAVA_OPT:--Xms256m -Xmx1024m} \
  -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8 \
  -cp "$APP_HOME/config:$APP_HOME/cn-universal-web.jar:$APP_HOME/lib/*" \
  cn.universal.CnUniversalIoTApplication \
  --spring.config.location="$APP_HOME/config/" \
  --logging.config="$APP_HOME/config/logback-spring.xml" \
  > "$APP_HOME/logs/console.log" 2>&1 &
APP_PID=$!
printf '%s\n' "$APP_PID" > "$PID_FILE"
HEALTH_URL="${NEXIOT_HEALTH_URL:-http://127.0.0.1:${SERVER_PORT:-9092}/actuator/health}"
for attempt in {1..90}; do
  if ! is_own_process "$APP_PID"; then
    echo "NexIoT exited; see $APP_HOME/logs/console.log" >&2
    exit 1
  fi
  if curl --silent --fail --max-time 2 "$HEALTH_URL" | grep -q '"status":"UP"'; then
    echo "NexIoT ready: PID=$APP_PID health=$HEALTH_URL"
    exit 0
  fi
  sleep 1
done
echo "NexIoT did not become healthy; see $APP_HOME/logs/console.log" >&2
exit 1
