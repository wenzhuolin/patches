#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/install-reverse-proxy.sh your.domain.com"
  exit 1
fi

if [ "$#" -lt 1 ]; then
  echo "用法: sudo bash scripts/huawei/install-reverse-proxy.sh <domain> [upstream]"
  echo "示例: sudo bash scripts/huawei/install-reverse-proxy.sh api.example.com 127.0.0.1:8080"
  exit 1
fi

DOMAIN="$1"
UPSTREAM="${2:-127.0.0.1:8080}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
TEMPLATE_FILE="${ROOT_DIR}/scripts/huawei/nginx/patch-lifecycle.conf.template"
TARGET_CONF="/etc/nginx/conf.d/patch-lifecycle.conf"
UPSTREAM_INC="/etc/nginx/conf.d/patch-lifecycle-upstream.inc"

if ! command -v nginx >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    apt-get install -y nginx
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y nginx
  elif command -v yum >/dev/null 2>&1; then
    yum install -y nginx
  else
    echo "未识别系统包管理器，无法自动安装 nginx"
    exit 1
  fi
fi

mkdir -p /var/www/certbot
cp "${TEMPLATE_FILE}" "${TARGET_CONF}"
sed -i "s/__SERVER_NAME__/${DOMAIN}/g" "${TARGET_CONF}"
echo "server ${UPSTREAM} max_fails=3 fail_timeout=10s;" > "${UPSTREAM_INC}"

nginx -t
systemctl enable --now nginx
systemctl reload nginx

echo "Nginx 反向代理安装完成。"
echo "域名: ${DOMAIN}"
echo "上游: ${UPSTREAM}"
