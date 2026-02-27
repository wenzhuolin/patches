#!/usr/bin/env bash
set -euo pipefail

RETENTION_DAYS="${1:-14}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
BACKUP_DIR="${ROOT_DIR}/backups"

mkdir -p "${BACKUP_DIR}"
find "${BACKUP_DIR}" -type f -name "*.sql" -mtime +"${RETENTION_DAYS}" -print -delete
echo "备份清理完成，保留最近 ${RETENTION_DAYS} 天。"
