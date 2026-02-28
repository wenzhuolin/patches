#!/usr/bin/env bash
set -euo pipefail

PROM_URL="${1:-http://127.0.0.1:9090}"
GRAFANA_URL="${2:-http://127.0.0.1:3000}"
ALERTMANAGER_URL="${3:-http://127.0.0.1:9093}"

echo "[1/3] Prometheus 健康检查..."
curl -fsS "${PROM_URL%/}/-/healthy" >/dev/null
echo "Prometheus healthy"

echo "[2/4] Alertmanager 健康检查..."
curl -fsS "${ALERTMANAGER_URL%/}/-/healthy" >/dev/null
echo "Alertmanager healthy"

echo "[3/4] Grafana 健康检查..."
curl -fsS "${GRAFANA_URL%/}/api/health" >/dev/null
echo "Grafana healthy"

echo "[4/4] Prometheus targets & rules 快照..."
TARGETS_JSON="$(curl -fsS "${PROM_URL%/}/api/v1/targets")"
echo "${TARGETS_JSON}" | tr '\n' ' ' | cut -c1-500
echo
RULES_JSON="$(curl -fsS "${PROM_URL%/}/api/v1/rules")"
echo "${RULES_JSON}" | tr '\n' ' ' | cut -c1-500
echo
echo "Monitoring smoke 完成"
