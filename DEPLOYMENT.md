# TravelMap Deployment

## Target

Recommended low-cost setup:

- Frontend: Render Web Service using `apps/web/Dockerfile`
- Backend: Render Web Service using `services/api/Dockerfile`
- Database: Supabase PostgreSQL with PostGIS, Neon with PostGIS, or Render PostgreSQL if PostGIS is enabled
- Maps/Search: Geoapify tile, geocoding and places API

## Required Inputs

You need these before a public deploy:

```text
Git remote URL
Cloud PostgreSQL JDBC URL
Cloud database username
Cloud database password
Frontend public URL
Backend public URL
Geoapify API key
JWT secret, 32+ characters
Payment webhook secret, 32+ characters
```

## Render Environment Variables

Backend service `travelmap-api`:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://HOST:PORT/DATABASE?sslmode=require
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=generate-a-long-random-secret
PAYMENT_WEBHOOK_SECRET=generate-a-long-random-secret
CORS_ALLOWED_ORIGIN_PATTERNS=https://YOUR_FRONTEND_DOMAIN
```

Frontend service `travelmap-web`:

```text
VITE_API_BASE_URL=https://YOUR_BACKEND_DOMAIN
VITE_MAP_TILE_URL=https://maps.geoapify.com/v1/tile/carto/{z}/{x}/{y}.png?&apiKey=YOUR_GEOAPIFY_API_KEY
VITE_MAP_TILE_ATTRIBUTION=&copy; OpenStreetMap contributors &copy; Geoapify
VITE_GEOAPIFY_API_KEY=YOUR_GEOAPIFY_API_KEY
```

## Deploy Flow

1. Push this repository to GitHub/GitLab.
2. Create a cloud PostgreSQL database and enable PostGIS.
3. Create `travelmap-api` from `render.yaml`.
4. Set backend environment variables.
5. Wait for backend startup; Flyway will run migrations automatically.
6. Create `travelmap-web` from `render.yaml`.
7. Set frontend environment variables.
8. After frontend URL exists, update backend `CORS_ALLOWED_ORIGIN_PATTERNS`.
9. Redeploy backend.
10. Open the frontend URL and test login, GPS/location, map tile, search radius and pagination.

## Production Seed

The local seed script is demo-only. For a real production demo, create a
production-safe admin user intentionally and do not reuse local passwords.

Local demo seed:

```bash
./infra/scripts/seed-demo.sh
```

For cloud seed, run an adapted SQL script against the cloud database only after
confirming the target database is disposable or demo-only.

## Smoke Checks

Backend:

```bash
curl https://YOUR_BACKEND_DOMAIN/api/v1/health
```

Frontend:

```bash
curl -I https://YOUR_FRONTEND_DOMAIN/health
```

Browser:

```text
1. Open frontend URL
2. Login or register
3. Allow location permission, or enter coordinates manually
4. Search `cafe` with radius 2 km
5. Confirm results stay within radius and pagination shows 10 per page
```
