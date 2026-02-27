#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/install-maintenance-cron.sh /opt/patch-lifecycle"
  exit 1
fi

APP_DIR="${1:-/opt/patch-lifecycle}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
BACKUP_CRON="${BACKUP_CRON:-0 3 * * *}"
RENEW_CRON="${RENEW_CRON:-30 3 * * 1}"

CRON_FILE="/etc/cron.d/patch-lifecycle-maintenance"

cat > "${CRON_FILE}" <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# 每日数据库备份 + 清理过期备份
${BACKUP_CRON} root cd ${APP_DIR} && bash scripts/huawei/db-backup.sh && bash scripts/huawei/prune-backups.sh ${RETENTION_DAYS} >> /var/log/patch-lifecycle-maintenance.log 2>&1

# 每周证书续期检查
${RENEW_CRON} root cd ${APP_DIR} && bash scripts/huawei/renew-cert.sh >> /var/log/patch-lifecycle-maintenance.log 2>&1
EOF

chmod 644 "${CRON_FILE}"
systemctl restart cron 2>/dev/null || systemctl restart crond 2>/dev/null || true

echo "定时维护任务安装完成: ${CRON_FILE}"
echo "备份 cron: ${BACKUP_CRON}"
echo "续期 cron: ${RENEW_CRON}"
