#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行该脚本，例如: sudo bash scripts/huawei/server-init.sh"
  exit 1
fi

APP_PORT="${APP_PORT:-8080}"
DB_PORT="${DB_PORT:-5432}"

echo "[1/4] 安装 Docker..."
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi

systemctl enable docker
systemctl restart docker

if ! docker compose version >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    apt-get install -y docker-compose-plugin
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y docker-compose-plugin || true
  elif command -v yum >/dev/null 2>&1; then
    yum install -y docker-compose-plugin || true
  fi
fi

if [ -n "${SUDO_USER:-}" ] && [ "${SUDO_USER}" != "root" ]; then
  usermod -aG docker "${SUDO_USER}" || true
fi

echo "[2/4] 安装基础工具..."
if command -v apt-get >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y curl git tar
elif command -v dnf >/dev/null 2>&1; then
  dnf install -y curl git tar
elif command -v yum >/dev/null 2>&1; then
  yum install -y curl git tar
fi

echo "[3/4] 开放防火墙端口(若启用防火墙)..."
if command -v ufw >/dev/null 2>&1; then
  ufw allow 22/tcp || true
  ufw allow "${APP_PORT}/tcp" || true
  ufw allow "${DB_PORT}/tcp" || true
fi

if command -v firewall-cmd >/dev/null 2>&1 && systemctl is-active firewalld >/dev/null 2>&1; then
  firewall-cmd --permanent --add-port="${APP_PORT}/tcp" || true
  firewall-cmd --permanent --add-port="${DB_PORT}/tcp" || true
  firewall-cmd --reload || true
fi

echo "[4/4] 检查完成"
docker --version
docker compose version || true
echo "服务器初始化完成。请重新登录以生效 docker 用户组。"
