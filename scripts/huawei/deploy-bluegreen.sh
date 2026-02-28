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

set -a
source .env
set +a

BLUE_PORT="${BLUE_PORT:-18080}"
GREEN_PORT="${GREEN_PORT:-28080}"
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
echo "[1/4] 拉起数据库和目标槽位..."
compose -f docker-compose.bluegreen.yml up -d --build postgres "app-${TARGET_SLOT}"

echo "[2/4] 等待目标槽位健康..."
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

echo "[3/4] 切换 Nginx upstream..."
bash scripts/huawei/switch-bluegreen.sh "${TARGET_SLOT}"

if [ "${STOP_OLD:-false}" = "true" ] && [ "${OLD_SLOT}" != "none" ]; then
  echo "[4/4] 停止旧槽位 app-${OLD_SLOT}"
  compose -f docker-compose.bluegreen.yml stop "app-${OLD_SLOT}"
else
  echo "[4/4] 保留旧槽位运行，便于秒级回滚"
fi

echo "蓝绿发布成功。当前 active=${TARGET_SLOT}"
if [ "${OLD_SLOT}" != "none" ]; then
  echo "回滚命令: sudo bash scripts/huawei/switch-bluegreen.sh ${OLD_SLOT}"
fi
