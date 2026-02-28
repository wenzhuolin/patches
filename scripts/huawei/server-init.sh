#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  echo "请使用 root 执行该脚本，例如: sudo bash scripts/huawei/server-init.sh"
  exit 1
fi

APP_PORT="${APP_PORT:-8080}"
DB_PORT="${DB_PORT:-5432}"
ENABLE_DOCKER_MIRROR="${ENABLE_DOCKER_MIRROR:-true}"
DOCKER_REGISTRY_MIRRORS="${DOCKER_REGISTRY_MIRRORS:-https://docker.m.daocloud.io,https://mirror.ccs.tencentyun.com,https://hub-mirror.c.163.com}"
HUAWEI_SWR_MIRROR="${HUAWEI_SWR_MIRROR:-}"
ENABLE_HUAWEI_APT_MIRROR="${ENABLE_HUAWEI_APT_MIRROR:-true}"
HUAWEI_APT_MIRROR="${HUAWEI_APT_MIRROR:-https://repo.huaweicloud.com}"

echo "[1/4] 安装 Docker..."
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi

systemctl enable docker
systemctl restart docker

configure_docker_mirror() {
  if [ "${ENABLE_DOCKER_MIRROR}" != "true" ]; then
    echo "[Docker镜像加速] 跳过（ENABLE_DOCKER_MIRROR=${ENABLE_DOCKER_MIRROR}）"
    return 0
  fi
  if ! command -v python3 >/dev/null 2>&1; then
    echo "[Docker镜像加速] 未检测到 python3，跳过自动配置。"
    return 0
  fi
  local daemon_file="/etc/docker/daemon.json"
  local tmp_file
  tmp_file="$(mktemp)"
  python3 - "$daemon_file" "$DOCKER_REGISTRY_MIRRORS" "$HUAWEI_SWR_MIRROR" "$tmp_file" <<'PY'
import json
import os
import sys
from pathlib import Path

daemon_path = Path(sys.argv[1])
mirrors_raw = sys.argv[2]
hw_mirror = sys.argv[3].strip()
tmp_path = Path(sys.argv[4])
mirrors = [m.strip() for m in mirrors_raw.split(",") if m.strip()]
if hw_mirror:
    mirrors = [hw_mirror] + mirrors

data = {}
if daemon_path.exists():
    try:
        data = json.loads(daemon_path.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            data = {}
    except Exception:
        data = {}

existing = data.get("registry-mirrors", [])
if not isinstance(existing, list):
    existing = []
merged = []
for item in existing + mirrors:
    if item and item not in merged:
        merged.append(item)
data["registry-mirrors"] = merged
tmp_path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
PY
  install -m 0644 "${tmp_file}" "${daemon_file}"
  rm -f "${tmp_file}"
  systemctl restart docker
  if [ -n "${HUAWEI_SWR_MIRROR}" ]; then
    echo "[Docker镜像加速] 已优先配置华为SWR镜像加速: ${HUAWEI_SWR_MIRROR}"
  fi
  echo "[Docker镜像加速] 当前 mirrors: ${DOCKER_REGISTRY_MIRRORS}"
}

configure_huawei_apt_mirror() {
  if [ "${ENABLE_HUAWEI_APT_MIRROR}" != "true" ]; then
    echo "[APT镜像源] 跳过（ENABLE_HUAWEI_APT_MIRROR=${ENABLE_HUAWEI_APT_MIRROR}）"
    return 0
  fi
  if ! command -v apt-get >/dev/null 2>&1; then
    return 0
  fi
  if [ ! -f /etc/os-release ]; then
    return 0
  fi
  # shellcheck disable=SC1091
  . /etc/os-release
  if [ "${ID:-}" != "ubuntu" ]; then
    return 0
  fi
  if [ -z "${VERSION_CODENAME:-}" ]; then
    return 0
  fi
  local mirror="${HUAWEI_APT_MIRROR%/}"
  local codename="${VERSION_CODENAME}"
  local backup="/etc/apt/sources.list.bak.before-huawei-mirror"
  if [ -f /etc/apt/sources.list ] && [ ! -f "${backup}" ]; then
    cp /etc/apt/sources.list "${backup}"
  fi
  cat > /etc/apt/sources.list <<EOF
deb ${mirror}/ubuntu/ ${codename} main restricted universe multiverse
deb ${mirror}/ubuntu/ ${codename}-updates main restricted universe multiverse
deb ${mirror}/ubuntu/ ${codename}-backports main restricted universe multiverse
deb ${mirror}/ubuntu/ ${codename}-security main restricted universe multiverse
EOF
  echo "[APT镜像源] 已切换到: ${mirror}/ubuntu/ (${codename})"
}

if ! docker compose version >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    apt-get install -y docker-compose-plugin
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y docker-compose-plugin || true
  elif command -v yum >/dev/null 2>&1; then
    yum install -y docker-compose-plugin || true
  fi
fi

if [ -n "${SUDO_USER:-}" ] && [ "${SUDO_USER}" != "root" ]; then
  usermod -aG docker "${SUDO_USER}" || true
fi

echo "[2/4] 安装基础工具..."
configure_huawei_apt_mirror
if command -v apt-get >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y curl git tar python3
elif command -v dnf >/dev/null 2>&1; then
  dnf install -y curl git tar python3
elif command -v yum >/dev/null 2>&1; then
  yum install -y curl git tar python3
fi

configure_docker_mirror

echo "[3/4] 开放防火墙端口(若启用防火墙)..."
if command -v ufw >/dev/null 2>&1; then
  ufw allow 22/tcp || true
  ufw allow 80/tcp || true
  ufw allow 443/tcp || true
  ufw allow "${APP_PORT}/tcp" || true
  ufw allow "${DB_PORT}/tcp" || true
fi

if command -v firewall-cmd >/dev/null 2>&1 && systemctl is-active firewalld >/dev/null 2>&1; then
  firewall-cmd --permanent --add-port="80/tcp" || true
  firewall-cmd --permanent --add-port="443/tcp" || true
  firewall-cmd --permanent --add-port="${APP_PORT}/tcp" || true
  firewall-cmd --permanent --add-port="${DB_PORT}/tcp" || true
  firewall-cmd --reload || true
fi

echo "[4/4] 检查完成"
docker --version
docker compose version || true
echo "服务器初始化完成。请重新登录以生效 docker 用户组。"
