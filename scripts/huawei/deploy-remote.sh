#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "用法: bash scripts/huawei/deploy-remote.sh <user@server-ip> [remote-dir]"
  echo "示例: bash scripts/huawei/deploy-remote.sh ubuntu@1.2.3.4 /opt/patch-lifecycle"
  exit 1
fi

REMOTE="$1"
REMOTE_DIR="${2:-/opt/patch-lifecycle}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ARCHIVE_NAME="patch-lifecycle-$(date +%Y%m%d%H%M%S).tar.gz"
TMP_ARCHIVE="/tmp/${ARCHIVE_NAME}"

echo "[1/4] 打包本地代码..."
tar --exclude='.git' --exclude='target' --exclude='.maven' -czf "${TMP_ARCHIVE}" -C "${ROOT_DIR}" .

echo "[2/4] 上传到远端 ${REMOTE}..."
scp "${TMP_ARCHIVE}" "${REMOTE}:/tmp/${ARCHIVE_NAME}"

echo "[3/4] 解压并部署..."
ssh "${REMOTE}" "set -euo pipefail; mkdir -p '${REMOTE_DIR}'; tar -xzf '/tmp/${ARCHIVE_NAME}' -C '${REMOTE_DIR}'; rm -f '/tmp/${ARCHIVE_NAME}'; cd '${REMOTE_DIR}'; bash scripts/huawei/deploy.sh"

echo "[4/4] 清理本地临时包..."
rm -f "${TMP_ARCHIVE}"

echo "远程部署完成。"
