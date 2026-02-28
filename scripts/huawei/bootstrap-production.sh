#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行，例如: sudo bash scripts/huawei/bootstrap-production.sh --domain api.example.com --email admin@example.com --mode bluegreen"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${ROOT_DIR}"

DOMAIN=""
EMAIL=""
MODE="prod"
APP_DIR="${ROOT_DIR}"
WITH_MONITORING="true"
WITH_MAINTENANCE_CRON="true"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --domain)
      DOMAIN="${2:-}"
      shift 2
      ;;
    --email)
      EMAIL="${2:-}"
      shift 2
      ;;
    --mode)
      MODE="${2:-prod}"
      shift 2
      ;;
    --app-dir)
      APP_DIR="${2:-${ROOT_DIR}}"
      shift 2
      ;;
    --with-monitoring)
      WITH_MONITORING="${2:-true}"
      shift 2
      ;;
    --with-maintenance-cron)
      WITH_MAINTENANCE_CRON="${2:-true}"
      shift 2
      ;;
    *)
      echo "未知参数: $1"
      exit 1
      ;;
  esac
done

if [ ! -f ".env" ]; then
  cp .env.example .env
  echo "已创建 .env，请先修改 POSTGRES_PASSWORD 后重新执行。"
  echo "如需免配置测试环境一键安装，可执行: sudo bash scripts/huawei/bootstrap-test-ip.sh"
  exit 1
fi

read_env_value() {
  local key="$1"
  local value
  value="$(awk -v k="${key}" '
    /^[[:space:]]*#/ { next }
    $0 ~ "^[[:space:]]*"k"[[:space:]]*=" {
      sub(/^[^=]*=/, "", $0)
      print $0
      exit
    }
  ' .env)"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  if [[ "${value}" =~ ^\".*\"$ ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "${value}" =~ ^\'.*\'$ ]]; then
    value="${value:1:${#value}-2}"
  fi
  printf '%s' "${value}"
}

ENV_DOCKER_MIRRORS="$(read_env_value DOCKER_REGISTRY_MIRRORS)"
ENV_HUAWEI_SWR_MIRROR="$(read_env_value HUAWEI_SWR_MIRROR)"
ENV_ENABLE_HUAWEI_APT_MIRROR="$(read_env_value ENABLE_HUAWEI_APT_MIRROR)"
ENV_HUAWEI_APT_MIRROR="$(read_env_value HUAWEI_APT_MIRROR)"
if [ -n "${ENV_DOCKER_MIRRORS}" ]; then
  export DOCKER_REGISTRY_MIRRORS="${ENV_DOCKER_MIRRORS}"
fi
if [ -n "${ENV_HUAWEI_SWR_MIRROR}" ]; then
  export HUAWEI_SWR_MIRROR="${ENV_HUAWEI_SWR_MIRROR}"
fi
if [ -n "${ENV_ENABLE_HUAWEI_APT_MIRROR}" ]; then
  export ENABLE_HUAWEI_APT_MIRROR="${ENV_ENABLE_HUAWEI_APT_MIRROR}"
fi
if [ -n "${ENV_HUAWEI_APT_MIRROR}" ]; then
  export HUAWEI_APT_MIRROR="${ENV_HUAWEI_APT_MIRROR}"
fi

echo "[1/8] 服务器初始化"
bash scripts/huawei/server-init.sh

echo "[2/8] 预检"
bash scripts/huawei/preflight-check.sh "${DOMAIN:-}"

if [ "${MODE}" = "bluegreen" ]; then
  echo "[3/8] 蓝绿部署"
  bash scripts/huawei/deploy-bluegreen.sh
  UPSTREAM="127.0.0.1:${BLUE_PORT:-18080}"
else
  echo "[3/8] 标准部署"
  bash scripts/huawei/deploy.sh
  UPSTREAM="127.0.0.1:${APP_PORT:-8080}"
fi

if [ -n "${DOMAIN}" ]; then
  echo "[4/8] 安装 Nginx 反向代理"
  bash scripts/huawei/install-reverse-proxy.sh "${DOMAIN}" "${UPSTREAM}"
  if [ -n "${EMAIL}" ]; then
    echo "[5/8] 配置 HTTPS"
    bash scripts/huawei/enable-https.sh "${DOMAIN}" "${EMAIL}"
  else
    echo "[5/8] 跳过 HTTPS（未提供 --email）"
  fi
else
  echo "[4/8] 跳过 Nginx/HTTPS（未提供 --domain）"
  echo "[5/8] 跳过 HTTPS（未提供 --domain）"
fi

echo "[6/8] 安装应用 systemd 守护"
bash scripts/huawei/install-systemd-service.sh "${APP_DIR}" "${MODE}"

if [ "${WITH_MONITORING}" = "true" ]; then
  echo "[7/8] 安装监控栈与监控 systemd"
  bash scripts/huawei/install-monitoring.sh
  bash scripts/huawei/install-monitoring-systemd-service.sh "${APP_DIR}"
else
  echo "[7/8] 跳过监控安装 (--with-monitoring=${WITH_MONITORING})"
fi

if [ "${WITH_MAINTENANCE_CRON}" = "true" ]; then
  echo "[8/8] 安装定时维护任务"
  bash scripts/huawei/install-maintenance-cron.sh "${APP_DIR}"
else
  echo "[8/8] 跳过定时维护任务 (--with-maintenance-cron=${WITH_MAINTENANCE_CRON})"
fi

echo "Bootstrap 完成。"
