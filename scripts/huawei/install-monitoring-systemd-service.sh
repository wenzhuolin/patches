#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/install-monitoring-systemd-service.sh /opt/patch-lifecycle"
  exit 1
fi

APP_DIR="${1:-/opt/patch-lifecycle}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
TEMPLATE="${ROOT_DIR}/scripts/huawei/systemd/patch-lifecycle-monitoring.service.template"
TARGET="/etc/systemd/system/patch-lifecycle-monitoring.service"

cp "${TEMPLATE}" "${TARGET}"
sed -i "s|__APP_DIR__|${APP_DIR}|g" "${TARGET}"

systemctl daemon-reload
systemctl enable patch-lifecycle-monitoring.service
systemctl restart patch-lifecycle-monitoring.service
systemctl status patch-lifecycle-monitoring.service --no-pager -l || true

echo "监控 systemd 服务安装完成。"
echo "服务名: patch-lifecycle-monitoring.service"
