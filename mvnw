#!/usr/bin/env bash
set -euo pipefail

MVN_VERSION="3.9.9"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
CACHE_DIR="${BASE_DIR}/.maven"
MVN_DIR="${CACHE_DIR}/apache-maven-${MVN_VERSION}"
MVN_BIN="${MVN_DIR}/bin/mvn"

if [ ! -x "${MVN_BIN}" ]; then
  mkdir -p "${CACHE_DIR}"
  ARCHIVE="${CACHE_DIR}/apache-maven-${MVN_VERSION}-bin.tar.gz"
  if [ ! -f "${ARCHIVE}" ]; then
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.tar.gz" -o "${ARCHIVE}"
  fi
  tar -xzf "${ARCHIVE}" -C "${CACHE_DIR}"
fi

exec "${MVN_BIN}" "$@"
