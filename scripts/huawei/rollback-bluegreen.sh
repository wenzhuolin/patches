#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/rollback-bluegreen.sh"
  exit 1
fi

BLUE_PORT="${BLUE_PORT:-18080}"
GREEN_PORT="${GREEN_PORT:-28080}"
UPSTREAM_INC="/etc/nginx/conf.d/patch-lifecycle-upstream.inc"
TARGET_SLOT="${1:-}"

if [ -z "${TARGET_SLOT}" ]; then
  if [ -f "${UPSTREAM_INC}" ] && grep -q "127.0.0.1:${BLUE_PORT}" "${UPSTREAM_INC}"; then
    TARGET_SLOT="green"
  else
    TARGET_SLOT="blue"
  fi
fi

bash scripts/huawei/switch-bluegreen.sh "${TARGET_SLOT}"
echo "回滚完成，当前 active=${TARGET_SLOT}"
