#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIPELINE_DIR="$ROOT_DIR/.travelmap"
CONFIG_FILE="$PIPELINE_DIR/pipeline.env"
STATE_FILE="$PIPELINE_DIR/pipeline-state.tsv"
LOG_DIR="$PIPELINE_DIR/logs"
ARTIFACT_DIR="$PIPELINE_DIR/artifacts"
PROGRESS_FILE="$ROOT_DIR/TRAVELMAP_PIPELINE_PROGRESS.md"
LOCK_DIR="$PIPELINE_DIR/pipeline.lock"

STAGES="0 1 2 3 4 5 6 7 8"

stage_name() {
  case "$1" in
    0) printf '%s' "Requirements baseline" ;;
    1) printf '%s' "Foundation" ;;
    2) printf '%s' "Database, Auth & RBAC" ;;
    3) printf '%s' "POI Management & Approval" ;;
    4) printf '%s' "Search IR Core" ;;
    5) printf '%s' "React Map UI" ;;
    6) printf '%s' "Booking & Payment" ;;
    7) printf '%s' "Review & Reporting" ;;
    8) printf '%s' "Hardening, QA & Deployment" ;;
    *) printf '%s' "Unknown" ;;
  esac
}

now() {
  date '+%Y-%m-%d %H:%M:%S %z'
}

sanitize_note() {
  printf '%s' "$1" | tr '\t\r\n|' '    '
}

item_catalog() {
  cat <<'EOF'
0|0.1|System design document exists
0|0.2|Baseline configuration is complete
0|0.3|Ranking weights are valid
0|0.4|Requirements snapshot is generated
1|1.1|Required local runtimes are available
1|1.2|Frontend scaffold exists
1|1.3|Backend scaffold exists
1|1.4|PostGIS Docker Compose exists
1|1.5|CI workflow and README exist
1|1.6|Frontend build passes
1|1.7|Backend test suite passes
2|2.1|User and token migrations exist
2|2.2|Authentication controllers/services/repositories exist
2|2.3|RBAC configuration exists
2|2.4|Auth unit and API tests pass
3|3.1|POI and opening-hours migrations exist
3|3.2|POI Controller/Service/Repository exist
3|3.3|PostGIS duplicate and service-area validation exist
3|3.4|Approval workflow and audit exist
3|3.5|POI tests pass
4|4.1|Query normalization and tokenizer exist
4|4.2|Inverted index and BM25 scorer exist
4|4.3|Spatial and temporal retrieval exist
4|4.4|Ranking and scoreDetail exist
4|4.5|Search API tests pass
4|4.6|IR evaluation report exists
5|5.1|Search form and validation exist
5|5.2|Map, marker cluster and list synchronization exist
5|5.3|POI details and score breakdown exist
5|5.4|Frontend component tests pass
5|5.5|Search E2E test passes
6|6.1|Booking schema and state machine exist
6|6.2|Transactional availability and hold exist
6|6.3|Payment adapter and webhook validation exist
6|6.4|Booking and payment tests pass
7|7.1|Review schema and eligibility rules exist
7|7.2|Rating aggregate and re-index exist
7|7.3|Owner and Admin reports exist
7|7.4|Review/report tests pass
8|8.1|Full backend and frontend tests pass
8|8.2|Load-test report exists
8|8.3|Security and secret checks pass
8|8.4|Cloud deployment configuration exists
8|8.5|Smoke-test report and runbook exist
EOF
}

ensure_dirs() {
  mkdir -p "$PIPELINE_DIR" "$LOG_DIR" "$ARTIFACT_DIR"
}

ensure_config() {
  if [ ! -f "$CONFIG_FILE" ]; then
    cp "$ROOT_DIR/pipeline.env.example" "$CONFIG_FILE"
  fi
}

ensure_state() {
  if [ ! -f "$STATE_FILE" ]; then
    item_catalog | while IFS='|' read -r stage item description; do
      printf '%s\t%s\tPENDING\t%s\t%s\n' "$stage" "$item" "$(now)" "Not started"
    done > "$STATE_FILE"
    return
  fi

  item_catalog | while IFS='|' read -r stage item description; do
    if ! awk -F '\t' -v wanted="$item" '$2 == wanted { found=1 } END { exit(found ? 0 : 1) }' "$STATE_FILE"; then
      printf '%s\t%s\tPENDING\t%s\t%s\n' "$stage" "$item" "$(now)" "Added by pipeline update" >> "$STATE_FILE"
    fi
  done
}

load_config() {
  # This file is user-controlled and must contain only KEY=VALUE assignments.
  # shellcheck disable=SC1090
  . "$CONFIG_FILE"
}

node_is_compatible() {
  candidate="$1"
  version="$($candidate --version 2>/dev/null | sed 's/^v//')"
  major="$(printf '%s' "$version" | awk -F. '{print $1}')"
  minor="$(printf '%s' "$version" | awk -F. '{print $2}')"
  [ -n "$major" ] || return 1
  if [ "$major" -gt 22 ]; then
    return 0
  fi
  if [ "$major" -eq 22 ] && [ "$minor" -ge 12 ]; then
    return 0
  fi
  if [ "$major" -eq 20 ] && [ "$minor" -ge 19 ]; then
    return 0
  fi
  return 1
}

resolve_node_bin_dir() {
  if command -v node >/dev/null 2>&1 && node_is_compatible "$(command -v node)"; then
    dirname "$(command -v node)"
    return 0
  fi

  for candidate in $(find "$HOME/.nvm/versions/node" -path '*/bin/node' -type f 2>/dev/null | sort -r); do
    if node_is_compatible "$candidate"; then
      dirname "$candidate"
      return 0
    fi
  done
  return 1
}

node_bin_dir() {
  resolve_node_bin_dir
}

item_status() {
  awk -F '\t' -v wanted="$1" '$2 == wanted { print $3; exit }' "$STATE_FILE"
}

stage_status() {
  stage="$1"
  statuses="$(awk -F '\t' -v wanted="$stage" '$1 == wanted { print $3 }' "$STATE_FILE")"
  if printf '%s\n' "$statuses" | grep -q '^FAILED$'; then
    printf '%s' "FAILED"
  elif printf '%s\n' "$statuses" | grep -q '^BLOCKED$'; then
    printf '%s' "BLOCKED"
  elif printf '%s\n' "$statuses" | grep -q '^RUNNING$'; then
    printf '%s' "RUNNING"
  elif [ -n "$statuses" ] && printf '%s\n' "$statuses" | grep -qv '^PASSED$'; then
    printf '%s' "PENDING"
  else
    printf '%s' "PASSED"
  fi
}

mark_item() {
  item="$1"
  status="$2"
  note="$(sanitize_note "$3")"
  tmp_file="$STATE_FILE.tmp.$$"
  awk -F '\t' -v OFS='\t' -v wanted="$item" -v new_status="$status" -v updated="$(now)" -v note="$note" '
    $2 == wanted { $3=new_status; $4=updated; $5=note }
    { print }
  ' "$STATE_FILE" > "$tmp_file"
  mv "$tmp_file" "$STATE_FILE"
  generate_progress
}

status_icon() {
  case "$1" in
    PASSED) printf '%s' "✅" ;;
    RUNNING) printf '%s' "🔄" ;;
    BLOCKED) printf '%s' "⛔" ;;
    FAILED) printf '%s' "❌" ;;
    *) printf '%s' "⬜" ;;
  esac
}

checkbox() {
  case "$1" in
    PASSED) printf '%s' "[x]" ;;
    *) printf '%s' "[ ]" ;;
  esac
}

generate_progress() {
  tmp_file="$PROGRESS_FILE.tmp.$$"
  {
    printf '# TravelMap — Pipeline Progress\n\n'
    printf '> Auto-generated by `run_pipeline.sh`. Do not edit checklist statuses manually.  \n'
    printf '> Last updated: %s\n\n' "$(now)"
    printf '## Commands\n\n'
    printf '```bash\n'
    printf './run_pipeline.sh status\n'
    printf './run_pipeline.sh next\n'
    printf './run_pipeline.sh stage <0-8>\n'
    printf './run_pipeline.sh all\n'
    printf './run_pipeline.sh reset <0-8>\n'
    printf '```\n\n'
    printf '## Stage summary\n\n'
    printf '| Stage | Name | Status |\n'
    printf '|---:|---|---|\n'
    for stage in $STAGES; do
      current="$(stage_status "$stage")"
      printf '| %s | %s | %s %s |\n' "$stage" "$(stage_name "$stage")" "$(status_icon "$current")" "$current"
    done
    printf '\n'

    for stage in $STAGES; do
      printf '## Stage %s — %s\n\n' "$stage" "$(stage_name "$stage")"
      item_catalog | awk -F '|' -v wanted="$stage" '$1 == wanted { print $2 "|" $3 }' | while IFS='|' read -r item description; do
        row="$(awk -F '\t' -v wanted="$item" '$2 == wanted { print $3 "|" $4 "|" $5; exit }' "$STATE_FILE")"
        status="${row%%|*}"
        rest="${row#*|}"
        updated="${rest%%|*}"
        note="${rest#*|}"
        printf -- '- %s `%s` %s — **%s**' "$(checkbox "$status")" "$item" "$description" "$status"
        if [ -n "$note" ]; then
          printf ' — %s' "$note"
        fi
        printf ' _(updated %s)_\n' "$updated"
      done
      printf '\n'
    done
  } > "$tmp_file"
  mv "$tmp_file" "$PROGRESS_FILE"
}

run_check() {
  item="$1"
  mode="$2"
  success_note="$3"
  failure_note="$4"
  shift 4

  mark_item "$item" "RUNNING" "Running validation"
  if "$@"; then
    mark_item "$item" "PASSED" "$success_note"
    return 0
  fi

  mark_item "$item" "$mode" "$failure_note"
  STAGE_HAS_ISSUES=1
  return 0
}

begin_stage() {
  STAGE_HAS_ISSUES=0
}

finish_stage() {
  [ "$STAGE_HAS_ISSUES" -eq 0 ]
}

files_exist() {
  for target in "$@"; do
    if [ ! -e "$ROOT_DIR/$target" ]; then
      return 1
    fi
  done
}

files_contain() {
  pattern="$1"
  shift
  for target in "$@"; do
    if [ -f "$ROOT_DIR/$target" ] && grep -Eq "$pattern" "$ROOT_DIR/$target"; then
      return 0
    fi
  done
  return 1
}

find_java_sources() {
  pattern="$1"
  if [ ! -d "$ROOT_DIR/services/api/src" ]; then
    return 1
  fi
  find "$ROOT_DIR/services/api/src" -type f \( -name '*.java' -o -name '*.kt' \) -print 2>/dev/null | grep -E "$pattern" >/dev/null 2>&1
}

find_web_sources() {
  pattern="$1"
  if [ ! -d "$ROOT_DIR/apps/web/src" ]; then
    return 1
  fi
  find "$ROOT_DIR/apps/web/src" -type f \( -name '*.ts' -o -name '*.tsx' \) -print 2>/dev/null | grep -E "$pattern" >/dev/null 2>&1
}

find_backend_tests() {
  pattern="$1"
  if [ ! -d "$ROOT_DIR/services/api/src/test" ]; then
    return 1
  fi
  find "$ROOT_DIR/services/api/src/test" -type f \( -name '*Test.java' -o -name '*Tests.java' -o -name '*Test.kt' \) -print 2>/dev/null | grep -Ei "$pattern" >/dev/null 2>&1
}

backend_feature_test() {
  pattern="$1"
  find_backend_tests "$pattern" && backend_test
}

runtime_check() {
  resolved_node_dir="$(node_bin_dir)" || return 1
  command -v java >/dev/null 2>&1 &&
    command -v javac >/dev/null 2>&1 &&
    java -version 2>&1 | grep -Eq 'version "21([.]|\")' &&
    test -x "$resolved_node_dir/node" &&
    test -x "$resolved_node_dir/npm" &&
    command -v docker >/dev/null 2>&1 &&
    command -v git >/dev/null 2>&1
}

resolve_maven() {
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return 0
  fi

  for candidate in $(find "$HOME/.m2/wrapper/dists" -path '*/bin/mvn' -type f 2>/dev/null | sort -r); do
    if [ -x "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

write_tool_inventory() {
  inventory="$ARTIFACT_DIR/01-tool-inventory.md"
  resolved_node_dir="$(node_bin_dir 2>/dev/null || true)"
  resolved_maven="$(resolve_maven 2>/dev/null || true)"
  {
    printf '# TravelMap — Local Tool Inventory\n\n'
    printf 'Generated: %s\n\n' "$(now)"
    printf '| Tool | Local status | Version / location |\n'
    printf '|---|---|---|\n'
    for tool_name in java javac docker psql pg_isready git curl; do
      tool_path="$(command -v "$tool_name" 2>/dev/null || true)"
      if [ -n "$tool_path" ]; then
        tool_version="$("$tool_path" --version 2>&1 | head -n 1)"
        printf '| %s | Available | `%s` — %s |\n' "$tool_name" "$tool_path" "$tool_version"
      else
        printf '| %s | Missing | — |\n' "$tool_name"
      fi
    done
    if [ -n "$resolved_node_dir" ]; then
      printf '| node | Available | `%s/node` — %s |\n' "$resolved_node_dir" "$("$resolved_node_dir/node" --version)"
      printf '| npm | Available | `%s/npm` — %s |\n' "$resolved_node_dir" "$(PATH="$resolved_node_dir:$PATH" "$resolved_node_dir/npm" --version)"
    else
      printf '| node/npm | Missing compatible version | — |\n'
    fi
    if [ -n "$resolved_maven" ]; then
      printf '| Maven | Available in wrapper cache | `%s` — %s |\n' "$resolved_maven" "$("$resolved_maven" --version 2>&1 | head -n 1)"
    else
      printf '| Maven | Missing | — |\n'
    fi
    printf '\nDocker CLI availability does not imply that the Docker daemon is reachable from the sandbox.\n'
  } > "$inventory"
  test -s "$inventory"
}

config_complete() {
  required_values="$PROJECT_NAME|$SCOPE|$SERVICE_AREA|$ARCHITECTURE|$FRONTEND_STACK|$BACKEND_STACK|$DATABASE_LOCAL|$DATABASE_CLOUD|$PAYMENT_MODE|$GUEST_SEARCH"
  ! printf '%s' "$required_values" | grep -Eq '(^|\|)[[:space:]]*(TBD|CHANGE_ME)?[[:space:]]*(\||$)'
}

weights_valid() {
  awk -v a="$RANKING_W_BM25" -v b="$RANKING_W_SPATIAL" -v c="$RANKING_W_TEMPORAL" -v d="$RANKING_W_RATING" 'BEGIN { sum=a+b+c+d; exit !(sum > 0.9999 && sum < 1.0001) }'
}

write_requirements_snapshot() {
  snapshot="$ARTIFACT_DIR/00-requirements-baseline.md"
  {
    printf '# TravelMap — Requirements Baseline\n\n'
    printf 'Generated: %s\n\n' "$(now)"
    printf '| Decision | Value |\n|---|---|\n'
    printf '| Project | %s |\n' "$PROJECT_NAME"
    printf '| Scope | %s |\n' "$SCOPE"
    printf '| Service area | %s |\n' "$SERVICE_AREA"
    printf '| Architecture | %s |\n' "$ARCHITECTURE"
    printf '| Frontend | %s |\n' "$FRONTEND_STACK"
    printf '| Backend | %s |\n' "$BACKEND_STACK"
    printf '| Local database | %s |\n' "$DATABASE_LOCAL"
    printf '| Cloud database | %s |\n' "$DATABASE_CLOUD"
    printf '| Guest search | %s |\n' "$GUEST_SEARCH"
    printf '| Payment mode | %s |\n' "$PAYMENT_MODE"
    printf '| Ranking | BM25=%s, spatial=%s, temporal=%s, rating=%s |\n' "$RANKING_W_BM25" "$RANKING_W_SPATIAL" "$RANKING_W_TEMPORAL" "$RANKING_W_RATING"
  } > "$snapshot"
  test -s "$snapshot"
}

frontend_build() {
  if [ ! -f "$ROOT_DIR/apps/web/package.json" ]; then
    return 1
  fi
  if [ ! -d "$ROOT_DIR/apps/web/node_modules" ]; then
    return 1
  fi
  resolved_node_dir="$(node_bin_dir)" || return 1
  (cd "$ROOT_DIR/apps/web" && PATH="$resolved_node_dir:$PATH" npm run build)
}

backend_test() {
  if [ -x "$ROOT_DIR/services/api/mvnw" ]; then
    (cd "$ROOT_DIR/services/api" && ./mvnw test)
  elif command -v mvn >/dev/null 2>&1; then
    (cd "$ROOT_DIR/services/api" && mvn test)
  elif resolved_maven="$(resolve_maven)"; then
    (cd "$ROOT_DIR/services/api" && "$resolved_maven" --offline test)
  elif [ -x "$ROOT_DIR/services/api/gradlew" ]; then
    (cd "$ROOT_DIR/services/api" && ./gradlew test)
  else
    return 1
  fi
}

frontend_test() {
  resolved_node_dir="$(node_bin_dir)" || return 1
  [ -d "$ROOT_DIR/apps/web/node_modules" ] && (cd "$ROOT_DIR/apps/web" && PATH="$resolved_node_dir:$PATH" npm test -- --run)
}

run_stage_0() {
  begin_stage
  run_check 0.1 FAILED "Design document found" "Missing TRAVELMAP_SYSTEM_IMPLEMENTATION_PLAN.md" files_exist TRAVELMAP_SYSTEM_IMPLEMENTATION_PLAN.md || return 1
  run_check 0.2 BLOCKED "Baseline config complete" "Complete .travelmap/pipeline.env" config_complete || return 1
  run_check 0.3 FAILED "Ranking weights sum to 1" "Ranking weights must sum to 1" weights_valid || return 1
  run_check 0.4 FAILED "Requirements snapshot generated" "Could not generate requirements snapshot" write_requirements_snapshot || return 1
  finish_stage
}

run_stage_1() {
  begin_stage
  run_check 1.1 BLOCKED "Java 21, Node, npm, Docker CLI and Git found" "Install/enable required runtimes" runtime_check || return 1
  run_check 1.2 BLOCKED "Frontend scaffold found" "Missing apps/web/package.json" files_exist apps/web/package.json || return 1
  run_check 1.3 BLOCKED "Backend scaffold found" "Missing services/api build definition" sh -c "test -f '$ROOT_DIR/services/api/pom.xml' || test -f '$ROOT_DIR/services/api/build.gradle' || test -f '$ROOT_DIR/services/api/build.gradle.kts'" || return 1
  run_check 1.4 BLOCKED "PostGIS Compose found" "Missing infra/docker-compose.yml" files_exist infra/docker-compose.yml || return 1
  run_check 1.5 BLOCKED "CI workflow and README found" "Missing README or CI workflow" files_exist README.md .github/workflows/ci.yml || return 1
  run_check 1.6 BLOCKED "Frontend build passed" "Run npm install, then fix frontend build" frontend_build || return 1
  run_check 1.7 BLOCKED "Backend tests passed" "Maven/Gradle wrapper or dependencies unavailable, or tests failed" backend_test || return 1
  finish_stage
}

run_stage_2() {
  begin_stage
  run_check 2.1 BLOCKED "Auth migrations found" "User/token migrations are not implemented" files_contain 'app_user|refresh_token' services/api/src/main/resources/db/migration/*.sql || return 1
  run_check 2.2 BLOCKED "Auth layers found" "Authentication Controller/Service/Repository are not implemented" find_java_sources 'auth/.+(Controller|Service|Repository)' || return 1
  run_check 2.3 BLOCKED "RBAC config found" "Security/RBAC config is not implemented" find_java_sources '(SecurityConfig|Authorization)' || return 1
  run_check 2.4 BLOCKED "Auth tests passed" "Auth tests are absent or failing" backend_feature_test '(auth|security|token)' || return 1
  finish_stage
}

run_stage_3() {
  begin_stage
  run_check 3.1 BLOCKED "POI migrations found" "POI/opening-hour migrations are not implemented" files_contain 'poi_opening_hour|create table poi' services/api/src/main/resources/db/migration/*.sql || return 1
  run_check 3.2 BLOCKED "POI layers found" "POI Controller/Service/Repository are not implemented" find_java_sources 'poi/.+(Controller|Service|Repository)' || return 1
  run_check 3.3 BLOCKED "Geo validation found" "PostGIS duplicate/service-area validation is not implemented" find_java_sources '(DuplicatePoi|ServiceArea|GeoValidation)' || return 1
  run_check 3.4 BLOCKED "Approval workflow found" "POI approval/audit is not implemented" find_java_sources '(PoiApproval|ApprovalHistory)' || return 1
  run_check 3.5 BLOCKED "POI tests passed" "POI tests are absent or failing" backend_feature_test '(poi|place|opening)' || return 1
  finish_stage
}

run_stage_4() {
  begin_stage
  run_check 4.1 BLOCKED "Query processor found" "Tokenizer/query normalization is not implemented" find_java_sources '(VietnameseTokenizer|QueryProcess|QueryNormal)' || return 1
  run_check 4.2 BLOCKED "BM25 index found" "Inverted index/BM25 scorer is not implemented" find_java_sources '(InvertedIndex|BM25)' || return 1
  run_check 4.3 BLOCKED "Spatial/temporal retrieval found" "Spatial/temporal retrieval is not implemented" find_java_sources '(Spatial|TemporalFit)' || return 1
  run_check 4.4 BLOCKED "Ranking and scoreDetail found" "Ranking/scoreDetail is not implemented" find_java_sources '(RankingService|ScoreDetail)' || return 1
  run_check 4.5 BLOCKED "Search tests passed" "Search tests are absent or failing" backend_feature_test '(search|bm25|ranking|spatial)' || return 1
  run_check 4.6 BLOCKED "IR report found" "Missing docs/qrels/ir-evaluation-report.md" files_exist docs/qrels/ir-evaluation-report.md || return 1
  finish_stage
}

run_stage_5() {
  begin_stage
  run_check 5.1 BLOCKED "Search form found" "Search form/validation is not implemented" find_web_sources '(SearchForm|search/schema)' || return 1
  run_check 5.2 BLOCKED "Map/list UI found" "Map markers/list synchronization is not implemented" find_web_sources '(MapView|MarkerCluster|SearchMap)' || return 1
  run_check 5.3 BLOCKED "POI detail found" "POI detail/score breakdown is not implemented" find_web_sources '(PoiDetail|ScoreBreakdown)' || return 1
  run_check 5.4 BLOCKED "Frontend tests passed" "Frontend component tests are absent or failing" frontend_test || return 1
  run_check 5.5 BLOCKED "Search E2E report found" "Missing apps/web/test-results/search-e2e.json" files_exist apps/web/test-results/search-e2e.json || return 1
  finish_stage
}

run_stage_6() {
  begin_stage
  run_check 6.1 BLOCKED "Booking state machine found" "Booking schema/state machine is not implemented" find_java_sources '(BookingStatus|BookingState)' || return 1
  run_check 6.2 BLOCKED "Availability/hold found" "Transactional availability/hold is not implemented" find_java_sources '(AvailabilityService|BookingHold)' || return 1
  run_check 6.3 BLOCKED "Payment integration found" "Payment adapter/webhook validation is not implemented" find_java_sources '(PaymentGateway|PaymentWebhook)' || return 1
  run_check 6.4 BLOCKED "Booking/payment tests passed" "Booking/payment tests are absent or failing" backend_feature_test '(booking|payment|availability)' || return 1
  finish_stage
}

run_stage_7() {
  begin_stage
  run_check 7.1 BLOCKED "Review rules found" "Review schema/eligibility is not implemented" find_java_sources '(ReviewEligibility|ReviewService)' || return 1
  run_check 7.2 BLOCKED "Rating re-index found" "Rating aggregate/re-index is not implemented" find_java_sources '(RatingAggregate|Reindex)' || return 1
  run_check 7.3 BLOCKED "Reports found" "Owner/Admin reports are not implemented" find_java_sources '(ReportController|ReportService)' || return 1
  run_check 7.4 BLOCKED "Review/report tests passed" "Review/report tests are absent or failing" backend_feature_test '(review|rating|report)' || return 1
  finish_stage
}

run_stage_8() {
  begin_stage
  run_check 8.1 BLOCKED "Full test suites passed" "Full frontend/backend tests are not ready" sh -c "'$ROOT_DIR/run_pipeline.sh' internal-full-test" || return 1
  run_check 8.2 BLOCKED "Load-test report found" "Missing infra/k6/results/search-summary.json" files_exist infra/k6/results/search-summary.json || return 1
  run_check 8.3 BLOCKED "Security checks passed" "Missing secret scan/security report" files_exist .travelmap/artifacts/security-check.md || return 1
  run_check 8.4 BLOCKED "Deployment config found" "Missing deployment configuration" sh -c "test -f '$ROOT_DIR/render.yaml' || test -f '$ROOT_DIR/infra/cloudrun/service.yaml'" || return 1
  run_check 8.5 BLOCKED "Smoke test and runbook found" "Missing smoke-test report or runbook" files_exist .travelmap/artifacts/smoke-test.md docs/runbook.md || return 1
  finish_stage
}

run_stage() {
  stage="$1"
  case "$stage" in
    0) run_stage_0 ;;
    1) run_stage_1 ;;
    2) run_stage_2 ;;
    3) run_stage_3 ;;
    4) run_stage_4 ;;
    5) run_stage_5 ;;
    6) run_stage_6 ;;
    7) run_stage_7 ;;
    8) run_stage_8 ;;
    *) printf 'Invalid stage: %s\n' "$stage" >&2; return 2 ;;
  esac
}

reset_stage() {
  stage="$1"
  case " $STAGES " in
    *" $stage "*) ;;
    *) printf 'Invalid stage: %s\n' "$stage" >&2; return 2 ;;
  esac
  item_catalog | awk -F '|' -v wanted="$stage" '$1 == wanted { print $2 }' | while IFS= read -r item; do
    mark_item "$item" "PENDING" "Reset by user"
  done
}

show_status() {
  printf '%-7s %-36s %s\n' "STAGE" "NAME" "STATUS"
  for stage in $STAGES; do
    printf '%-7s %-36s %s\n' "$stage" "$(stage_name "$stage")" "$(stage_status "$stage")"
  done
  printf '\nProgress file: %s\n' "$PROGRESS_FILE"
}

next_stage() {
  for stage in $STAGES; do
    if [ "$(stage_status "$stage")" != "PASSED" ]; then
      printf '%s' "$stage"
      return 0
    fi
  done
  return 1
}

acquire_lock() {
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    printf 'Another pipeline process is running or lock exists: %s\n' "$LOCK_DIR" >&2
    return 1
  fi
  trap 'rmdir "$LOCK_DIR" 2>/dev/null || true' EXIT INT TERM
}

usage() {
  cat <<'EOF'
Usage:
  ./run_pipeline.sh init
  ./run_pipeline.sh status
  ./run_pipeline.sh next
  ./run_pipeline.sh stage <0-8>
  ./run_pipeline.sh all
  ./run_pipeline.sh reset <0-8>
  ./run_pipeline.sh doctor
EOF
}

main() {
  ensure_dirs
  ensure_config
  ensure_state
  load_config
  generate_progress

  command="${1:-next}"
  case "$command" in
    init)
      generate_progress
      show_status
      ;;
    status)
      show_status
      ;;
    doctor)
      write_tool_inventory
      if runtime_check; then
        printf 'Runtime check: PASS\nInventory: %s\n' "$ARTIFACT_DIR/01-tool-inventory.md"
      else
        printf 'Runtime check: FAIL\n' >&2
        return 1
      fi
      ;;
    next)
      acquire_lock || return 1
      stage="$(next_stage)" || { printf 'All stages passed.\n'; return 0; }
      printf 'Running stage %s — %s\n' "$stage" "$(stage_name "$stage")"
      run_stage "$stage"
      ;;
    stage)
      [ "$#" -eq 2 ] || { usage; return 2; }
      acquire_lock || return 1
      run_stage "$2"
      ;;
    all)
      acquire_lock || return 1
      for stage in $STAGES; do
        if [ "$(stage_status "$stage")" = "PASSED" ]; then
          continue
        fi
        printf 'Running stage %s — %s\n' "$stage" "$(stage_name "$stage")"
        if ! run_stage "$stage"; then
          printf 'Pipeline stopped at stage %s with status %s.\n' "$stage" "$(stage_status "$stage")" >&2
          return 1
        fi
      done
      ;;
    reset)
      [ "$#" -eq 2 ] || { usage; return 2; }
      acquire_lock || return 1
      reset_stage "$2"
      ;;
    internal-full-test)
      backend_test && frontend_test
      ;;
    *)
      usage
      return 2
      ;;
  esac
}

main "$@"
