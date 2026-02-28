#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
TENANT_ID="${TENANT_ID:-1}"
USER_ID="${USER_ID:-1}"
ROLES="${ROLES:-SUPER_ADMIN}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

cd "${ROOT_DIR}"

auth_headers=(
  -H "X-Tenant-Id: ${TENANT_ID}"
  -H "X-User-Id: ${USER_ID}"
  -H "X-Roles: ${ROLES}"
  -H "X-Trace-Id: smoke-$(date +%s)"
)

echo "[1/4] 健康检查..."
curl -fsS "${BASE_URL%/}/actuator/health" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'
echo "健康检查通过"

echo "[2/4] OpenAPI 检查..."
curl -fsS "${BASE_URL%/}/v3/api-docs" >/dev/null
echo "OpenAPI 可访问"

echo "[3/4] 业务接口检查..."
curl -fsS "${auth_headers[@]}" "${BASE_URL%/}/api/v1/kpi/rules" >/dev/null
curl -fsS "${auth_headers[@]}" "${BASE_URL%/}/api/v1/qa/policies" >/dev/null
curl -fsS "${auth_headers[@]}" "${BASE_URL%/}/api/v1/patches" >/dev/null
echo "核心接口可访问"

echo "[4/4] 容器状态检查..."
if docker compose version >/dev/null 2>&1; then
  docker compose -f docker-compose.prod.yml ps || true
else
  docker-compose -f docker-compose.prod.yml ps || true
fi

echo "Smoke 测试通过"
