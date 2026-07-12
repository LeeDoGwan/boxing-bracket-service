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
- Supervisor React review desk with penalty creation and result confirmation
- Ring Manager React operations desk with ring bout control and round transitions
- Operations React monitoring desk with ring progress and exception tracking
- Audit log React desk with operational filters and paginated change history
- Tournament admin React desk with tournament CRUD management
- Ring admin React desk with per-tournament ring CRUD management
- Athlete admin React desk with searchable athlete CRUD management
- Notice admin React desk with tournament-scoped notice publishing management
- Bout admin React desk with schedule CRUD and CSV import
- Account admin React desk restricted to service managers
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

Current documented suite: 64 backend test classes, 329 backend test cases, and 39 frontend test cases.

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

Open `/judge?tournamentId=1` for the judge desk, `/supervisor?tournamentId=1` for the supervisor desk, `/ring-manager?tournamentId=1&ringId=1` for the ring manager desk, `/operations?tournamentId=1` for the operations desk, `/audit-logs?tournamentId=1` for the audit log desk, `/admin/tournaments?tournamentId=1` for tournament management, `/admin/rings?tournamentId=1` for ring management, `/admin/athletes?tournamentId=1` for athlete management, `/admin/notices?tournamentId=1` for notice management, `/admin/bouts?tournamentId=1` for bout management, or `/admin/accounts?tournamentId=1` for account management. These APIs require the matching role account; assignment is deferred, so judge and supervisor desks select from the official bout list while the ring manager desk loads a ring directly by ID.

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
