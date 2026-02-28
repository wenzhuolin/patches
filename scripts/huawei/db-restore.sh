#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "用法: bash scripts/huawei/db-restore.sh <backup-file.sql>"
  echo "示例: bash scripts/huawei/db-restore.sh backups/patches-20260227120000.sql"
  exit 1
fi

BACKUP_FILE="$1"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

if [ ! -f "${BACKUP_FILE}" ]; then
  echo "备份文件不存在: ${BACKUP_FILE}"
  exit 1
fi

if [ ! -f ".env" ]; then
  echo "未找到 .env，请先准备配置文件。"
  exit 1
fi

set -a
source .env
set +a

echo "将恢复数据库: ${POSTGRES_DB} (容器 postgres)"
echo "危险操作：将覆盖现有数据，请确认。"
read -r -p "输入 yes 继续: " confirm
if [ "${confirm}" != "yes" ]; then
  echo "已取消。"
  exit 1
fi

compose -f docker-compose.prod.yml exec -T postgres \
  psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" < "${BACKUP_FILE}"

echo "数据库恢复完成。"
