#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
URL="${BASE_URL%/}/actuator/health"

echo "检查健康状态: ${URL}"
RESPONSE="$(curl -fsS "${URL}")"
echo "${RESPONSE}"

if echo "${RESPONSE}" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
  echo "健康检查通过"
else
  echo "健康检查失败"
  exit 1
fi
