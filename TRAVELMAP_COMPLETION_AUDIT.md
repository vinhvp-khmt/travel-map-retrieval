# TravelMap — Completion Audit

Audit date: 2026-08-24

## Meaning of the pipeline result

`run_pipeline.sh` reports all stages as passed because its local implementation
gates are satisfied: required source files/artifacts exist and the selected
backend/frontend tests pass. It does not prove that cloud deployment, all UI
flows, production operations, or every item in the design plan is complete.

## Completed and verified locally

| Area | Result | Evidence |
|---|---|---|
| Local database | Complete | PostgreSQL 16/PostGIS container is healthy on port `55432` |
| Database schema | Complete | Flyway migrations `V1`–`V7` applied |
| Backend architecture | Complete | Controller / Service / Repository separation |
| Backend features | Implemented | Auth/RBAC, POI approval, search/ranking, booking, mock payment, review, reports |
| Backend validation | Implemented | DTO, service and database constraints |
| API contract | Complete as static YAML | `docs/openapi.yaml` parses successfully |
| Backend unit/API tests | Passing | 44/44 |
| Search/map frontend | Implemented | React search form, map/list, POI detail, login gate, GPS-required search, Geoapify tile/geocoding/places |
| Frontend tests | Passing | 17/17 |
| Local migration/start | Verified | API starts against PostGIS and health endpoint returns 200 |
| Local demo seed | Complete | Idempotent seed: 4 accounts, 20 POIs, 12 bookings/payments/reviews |
| Smoke/load baseline | Passing | 100 requests, concurrency 10, p95 122 ms, 0 failures |
| Deployment definition | Prepared only | Backend/frontend Dockerfiles and `render.yaml` exist |

## Not completed

| Area | Current state / required work |
|---|---|
| Supabase/cloud database | No project created; no credentials; no cloud migration run |
| Cloud deployment | No Git remote or hosting connection; no public frontend/API URL |
| Production seed/bootstrap | Local demo Admin exists; no production-safe Admin bootstrap configured |
| Full frontend product | Login/Register gate, search/map/detail and GPS action are implemented; Owner/Admin, booking/payment, review and reporting screens are absent |
| Online map tiles | Geoapify Carto tile URL is configured through `VITE_MAP_TILE_URL`; local browser verified 12/12 tiles loaded |
| Places/category search | Geoapify Places API is implemented for common categories; every Geoapify result is client-filtered to the selected radius; local browser verified `cafe` near Hanoi within 2 km returned 20 nearby places, max 540 m |
| Image storage/upload | Review supports HTTP(S) image URLs only; no Supabase Storage/S3 upload |
| Swagger UI | Not installed; only static OpenAPI YAML exists |
| Actuator/metrics | Custom health endpoint exists; Spring Actuator/metrics dashboard does not |
| Real browser E2E | Current frontend E2E is Vitest/jsdom with mocked API, not Playwright against the live stack |
| Repository integration tests | No Testcontainers/PostGIS integration suite |
| Security hardening | No rate limiter, Trivy/Gitleaks scan, WAF or external penetration test |
| Operations | No live monitoring, budget alert or verified cloud backup/restore exercise |
| Load testing | ApacheBench baseline exists; `k6` itself is not installed/run |

## Database decision

Local development uses the Docker PostGIS database in `infra/docker-compose.yml`.
Supabase is only the recommended cloud option. Supabase is hosted PostgreSQL, so
the Spring Boot API can use its PostgreSQL JDBC connection and run the same
Flyway SQL migrations. `SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_ROLE_KEY` are
not needed unless the application later uses Supabase Auth, REST, or Storage.

## Inputs required for a cloud demo

- A Git repository remote and permission to push.
- A Supabase (or Neon/PostGIS) project and database credentials.
- A Render/Cloud Run account and permission to create services.
- Production frontend origin for CORS.
- Strong JWT/payment secrets and an initial Admin account decision.
- If images are uploaded, a storage provider/bucket.
