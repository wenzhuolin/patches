#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/switch-bluegreen.sh blue"
  exit 1
fi

if [ "$#" -lt 1 ]; then
  echo "用法: sudo bash scripts/huawei/switch-bluegreen.sh <blue|green>"
  exit 1
fi

SLOT="$1"
BLUE_PORT="${BLUE_PORT:-18080}"
GREEN_PORT="${GREEN_PORT:-28080}"
UPSTREAM_INC="/etc/nginx/conf.d/patch-lifecycle-upstream.inc"

case "${SLOT}" in
  blue)
    TARGET_PORT="${BLUE_PORT}"
    ;;
  green)
    TARGET_PORT="${GREEN_PORT}"
    ;;
  *)
    echo "非法 slot: ${SLOT}，仅支持 blue|green"
    exit 1
    ;;
esac

if [ ! -f "/etc/nginx/conf.d/patch-lifecycle.conf" ]; then
  echo "未检测到 Nginx 反向代理配置，请先执行 install-reverse-proxy.sh"
  exit 1
fi

echo "server 127.0.0.1:${TARGET_PORT} max_fails=3 fail_timeout=10s;" > "${UPSTREAM_INC}"
nginx -t
systemctl reload nginx

echo "切流成功，当前 active=${SLOT} (127.0.0.1:${TARGET_PORT})"
