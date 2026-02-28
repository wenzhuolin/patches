#!/usr/bin/env bash
set -euo pipefail

ALERTMANAGER_URL="${1:-http://127.0.0.1:9093}"

NOW="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
END="$(date -u -d '+10 minutes' +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v+10M +"%Y-%m-%dT%H:%M:%SZ")"

PAYLOAD="$(cat <<JSON
[
  {
    "labels": {
      "alertname": "PatchManualTestAlert",
      "severity": "warning",
      "service": "patch-lifecycle"
    },
    "annotations": {
      "summary": "Manual test alert",
      "description": "This alert is injected by scripts/huawei/test-alerting.sh"
    },
    "startsAt": "${NOW}",
    "endsAt": "${END}"
  }
]
JSON
)"

echo "[1/2] 发送测试告警到 Alertmanager..."
curl -fsS -XPOST "${ALERTMANAGER_URL%/}/api/v2/alerts" \
  -H "Content-Type: application/json" \
  -d "${PAYLOAD}" >/dev/null
echo "发送成功"

echo "[2/2] 查询当前告警..."
curl -fsS "${ALERTMANAGER_URL%/}/api/v2/alerts" | tr '\n' ' ' | cut -c1-800
echo
echo "测试完成"
