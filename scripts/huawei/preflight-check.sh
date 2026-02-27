#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:-}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

ok() { echo "[OK] $*"; }
warn() { echo "[WARN] $*"; }
fail() { echo "[FAIL] $*"; exit 1; }

echo "== 部署前预检开始 =="

if [ ! -f "${ENV_FILE}" ]; then
  fail "未找到 .env，请先 cp .env.example .env 并填写配置"
fi
ok ".env 存在"

# shellcheck disable=SC1090
set -a; source "${ENV_FILE}"; set +a

if [ "${POSTGRES_PASSWORD:-}" = "please_change_me" ] || [ -z "${POSTGRES_PASSWORD:-}" ]; then
  fail "POSTGRES_PASSWORD 未修改或为空"
fi
ok "数据库密码已配置"

command -v docker >/dev/null 2>&1 || fail "docker 未安装"
ok "docker 已安装"

if ! docker info >/dev/null 2>&1; then
  fail "docker daemon 不可用，请检查 systemctl status docker"
fi
ok "docker daemon 可用"

if docker compose version >/dev/null 2>&1; then
  ok "docker compose 可用"
elif command -v docker-compose >/dev/null 2>&1; then
  ok "docker-compose 可用"
else
  fail "compose 不可用，请安装 docker compose plugin"
fi

if command -v nginx >/dev/null 2>&1; then
  ok "nginx 已安装"
else
  warn "nginx 未安装（如需域名反代/HTTPS，请执行 install-reverse-proxy.sh）"
fi

if [ -n "${DOMAIN}" ]; then
  if ! command -v getent >/dev/null 2>&1; then
    warn "系统不支持 getent，跳过域名解析校验"
  else
    DNS_IP="$(getent ahosts "${DOMAIN}" | awk 'NR==1{print $1}')"
    if [ -z "${DNS_IP}" ]; then
      fail "域名 ${DOMAIN} 未解析到IP"
    fi
    PUBLIC_IP="$(curl -4fsS https://ifconfig.me || true)"
    if [ -n "${PUBLIC_IP}" ] && [ "${DNS_IP}" != "${PUBLIC_IP}" ]; then
      warn "域名解析IP(${DNS_IP}) 与服务器公网IP(${PUBLIC_IP}) 不一致"
    else
      ok "域名解析校验通过: ${DOMAIN} -> ${DNS_IP}"
    fi
  fi
fi

for port in "${APP_PORT:-8080}" "${DB_PORT:-5432}" 80 443; do
  if ss -ltn "( sport = :${port} )" | grep -q ":${port}"; then
    warn "端口 ${port} 已被占用，请确认是否预期"
  else
    ok "端口 ${port} 空闲（或未监听）"
  fi
done

echo "== 预检完成 =="
