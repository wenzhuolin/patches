#!/usr/bin/env bash
set -euo pipefail

PROM_URL="${1:-http://127.0.0.1:9090}"
GRAFANA_URL="${2:-http://127.0.0.1:3000}"

echo "[1/3] Prometheus 健康检查..."
curl -fsS "${PROM_URL%/}/-/healthy" >/dev/null
echo "Prometheus healthy"

echo "[2/3] Grafana 健康检查..."
curl -fsS "${GRAFANA_URL%/}/api/health" >/dev/null
echo "Grafana healthy"

echo "[3/3] Prometheus target 快照..."
TARGETS_JSON="$(curl -fsS "${PROM_URL%/}/api/v1/targets")"
echo "${TARGETS_JSON}" | tr '\n' ' ' | cut -c1-500
echo
echo "Monitoring smoke 完成"
