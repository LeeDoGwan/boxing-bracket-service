# Single-Server Deployment Runbook

Last updated: 2026-08-09

This runbook covers the MVP deployment shape: one Spring Boot backend, one
MariaDB instance, and one static Vite frontend behind the same reverse proxy.
No shared production MariaDB or deployment credentials are stored in this
repository.

## 1. Release prerequisites

- Java 11, Maven 3.9.x, Node.js 24.x, npm, and MariaDB 10.11 or compatible.
- A DNS name or server address for the public audience screen.
- A least-privilege MariaDB application account and a separate migration
  operator account if the hosting policy requires it.
- A TLS-enabled reverse proxy that supports long-lived SSE connections.
- A tested backup destination with retention and access controls.

## 2. Create the database

Run the following as a MariaDB administrator. Replace placeholders locally and
do not commit the resulting password.

```sql
CREATE DATABASE boxing_bracket
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'boxing'@'localhost' IDENTIFIED BY '<strong-password>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES,
      LOCK TABLES ON boxing_bracket.* TO 'boxing'@'localhost';
FLUSH PRIVILEGES;
```

The application starts with `ddl-auto: validate`. Flyway owns schema creation
and applies V1 through V6 on a new database. Do not enable Hibernate schema
creation or `baseline-on-migrate` to bypass a mismatch.

## 3. Configure and start the backend

Set the database password in the backend process environment. For a remote
database, override the standard Spring datasource properties rather than
editing the checked-in application file.

```powershell
$env:BOXING_DB_PASSWORD = '<strong-password>'
$env:SPRING_DATASOURCE_URL = 'jdbc:mariadb://db-host:3306/boxing_bracket'
$env:SPRING_DATASOURCE_USERNAME = 'boxing'
cd back
mvn -q test
mvn spring-boot:run
```

Before opening the frontend, verify:

```text
GET /api/health -> 200
GET /api/home?tournamentId=<id> -> success=true
```

The startup log must show Flyway completing without checksum, pending
migration, or schema-validation errors. A failed migration is a release stop.

## 4. Build and publish the frontend

Set `VITE_API_BASE_URL` to the same-origin API path or the approved API origin
before building. Never place credentials in Vite variables.

```powershell
cd front
npm ci
npm test
npm run lint
npm run build
```

Publish `front/dist` as static content. The reverse proxy must:

- route `/api/` to the backend;
- forward `/api/events/stream` without response buffering or short idle timeouts;
- provide history fallback for `/`, `/bracket`, and staff routes;
- serve the new asset manifest and invalidate an older application shell;
- enforce HTTPS and restrict database access to the backend host.

## 5. Initial data and account smoke test

Before the event, create one tournament, its rings, athletes, bouts, schedules,
and active accounts for Judge, Supervisor, Ring Manager, and Game Manager.
Verify that the tournament Judge count is `3` or `5`, assignments match the
physical rings, and the selected tournament ID is valid.

## 6. Release smoke test

Run this with one real or fixture bout and record the result in the release
ticket:

1. Open the public home on a tablet and confirm notices, rings, current/next
   bouts, schedules, and bracket navigation.
2. Open bout detail and confirm submitted round scores render without judge
   account IDs; draft scores remain hidden.
3. Log in as each staff role through `/staff/login` and confirm only the
   assigned operational menu is visible.
4. Start a bout, submit scores from all configured Judges, and confirm the
   Supervisor remains blocked until all required submissions exist.
5. Add a penalty and verify it increases the opponent's effective score.
6. Confirm a normal result, TKO, and a tied-effective-score case according to
   the venue decision. Verify the result appears publicly after refresh/SSE.
7. Correct one confirmed result with a Supervisor reason and verify the same
   reason appears in the audit log.
8. Check 360px and tablet widths for horizontal overflow and usable score/action
   controls.

Any failed safety rule, leaked judge identifier, missing audit reason, stale
result, or unhandled API error blocks release.

## 7. Backup and recovery

Take a verified backup before the first migration and before each production
schema change:

```powershell
mariadb-dump --single-transaction --routines --events boxing_bracket > boxing_bracket_YYYYMMDD.sql
mariadb --database boxing_bracket < boxing_bracket_YYYYMMDD.sql
```

The restore command is illustrative; validate the dump and restore into an
isolated database before using it for recovery. Flyway migrations are not
automatically reversible. For a failed release, stop application writes,
preserve logs and the failed database state, then use the approved backup
  restore or a reviewed forward-fix migration. Never edit an applied migration.
- Before first production use, query `athletes` for `tournament_id IS NULL`.
  Backfill each legacy row from the approved tournament roster, verify all bout
  references, then schedule a reviewed migration that changes the column to
  `NOT NULL`. Do not guess a tournament for an existing athlete.

## 8. Rollback and operating limits

- A frontend-only rollback may restore the previous `dist` while the backend
  remains unchanged.
- A backend rollback is allowed only when the previous binary is compatible
  with the applied schema; otherwise use a forward-fix migration.
- The MVP uses process-local sessions and in-memory event delivery. Run one
  backend instance only. Horizontal scaling requires shared sessions and an
  external event strategy.
- Monitor backend health, database disk space, migration status, SSE connection
  counts, and audit write failures during the event.
