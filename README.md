# TravelMap

## Node.js

The repository pins the already-supported NVM runtime in `.nvmrc`:

```bash
nvm use
```

TravelMap is a modular-monolith travel discovery platform built with React,
Spring Boot and PostgreSQL/PostGIS. The implementation is driven by the staged
pipeline documented in `TRAVELMAP_SYSTEM_IMPLEMENTATION_PLAN.md`.

## Local prerequisites

- Java/Javac 25
- Node.js 24 and npm
- Maven 3.9+
- Docker with Compose

Check what is already installed before downloading anything:

```bash
./run_pipeline.sh doctor
```

Run the next incomplete stage and inspect the generated checklist:

```bash
./run_pipeline.sh next
./run_pipeline.sh status
```

The detailed status is written to `TRAVELMAP_PIPELINE_PROGRESS.md`. Local
pipeline configuration and logs live under `.travelmap/`.

The versioned API contract is available at `docs/openapi.yaml`.

## Start locally

The local application uses PostgreSQL 16 + PostGIS in Docker. Supabase is an
optional cloud target only and is not required for local development.

Start the database from the repository root:

```bash
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
```

Start the API in a second terminal:

```bash
cd services/api
./mvnw spring-boot:run
```

Flyway automatically applies `V1` through `V9`. Verify the API and database:

```bash
curl http://localhost:8080/api/v1/health
```

Seed idempotent local demo data (plus the IR dataset) after the schema reaches V9:

```bash
./infra/scripts/seed-demo.sh
```

Demo accounts all use password `Demo1234!`:

```text
admin@travelmap.local   ADMIN
owner1@travelmap.local  OWNER
owner2@travelmap.local  OWNER
user@travelmap.local    USER
```

Start React in a third terminal. The dependencies are already installed in the
current workspace; only run `npm install` when `node_modules` is absent.

```bash
source "$HOME/.nvm/nvm.sh"
nvm use
cd apps/web
npm run dev
```

Open <http://localhost:5173>. The API contract is stored at
`docs/openapi.yaml`; Swagger UI is not installed yet.

The frontend now requires login before the search homepage is shown. Use one of
the demo accounts above, then search for terms like `cafe` or `museum`.
Click `Dùng vị trí của tôi` to let the browser request real GPS/location
permission and fill latitude/longitude. Search is blocked until the browser
gets GPS permission or the user enters real coordinates manually; the app no
longer falls back to a demo District 1 coordinate.

The map tile URL is configured through `VITE_MAP_TILE_URL`. Local development
uses `apps/web/.env.local`; `apps/web/.env.example` shows the variables needed
for another machine or deployment. The current local setup uses Geoapify Carto
tiles, and TravelMap still displays a local road overlay if the online tile
provider is unavailable.

Geoapify search is configured through `VITE_GEOAPIFY_API_KEY`. The map/search UI
uses Geoapify geocoding and Places API results so demo seed POI titles do not
appear in the user-facing map search. Try queries like `supermarket`, `cafe`,
`restaurant`, `hotel`, `museum`, `park`, or a full address after setting a
location.

Local database connection:

```text
host: localhost
port: 55432
database: travelmap
username: travelmap
password: travelmap_local
```

Stop the API and web with `Ctrl+C`, then stop the database without deleting its
volume:

```bash
docker compose -f infra/docker-compose.yml stop
```

Do not run `docker compose down -v` unless the local database data is intended
to be deleted.

See `TRAVELMAP_COMPLETION_AUDIT.md` for the distinction between passed pipeline
gates and remaining product/cloud work.

## IR search, dataset and evaluation

Search does not depend on Geoapify for ranking. The backend builds its own
inverted index and BM25 scorer over the local POI table, then combines text
relevance with spatial decay, opening-hours fit and Bayesian-shrunk rating
into a single `finalScore`. Every result carries a `scoreDetail` (bm25,
spatial, temporal, rating, finalScore) so ranking can be explained and
audited, not just trusted.

`GET /api/v1/search` accepts `rankingMode=keyword|distance|full` to run the
same query through one signal at a time (`keyword` = BM25 only, `distance` =
spatial only, `full` = all four signals, the default) — useful for comparing
baselines without a separate UI. `k1`/`b` for BM25 are configurable via
`travelmap.search.bm25.k1`/`.b` (env: `SEARCH_BM25_K1`, `SEARCH_BM25_B`),
default 1.2/0.75.

`./infra/scripts/seed-demo.sh` also applies `infra/seed/ir_dataset_seed.sql`:
45 POI total, designed on purpose (not random) to exercise every ranking
signal — near-duplicate names (`ABC Coffee`, `ABC Coffee 2`, …), overnight
opening hours (`18:00`→`02:00` and variants), deliberately skewed
rating/review counts (5.0/1 review vs 4.5/1000 reviews), and the same
category spread across 0.3/1/3/8 km from a shared reference point.

Ground truth for evaluation lives at `docs/qrels/ir_dataset_qrels.json` — 14
queries with graded relevance (0–3) against real, deterministic POI UUIDs
(reproducible: `md5(seed_key)`), a fixed reference location, and a fixed
`visitAt` (not server "now") so a rerun always produces the same numbers.

Run the real evaluation (needs the API up and the dataset seeded):

```bash
services/api/mvnw test -Dtest=IrDatasetEvaluationRunner
```

This calls the live `GET /api/v1/search` over HTTP for every query × mode,
computes Precision@5/MAP/NDCG@10 with the same `Metrics` class the unit tests
use, prints an aggregate + per-query table, and writes
`outputs/evaluation/results.{json,csv}`. Its class name does not match
Surefire's default `*Test` pattern, so it is skipped by plain `mvn test`
(and therefore never needs a live API to pass in CI); it only runs when
named explicitly as above. Without a live API it self-skips instead of
failing.

Signed-in users get personal history: `GET/DELETE /api/v1/search/history`
(deduplicated by query, most recent first) and
`GET/DELETE /api/v1/users/me/viewed-pois` (recorded via
`POST /api/v1/pois/{poiId}/view`, since POI detail renders from the search
result already in memory rather than a separate fetch). The web UI shows
both as "Tìm kiếm gần đây" and "Địa điểm đã xem" before a search is run.

### Integration tests (Testcontainers)

`SearchApiIT` exercises the search/history/viewed-POI HTTP endpoints end to
end through `MockMvc` on a real `postgis/postgis:16-3.4` container (Flyway
V1–V9, no mocked repositories) — this is what caught the `search_log`/
`search_history` `NOT NULL` regression that a GPS-less search used to hit.
Like `IrDatasetEvaluationRunner`, its name deliberately avoids Surefire's
default `*Test` pattern (it's suffixed `IT`, the Maven Failsafe convention)
so plain `mvn test`/`mvn clean test` never needs Docker. Run it explicitly:

```bash
docker info >/dev/null 2>&1 || colima start   # any Docker daemon works
cd services/api && ./mvnw test -Dtest=SearchApiIT
```

If Docker only listens on a non-default socket (e.g. Colima on macOS),
export before running:

```bash
export DOCKER_HOST=unix:///Users/<you>/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Foundation commands

```bash
docker compose -f infra/docker-compose.yml up -d
cd services/api && ./mvnw test
cd apps/web && npm install && npm run build
```

TravelMap publishes PostGIS on host port `55432` by default to avoid colliding
with existing local PostgreSQL instances. Override it with `POSTGRES_PORT`.

No cloud account is required for local development. The Docker Compose database
uses development-only credentials and must not be reused in production.
