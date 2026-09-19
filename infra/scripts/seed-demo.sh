#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/docker-compose.yml"
SEED_FILE="$ROOT_DIR/infra/seed/demo_seed.sql"
IR_SEED_FILE="$ROOT_DIR/infra/seed/ir_dataset_seed.sql"

docker compose -f "$COMPOSE_FILE" up -d postgres
until docker compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U travelmap -d travelmap >/dev/null 2>&1; do
  sleep 1
done

schema_version="$(docker compose -f "$COMPOSE_FILE" exec -T postgres \
  psql -U travelmap -d travelmap -Atc "SELECT COALESCE(MAX(installed_rank),0) FROM flyway_schema_history WHERE success")"
if [ "$schema_version" -lt 7 ]; then
  printf 'Database schema is not at V7. Start the API once so Flyway can migrate it, then rerun this script.\n' >&2
  exit 1
fi

docker compose -f "$COMPOSE_FILE" exec -T postgres psql -v ON_ERROR_STOP=1 -U travelmap -d travelmap < "$SEED_FILE"
# IR dataset seed (Phase 3): thêm POI thiết kế cho đánh giá search engine nội bộ.
# Độc lập với demo_seed.sql (không đụng app_user/booking/payment/review) nên áp dụng
# được nhiều lần, không phá dữ liệu demo phía trên.
docker compose -f "$COMPOSE_FILE" exec -T postgres psql -v ON_ERROR_STOP=1 -U travelmap -d travelmap < "$IR_SEED_FILE"
docker compose -f "$COMPOSE_FILE" exec -T postgres psql -U travelmap -d travelmap -P pager=off -c "
  SELECT (SELECT COUNT(*) FROM app_user WHERE email LIKE '%@travelmap.local') AS demo_users,
         (SELECT COUNT(*) FROM poi WHERE status='ACTIVE') AS active_pois,
         (SELECT COUNT(*) FROM booking WHERE notes LIKE 'Demo booking%') AS demo_bookings,
         (SELECT COUNT(*) FROM review WHERE comment IN ('Trải nghiệm rất tốt, sẽ quay lại.','Vị trí thuận tiện và phục vụ thân thiện.','Không gian đẹp, phù hợp chuyến đi cuối tuần.')) AS demo_reviews;"
