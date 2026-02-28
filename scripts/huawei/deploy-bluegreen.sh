#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/deploy-bluegreen.sh"
  exit 1
fi

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
  echo "未检测到 .env，已生成模板。请先修改 .env 后再执行。"
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

BLUE_PORT="$(read_env_value BLUE_PORT)"
GREEN_PORT="$(read_env_value GREEN_PORT)"
APP_BUILD_ENABLED="$(read_env_value APP_BUILD_ENABLED)"
APP_PULL_IMAGE="$(read_env_value APP_PULL_IMAGE)"
COMPOSE_BUILD_PULL="$(read_env_value COMPOSE_BUILD_PULL)"
STOP_OLD="$(read_env_value STOP_OLD)"

BLUE_PORT="${BLUE_PORT:-18080}"
GREEN_PORT="${GREEN_PORT:-28080}"
APP_BUILD_ENABLED="${APP_BUILD_ENABLED:-true}"
APP_PULL_IMAGE="${APP_PULL_IMAGE:-false}"
COMPOSE_BUILD_PULL="${COMPOSE_BUILD_PULL:-false}"
STOP_OLD="${STOP_OLD:-false}"

UPSTREAM_INC="/etc/nginx/conf.d/patch-lifecycle-upstream.inc"
ACTIVE_SLOT="none"

if [ -f "${UPSTREAM_INC}" ]; then
  if grep -q "127.0.0.1:${BLUE_PORT}" "${UPSTREAM_INC}"; then
    ACTIVE_SLOT="blue"
  elif grep -q "127.0.0.1:${GREEN_PORT}" "${UPSTREAM_INC}"; then
    ACTIVE_SLOT="green"
  fi
fi

if [ "${ACTIVE_SLOT}" = "blue" ]; then
  TARGET_SLOT="green"
  TARGET_PORT="${GREEN_PORT}"
  OLD_SLOT="blue"
elif [ "${ACTIVE_SLOT}" = "green" ]; then
  TARGET_SLOT="blue"
  TARGET_PORT="${BLUE_PORT}"
  OLD_SLOT="green"
else
  TARGET_SLOT="blue"
  TARGET_PORT="${BLUE_PORT}"
  OLD_SLOT="none"
fi

echo "当前 active=${ACTIVE_SLOT}, 目标部署 slot=${TARGET_SLOT}"
echo "[1/5] 拉起数据库..."
compose -f docker-compose.bluegreen.yml up -d postgres

if [ "${APP_PULL_IMAGE}" = "true" ]; then
  echo "[2/5] 拉取目标槽位镜像..."
  compose -f docker-compose.bluegreen.yml pull "app-${TARGET_SLOT}"
elif [ "${APP_BUILD_ENABLED}" = "true" ]; then
  echo "[2/5] 构建目标槽位镜像..."
  if [ "${COMPOSE_BUILD_PULL}" = "true" ]; then
    compose -f docker-compose.bluegreen.yml build --pull "app-${TARGET_SLOT}"
  else
    compose -f docker-compose.bluegreen.yml build "app-${TARGET_SLOT}"
  fi
else
  echo "[2/5] 跳过构建与拉取（使用本地已有镜像）"
fi

echo "[3/5] 拉起目标槽位..."
if [ "${APP_BUILD_ENABLED}" = "true" ]; then
  compose -f docker-compose.bluegreen.yml up -d "app-${TARGET_SLOT}"
else
  compose -f docker-compose.bluegreen.yml up -d --no-build "app-${TARGET_SLOT}"
fi

echo "[4/5] 等待目标槽位健康..."
TRIES=60
until [ "${TRIES}" -le 0 ]; do
  if curl -fsS "http://127.0.0.1:${TARGET_PORT}/actuator/health" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    break
  fi
  TRIES=$((TRIES - 1))
  sleep 2
done

if [ "${TRIES}" -le 0 ]; then
  echo "目标槽位健康检查失败，发布终止。"
  exit 1
fi

echo "[5/5] 切换 Nginx upstream..."
bash scripts/huawei/switch-bluegreen.sh "${TARGET_SLOT}"

if [ "${STOP_OLD:-false}" = "true" ] && [ "${OLD_SLOT}" != "none" ]; then
  echo "停止旧槽位 app-${OLD_SLOT}"
  compose -f docker-compose.bluegreen.yml stop "app-${OLD_SLOT}"
else
  echo "保留旧槽位运行，便于秒级回滚"
fi

echo "蓝绿发布成功。当前 active=${TARGET_SLOT}"
if [ "${OLD_SLOT}" != "none" ]; then
  echo "回滚命令: sudo bash scripts/huawei/switch-bluegreen.sh ${OLD_SLOT}"
fi
