#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/bootstrap-test-ip.sh"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

APP_DIR="${ROOT_DIR}"
APP_PORT="${APP_PORT:-8080}"
DOCKER_REGISTRY_MIRRORS="${DOCKER_REGISTRY_MIRRORS:-https://docker.m.daocloud.io,https://mirror.ccs.tencentyun.com,https://hub-mirror.c.163.com}"
HUAWEI_SWR_MIRROR="${HUAWEI_SWR_MIRROR:-}"
ENABLE_HUAWEI_APT_MIRROR="${ENABLE_HUAWEI_APT_MIRROR:-true}"
HUAWEI_APT_MIRROR="${HUAWEI_APT_MIRROR:-https://repo.huaweicloud.com}"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --app-dir)
      APP_DIR="${2:-${ROOT_DIR}}"
      shift 2
      ;;
    --app-port)
      APP_PORT="${2:-8080}"
      shift 2
      ;;
    --docker-mirrors)
      DOCKER_REGISTRY_MIRRORS="${2:-${DOCKER_REGISTRY_MIRRORS}}"
      shift 2
      ;;
    --swr-mirror)
      HUAWEI_SWR_MIRROR="${2:-${HUAWEI_SWR_MIRROR}}"
      shift 2
      ;;
    --disable-huawei-apt-mirror)
      ENABLE_HUAWEI_APT_MIRROR="false"
      shift 1
      ;;
    *)
      echo "未知参数: $1"
      echo "用法: sudo bash scripts/huawei/bootstrap-test-ip.sh [--app-dir /opt/patch-lifecycle] [--app-port 8080] [--docker-mirrors ...] [--swr-mirror ...] [--disable-huawei-apt-mirror]"
      exit 1
      ;;
  esac
done

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
  else
    date +%s%N | sha256sum | awk '{print $1}' | cut -c1-32
  fi
}

set_env_value() {
  local key="$1"
  local value="$2"
  if grep -qE "^[[:space:]]*${key}=" .env; then
    sed -i "s|^[[:space:]]*${key}=.*|${key}=${value}|g" .env
  else
    printf '%s=%s\n' "${key}" "${value}" >> .env
  fi
}

if [ ! -f ".env" ]; then
  cp .env.example .env
fi

set_env_value APP_BIND_IP "0.0.0.0"
set_env_value DB_BIND_IP "127.0.0.1"
set_env_value APP_PORT "${APP_PORT}"
set_env_value APP_BUILD_ENABLED "true"
set_env_value APP_PULL_IMAGE "false"
set_env_value COMPOSE_BUILD_PULL "false"
if [ -n "${HUAWEI_SWR_MIRROR}" ]; then
  set_env_value HUAWEI_SWR_MIRROR "\"${HUAWEI_SWR_MIRROR}\""
  DOCKER_REGISTRY_MIRRORS="${HUAWEI_SWR_MIRROR},${DOCKER_REGISTRY_MIRRORS}"
fi
set_env_value DOCKER_REGISTRY_MIRRORS "\"${DOCKER_REGISTRY_MIRRORS}\""
set_env_value ENABLE_HUAWEI_APT_MIRROR "${ENABLE_HUAWEI_APT_MIRROR}"
set_env_value HUAWEI_APT_MIRROR "${HUAWEI_APT_MIRROR}"

if grep -q '^POSTGRES_PASSWORD=please_change_me' .env || grep -q '^POSTGRES_PASSWORD=$' .env; then
  set_env_value POSTGRES_PASSWORD "$(random_secret)"
fi
if grep -q '^GRAFANA_ADMIN_PASSWORD=please_change_grafana_password' .env || grep -q '^GRAFANA_ADMIN_PASSWORD=$' .env; then
  set_env_value GRAFANA_ADMIN_PASSWORD "$(random_secret)"
fi

export DOCKER_REGISTRY_MIRRORS
if [ -n "${HUAWEI_SWR_MIRROR}" ]; then
  export HUAWEI_SWR_MIRROR
fi
export ENABLE_HUAWEI_APT_MIRROR
export HUAWEI_APT_MIRROR

bash scripts/huawei/bootstrap-production.sh \
  --mode prod \
  --app-dir "${APP_DIR}" \
  --with-monitoring false \
  --with-maintenance-cron false

SERVER_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
if [ -z "${SERVER_IP}" ]; then
  SERVER_IP="<服务器IP>"
fi
echo "测试环境部署完成："
echo "- Swagger: http://${SERVER_IP}:${APP_PORT}/swagger-ui.html"
echo "- Health:  http://${SERVER_IP}:${APP_PORT}/actuator/health"
