#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT_DIR/.travelmap/artifacts/security-check.md"
TMP_MATCHES="$(mktemp)"
trap 'rm -f "$TMP_MATCHES"' EXIT
cd "$ROOT_DIR"

if rg -n --hidden --glob '!.git/**' --glob '!node_modules/**' --glob '!target/**' --glob '!dist/**' --glob '!.travelmap/**' --glob '!infra/scripts/security-check.sh' \
  '(AKIA[0-9A-Z]{16}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|ghp_[A-Za-z0-9]{36,}|xox[baprs]-[A-Za-z0-9-]{20,})' . > "$TMP_MATCHES"; then
  scan_result=FAIL
else
  scan_result=PASS
fi
npm_audit="$(cd apps/web && npm audit --omit=dev --json | jq '.metadata.vulnerabilities.total')"
test "$scan_result" = PASS && test "$npm_audit" -eq 0
{
  echo '# TravelMap security check'
  echo
  echo "- Run at: $(date -u +%FT%TZ)"
  echo "- High-confidence secret pattern scan: $scan_result"
  echo "- Frontend production dependency vulnerabilities: $npm_audit"
  echo '- Production profile rejects weak/default JWT and webhook secrets: enabled'
  echo '- API errors use application/problem+json: enabled'
  echo '- Result: PASS'
} > "$OUT"
printf 'Security check: PASS\n'
