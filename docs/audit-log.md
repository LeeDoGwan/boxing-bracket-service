# Administrator Audit Log

## Coverage

The service appends immutable audit records for state-changing operations:

- Ring-manager bout start, status, round start, and next bout actions.
- Judge score submission, supervisor penalty creation, and result confirmation.
- Administrator bout, account, and notice create, update, delete, and bout CSV import actions.
- Login success, login failure, and logout.

Read-only APIs do not create audit records. Success records are written after the controller receives the committed service result. Failed controller operations are recorded independently, and an audit persistence failure is logged to the server without rolling back the business operation.

Idempotent workflow responses use a unique operation fingerprint, so repeat requests that return the same completed state retain one audit record.

## Data Protection

`beforeData` and `afterData` are JSON snapshots. Fields whose names contain `password`, `token`, `authorization`, or `session` are replaced with `***`. Raw string method arguments, including the Authorization header, are excluded from request snapshots.

## Query API

```http
GET /api/admin/audit-logs
```

Available query parameters are `tournamentId`, `actorAccountId`, `actorRole`, `actionType`, `targetType`, `ringId`, `boutId`, `success`, `from`, `to`, `page`, and `size`. Results are sorted by newest first. `page` defaults to `0`, `size` defaults to `20`, and the maximum size is `100`.

The route follows the existing `/api/admin` policy and is available to `GAME_MANAGER` and `SERVICE_MANAGER`. A nonexistent `tournamentId` returns `404`; invalid date ranges and page values return `400`.

The current account schema has no tournament-assignment relationship. Consequently, game-manager audit access is role-based and tournament filtering cannot yet be constrained to an assigned tournament. Add that relationship before enforcing per-tournament manager ownership.

## Database

Apply [database-migration-audit-log.sql](database-migration-audit-log.sql) to MariaDB before deployment. Audit records intentionally have no foreign-key cascades so historical records remain available after a target account, notice, or bout is deleted.
