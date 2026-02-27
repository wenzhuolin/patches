#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/renew-cert.sh"
  exit 1
fi

if ! command -v certbot >/dev/null 2>&1; then
  echo "未安装 certbot，请先执行 enable-https.sh"
  exit 1
fi

certbot renew --nginx
nginx -t
systemctl reload nginx
echo "证书续期检查完成。"
