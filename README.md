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

- Java/Javac 21
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

Flyway automatically applies `V1` through `V7`. Verify the API and database:

```bash
curl http://localhost:8080/api/v1/health
```

Seed idempotent local demo data after the schema reaches V7:

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
