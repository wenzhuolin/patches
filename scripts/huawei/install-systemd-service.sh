#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/install-systemd-service.sh /opt/patch-lifecycle prod"
  exit 1
fi

APP_DIR="${1:-/opt/patch-lifecycle}"
MODE="${2:-prod}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
TEMPLATE="${ROOT_DIR}/scripts/huawei/systemd/patch-lifecycle.service.template"
TARGET="/etc/systemd/system/patch-lifecycle.service"

if [ "${MODE}" = "bluegreen" ]; then
  COMPOSE_FILE="docker-compose.bluegreen.yml"
else
  COMPOSE_FILE="docker-compose.prod.yml"
fi

cp "${TEMPLATE}" "${TARGET}"
sed -i "s|__APP_DIR__|${APP_DIR}|g" "${TARGET}"
sed -i "s|__COMPOSE_FILE__|${COMPOSE_FILE}|g" "${TARGET}"

systemctl daemon-reload
systemctl enable patch-lifecycle.service
systemctl restart patch-lifecycle.service
systemctl status patch-lifecycle.service --no-pager -l || true

echo "systemd 服务安装完成。"
echo "服务名: patch-lifecycle.service"
echo "模式: ${MODE}"
