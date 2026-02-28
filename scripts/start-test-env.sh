#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/.runtime/test-env"
DATA_DIR="${RUNTIME_DIR}/data"
PID_FILE="${RUNTIME_DIR}/app.pid"
LOG_FILE="${RUNTIME_DIR}/app.log"
DB_FILE="${SQLITE_PATH:-${DATA_DIR}/patches-test.db}"

APP_PORT="${APP_PORT:-18080}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx1024m}"
START_TIMEOUT_SEC="${START_TIMEOUT_SEC:-90}"
SKIP_BUILD="${SKIP_BUILD:-false}"
TAIL_LINES="${TAIL_LINES:-200}"

usage() {
  cat <<'USAGE'
Usage:
  bash scripts/start-test-env.sh [start|stop|restart|status|health|logs]

Environment variables:
  APP_PORT=18080                    # Spring Boot listen port
  SQLITE_PATH=.runtime/test-env/data/patches-test.db
  JAVA_OPTS="-Xms256m -Xmx1024m"
  START_TIMEOUT_SEC=90
  SKIP_BUILD=false                  # true to skip mvn package
  TAIL_LINES=200                    # lines for logs command
USAGE
}

is_running() {
  if [[ ! -f "${PID_FILE}" ]]; then
    return 1
  fi

  local pid
  pid="$(cat "${PID_FILE}")"
  if [[ -z "${pid}" ]]; then
    return 1
  fi

  kill -0 "${pid}" >/dev/null 2>&1
}

resolve_jar() {
  shopt -s nullglob
  local jars=("${ROOT_DIR}"/target/*.jar)
  local jar
  for jar in "${jars[@]}"; do
    if [[ "${jar}" == *"original-"* ]]; then
      continue
    fi
    printf '%s' "${jar}"
    return 0
  done
  return 1
}

wait_for_health() {
  local url="http://127.0.0.1:${APP_PORT}/actuator/health"
  local i
  for ((i=1; i<=START_TIMEOUT_SEC; i++)); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "Test environment is UP: ${url}"
      return 0
    fi
    sleep 1
  done
  echo "Health check timeout after ${START_TIMEOUT_SEC}s: ${url}"
  return 1
}

start_app() {
  if is_running; then
    echo "Test environment is already running (PID: $(cat "${PID_FILE}"))."
    return 0
  fi

  mkdir -p "${RUNTIME_DIR}" "${DATA_DIR}"

  if [[ "${SKIP_BUILD}" != "true" ]]; then
    echo "[1/3] Building application jar..."
    (
      cd "${ROOT_DIR}"
      ./mvnw -DskipTests package
    )
  else
    echo "[1/3] Skipping build (SKIP_BUILD=true)."
  fi

  local jar
  jar="$(resolve_jar || true)"
  if [[ -z "${jar}" ]]; then
    echo "No runnable jar found under target/. Please run: ./mvnw -DskipTests package"
    exit 1
  fi

  echo "[2/3] Starting app with SQLite test database..."
  (
    cd "${ROOT_DIR}"
    DB_TYPE=sqlite \
    SQLITE_PATH="${DB_FILE}" \
    SERVER_PORT="${APP_PORT}" \
    nohup java ${JAVA_OPTS} -jar "${jar}" >"${LOG_FILE}" 2>&1 &
    echo $! > "${PID_FILE}"
  )

  echo "[3/3] Waiting for health check..."
  if ! wait_for_health; then
    echo "Startup failed. Recent logs:"
    if [[ -f "${LOG_FILE}" ]]; then
      tail -n 80 "${LOG_FILE}" || true
    fi
    stop_app || true
    exit 1
  fi

  echo "PID: $(cat "${PID_FILE}")"
  echo "Log: ${LOG_FILE}"
  echo "SQLite DB: ${DB_FILE}"
  echo "Swagger: http://127.0.0.1:${APP_PORT}/swagger-ui.html"
}

stop_app() {
  if [[ ! -f "${PID_FILE}" ]]; then
    echo "Test environment is not running."
    return 0
  fi

  local pid
  pid="$(cat "${PID_FILE}")"
  if [[ -z "${pid}" ]]; then
    rm -f "${PID_FILE}"
    echo "Removed empty PID file."
    return 0
  fi

  if kill -0 "${pid}" >/dev/null 2>&1; then
    echo "Stopping PID ${pid}..."
    kill "${pid}" >/dev/null 2>&1 || true
    local i
    for ((i=1; i<=15; i++)); do
      if ! kill -0 "${pid}" >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done
    if kill -0 "${pid}" >/dev/null 2>&1; then
      echo "Force killing PID ${pid}..."
      kill -9 "${pid}" >/dev/null 2>&1 || true
    fi
  fi

  rm -f "${PID_FILE}"
  echo "Test environment stopped."
}

status_app() {
  if is_running; then
    echo "RUNNING (PID: $(cat "${PID_FILE}"))"
    echo "Health URL: http://127.0.0.1:${APP_PORT}/actuator/health"
    echo "Log: ${LOG_FILE}"
    echo "SQLite DB: ${DB_FILE}"
  else
    echo "STOPPED"
  fi
}

health_app() {
  local url="http://127.0.0.1:${APP_PORT}/actuator/health"
  echo "Checking ${url}"
  curl -fsS "${url}"
  echo
}

logs_app() {
  mkdir -p "${RUNTIME_DIR}"
  touch "${LOG_FILE}"
  tail -n "${TAIL_LINES}" -f "${LOG_FILE}"
}

COMMAND="${1:-start}"
case "${COMMAND}" in
  start)
    start_app
    ;;
  stop)
    stop_app
    ;;
  restart)
    stop_app
    start_app
    ;;
  status)
    status_app
    ;;
  health)
    health_app
    ;;
  logs)
    logs_app
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    echo "Unknown command: ${COMMAND}"
    usage
    exit 1
    ;;
esac
