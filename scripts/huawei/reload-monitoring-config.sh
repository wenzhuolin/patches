#!/usr/bin/env bash
set -euo pipefail

PROM_URL="${1:-http://127.0.0.1:9090}"

echo "[1/2] 触发 Prometheus 配置热加载..."
curl -fsS -X POST "${PROM_URL%/}/-/reload" >/dev/null
echo "Prometheus reload 完成"

echo "[2/2] 验证规则状态..."
curl -fsS "${PROM_URL%/}/api/v1/rules" | tr '\n' ' ' | cut -c1-600
echo
echo "监控配置热加载完成"
