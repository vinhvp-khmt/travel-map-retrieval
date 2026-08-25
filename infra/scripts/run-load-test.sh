#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
REQUESTS="${REQUESTS:-100}"
CONCURRENCY="${CONCURRENCY:-10}"
RESULT_DIR="$ROOT_DIR/infra/k6/results"
TMP_RESULT="$(mktemp)"
mkdir -p "$RESULT_DIR"
trap 'rm -f "$TMP_RESULT"' EXIT

ab -n "$REQUESTS" -c "$CONCURRENCY" \
  "$BASE_URL/api/v1/search?q=cafe&latitude=10.7769&longitude=106.7009&radiusKm=2" > "$TMP_RESULT"

requests_per_second="$(awk -F: '/Requests per second/{gsub(/^[ \t]+/, "", $2); print $2+0}' "$TMP_RESULT")"
p95_ms="$(awk '$1==95"%"{print $2}' "$TMP_RESULT")"
failed="$(awk -F: '/Failed requests/{gsub(/[ \t]/, "", $2); print $2+0}' "$TMP_RESULT")"
jq -n --arg tool "ApacheBench" --argjson requests "$REQUESTS" --argjson concurrency "$CONCURRENCY" \
  --argjson requestsPerSecond "$requests_per_second" --argjson p95Ms "$p95_ms" --argjson failedRequests "$failed" \
  '{tool:$tool,requests:$requests,concurrency:$concurrency,requestsPerSecond:$requestsPerSecond,p95Ms:$p95Ms,failedRequests:$failedRequests,passed:($failedRequests==0 and $p95Ms<500)}' \
  > "$RESULT_DIR/search-summary.json"
cat "$RESULT_DIR/search-summary.json"
