#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

if [ ! -f ".env" ]; then
  cp .env.example .env
  echo "未检测到 .env，已生成模板。请先修改 .env 中的数据库密码后再次执行。"
  exit 1
fi

read_env_value() {
  local key="$1"
  local value
  value="$(awk -v k="${key}" '
    /^[[:space:]]*#/ { next }
    $0 ~ "^[[:space:]]*"k"[[:space:]]*=" {
      sub(/^[^=]*=/, "", $0)
      print $0
      exit
    }
  ' .env)"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  if [[ "${value}" =~ ^\".*\"$ ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "${value}" =~ ^\'.*\'$ ]]; then
    value="${value:1:${#value}-2}"
  fi
  printf '%s' "${value}"
}

APP_BUILD_ENABLED="$(read_env_value APP_BUILD_ENABLED)"
APP_PULL_IMAGE="$(read_env_value APP_PULL_IMAGE)"
COMPOSE_BUILD_PULL="$(read_env_value COMPOSE_BUILD_PULL)"
APP_PORT_VALUE="$(read_env_value APP_PORT)"
APP_IMAGE_VALUE="$(read_env_value APP_IMAGE)"
POSTGRES_IMAGE_VALUE="$(read_env_value POSTGRES_IMAGE)"

APP_BUILD_ENABLED="${APP_BUILD_ENABLED:-true}"
APP_PULL_IMAGE="${APP_PULL_IMAGE:-false}"
COMPOSE_BUILD_PULL="${COMPOSE_BUILD_PULL:-false}"
APP_PORT_VALUE="${APP_PORT_VALUE:-8080}"
APP_IMAGE_VALUE="${APP_IMAGE_VALUE:-patch-lifecycle:latest}"
POSTGRES_IMAGE_VALUE="${POSTGRES_IMAGE_VALUE:-postgres:16-alpine}"

echo "[1/4] 镜像策略检查..."
echo "APP_IMAGE=${APP_IMAGE_VALUE}"
echo "POSTGRES_IMAGE=${POSTGRES_IMAGE_VALUE}"
echo "APP_BUILD_ENABLED=${APP_BUILD_ENABLED}, APP_PULL_IMAGE=${APP_PULL_IMAGE}, COMPOSE_BUILD_PULL=${COMPOSE_BUILD_PULL}"

if [ "${APP_PULL_IMAGE}" = "true" ]; then
  echo "[2/4] 拉取应用镜像..."
  compose -f docker-compose.prod.yml pull app
elif [ "${APP_BUILD_ENABLED}" = "true" ]; then
  echo "[2/4] 构建应用镜像..."
  if [ "${COMPOSE_BUILD_PULL}" = "true" ]; then
    compose -f docker-compose.prod.yml build --pull app
  else
    compose -f docker-compose.prod.yml build app
  fi
else
  echo "[2/4] 跳过构建与拉取（使用本地已有镜像）"
fi

echo "[3/4] 启动服务..."
if [ "${APP_BUILD_ENABLED}" = "true" ]; then
  compose -f docker-compose.prod.yml up -d
else
  compose -f docker-compose.prod.yml up -d --no-build
fi

echo "[4/4] 状态检查..."
compose -f docker-compose.prod.yml ps
echo "部署完成。健康检查: curl http://127.0.0.1:${APP_PORT_VALUE}/actuator/health"
