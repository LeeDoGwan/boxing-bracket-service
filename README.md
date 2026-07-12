# boxing-bracket-service

Boxing bracket and tournament advice service.

## Status

Sprint 1 core backend flow is in progress.

Implemented core areas:

- Auth login/logout/me APIs, role access policy, and BCrypt password hashing
- Audience home, notice banners, ring status, current bout, official bout list/search/detail
- SSE event stream for bout updates
- Judge score submission and judge-specific score query filter
- Supervisor score overview, penalties, and result confirmation
- Ring manager bout list/start/status/round start/next
- Admin tournament, ring, athlete, bout, notice, and account management APIs
- Admin bout CSV import API
- Audience React MVP with live bout updates and official bracket search
- Judge React scoring desk with authenticated login and round score submission
- Tournament operation status summary for game and service managers
- Idempotent bout, round, score, and result requests with transaction-safe SSE delivery
- Immutable administrator audit logs for operational, admin, and authentication mutations

## Documentation

- [Product requirements](docs/requirements.md)
- [Sprint 1 scope](docs/sprint-1.md)
- [Test inventory and verification](docs/testing.md)
- [Concurrency database migration](docs/database-migration-concurrency.sql)
- [Administrator audit log](docs/audit-log.md)
- [Audit log database migration](docs/database-migration-audit-log.sql)
- [Audience web MVP](frontend/README.md)

## Local Development

### Requirements

- Java 11
- Maven 3.9.x
- Node.js and npm

### Run tests

```bash
mvn test
```

Current documented suite: 64 backend test classes, 329 backend test cases, and 12 frontend test cases.

### Run application

```bash
mvn spring-boot:run
```

Run the audience frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `/judge?tournamentId=1` for the judge desk. Judge APIs require a `JUDGE` account; bout assignment is deferred, so the desk selects from the tournament's official bout list.

### Health check

```http
GET http://localhost:8080/api/health
```

## Main API Groups

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/events/stream?tournamentId=&ringId=`
- `GET /api/home?tournamentId=`
- `GET /api/notices?tournamentId=`
- `GET /api/bouts?tournamentId=`
- `GET /api/bouts/search?tournamentId=&keyword=`
- `GET /api/rings/status?tournamentId=`
- `POST /api/judge/bouts/{boutId}/rounds/{roundNo}/scores`
- `GET /api/judge/bouts/{boutId}/scores?judgeId=`
- `GET /api/supervisor/bouts/{boutId}/scores`
- `POST /api/supervisor/bouts/{boutId}/penalties`
- `POST /api/supervisor/bouts/{boutId}/result`
- `GET /api/ring-manager/rings/{ringId}/bouts`
- `POST /api/ring-manager/bouts/{boutId}/start`
- `POST /api/ring-manager/bouts/{boutId}/rounds/{roundNo}/start`
- `POST /api/ring-manager/bouts/{boutId}/status`
- `POST /api/ring-manager/rings/{ringId}/next`
- `/api/admin/tournaments`
- `/api/admin/rings`
- `/api/admin/athletes`
- `/api/admin/bouts`
- `POST /api/admin/bouts/import`
- `/api/admin/notices`
- `/api/admin/accounts`
- `GET /api/admin/operations/status?tournamentId=`
- `GET /api/admin/audit-logs?tournamentId=&actorAccountId=&actionType=&page=&size=`
