#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
OUT="$ROOT_DIR/.travelmap/artifacts/smoke-test.md"
health_status="$(curl -sS -o /tmp/travelmap-health.json -w '%{http_code}' "$BASE_URL/api/v1/health")"
search_status="$(curl -sS -o /tmp/travelmap-search.json -w '%{http_code}' "$BASE_URL/api/v1/search?q=cafe&latitude=10.7769&longitude=106.7009&radiusKm=2")"
owner_status="$(curl -sS -o /tmp/travelmap-owner.json -w '%{http_code}' "$BASE_URL/api/v1/owner/reports/overview")"
test "$health_status" = 200 && test "$search_status" = 200 && test "$owner_status" = 403
{
  echo '# TravelMap smoke test'
  echo
  echo "- Run at: $(date -u +%FT%TZ)"
  echo "- Health endpoint: HTTP $health_status"
  echo "- Public search endpoint: HTTP $search_status"
  echo "- Protected Owner report without JWT: HTTP $owner_status"
  echo '- Result: PASS'
} > "$OUT"
printf 'Smoke test: PASS\n'
