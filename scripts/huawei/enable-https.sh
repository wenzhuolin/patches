#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/enable-https.sh api.example.com admin@example.com"
  exit 1
fi

if [ "$#" -lt 2 ]; then
  echo "用法: sudo bash scripts/huawei/enable-https.sh <domain> <email> [staging]"
  echo "示例: sudo bash scripts/huawei/enable-https.sh api.example.com admin@example.com"
  echo "测试证书: sudo bash scripts/huawei/enable-https.sh api.example.com admin@example.com staging"
  exit 1
fi

DOMAIN="$1"
EMAIL="$2"
STAGING="${3:-}"

install_certbot() {
  if command -v certbot >/dev/null 2>&1; then
    return
  fi
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    apt-get install -y certbot python3-certbot-nginx
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y certbot python3-certbot-nginx
  elif command -v yum >/dev/null 2>&1; then
    yum install -y certbot python3-certbot-nginx
  else
    echo "未识别系统包管理器，无法自动安装 certbot"
    exit 1
  fi
}

install_certbot

if [ ! -f "/etc/nginx/conf.d/patch-lifecycle.conf" ]; then
  echo "未检测到 /etc/nginx/conf.d/patch-lifecycle.conf，请先执行 install-reverse-proxy.sh"
  exit 1
fi

nginx -t
systemctl reload nginx

EXTRA_ARGS=""
if [ "${STAGING}" = "staging" ]; then
  EXTRA_ARGS="--staging"
fi

certbot --nginx \
  -d "${DOMAIN}" \
  -m "${EMAIL}" \
  --agree-tos \
  --no-eff-email \
  --redirect \
  --non-interactive \
  ${EXTRA_ARGS}

systemctl enable --now certbot.timer || true
nginx -t
systemctl reload nginx

echo "HTTPS 配置完成。请检查: https://${DOMAIN}/actuator/health"
