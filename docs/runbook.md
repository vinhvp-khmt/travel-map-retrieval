# TravelMap Operations Runbook

## Required production inputs

- PostgreSQL 16 with PostGIS, plus `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.
- Random `JWT_SECRET` and `PAYMENT_WEBHOOK_SECRET`, each at least 32 characters.
- `CORS_ALLOWED_ORIGIN_PATTERNS` containing only the deployed frontend origins.
- `SPRING_PROFILES_ACTIVE=prod`. Startup fails when production secrets are weak or use local defaults.

## Deploy and verify

1. Provision an online PostgreSQL/PostGIS database and apply its credentials in the host dashboard.
2. Deploy `travelmap-api` and wait for Flyway to migrate the schema.
3. Deploy `travelmap-web`, set its API base URL, then restrict the API CORS setting to that origin.
4. Run `BASE_URL=https://api.example.com infra/scripts/smoke-test.sh`.
5. Confirm `/api/v1/health` returns `{"status":"UP"}` and monitor non-2xx rates and latency.

## Incident response

- API unhealthy: inspect application logs, then database connectivity and Flyway history. Do not delete or recreate the database volume.
- Migration failure: stop the new API revision, preserve logs, fix with a forward-only Flyway migration, and redeploy.
- Suspected secret exposure: rotate JWT and webhook secrets, redeploy, and force users to authenticate again.
- Payment webhook errors: retain event IDs and payload hashes; retries are idempotent through the webhook-event table.

## Rollback and backup

- Roll back the application image to the previous immutable revision. Database migrations are forward-only.
- Enable daily managed-database backups and test point-in-time recovery before launch.
- Before a risky migration, create a provider snapshot and record its restore identifier.
