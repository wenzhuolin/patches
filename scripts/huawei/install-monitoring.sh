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

if ! command -v docker >/dev/null 2>&1; then
  echo "未安装 docker，请先执行 scripts/huawei/server-init.sh"
  exit 1
fi

if [ ! -f ".env" ]; then
  cp .env.example .env
  echo "未检测到 .env，已生成模板。请先配置并重试。"
  exit 1
fi

if ! docker network inspect patch-lifecycle-internal >/dev/null 2>&1; then
  docker network create patch-lifecycle-internal
fi

echo "[1/2] 启动监控栈..."
compose -f docker-compose.monitoring.yml up -d

echo "[2/2] Prometheus 重新加载配置..."
curl -fsS -X POST "http://127.0.0.1:${PROM_PORT:-9090}/-/reload" >/dev/null || true

echo "[3/3] 状态检查..."
compose -f docker-compose.monitoring.yml ps
echo "Prometheus: http://127.0.0.1:${PROM_PORT:-9090}"
echo "Alertmanager: http://127.0.0.1:${ALERTMANAGER_PORT:-9093}"
echo "Grafana: http://127.0.0.1:${GRAFANA_PORT:-3000} (默认 admin/admin，请立即修改)"
