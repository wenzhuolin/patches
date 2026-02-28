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
  echo "未找到 .env，请先部署并准备环境变量文件。"
  exit 1
fi

mkdir -p backups
BACKUP_FILE="backups/patches-$(date +%Y%m%d%H%M%S).sql"

set -a
source .env
set +a

compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" > "${BACKUP_FILE}"

echo "数据库备份完成: ${BACKUP_FILE}"
