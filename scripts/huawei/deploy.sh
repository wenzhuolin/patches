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

if [ ! -f ".env" ]; then
  cp .env.example .env
  echo "未检测到 .env，已生成模板。请先修改 .env 中的数据库密码后再次执行。"
  exit 1
fi

echo "[1/3] 构建应用镜像..."
compose -f docker-compose.prod.yml build --pull app

echo "[2/3] 启动服务..."
compose -f docker-compose.prod.yml up -d

echo "[3/3] 状态检查..."
compose -f docker-compose.prod.yml ps
echo "部署完成。健康检查: curl http://127.0.0.1:${APP_PORT:-8080}/actuator/health"
