# boxing-bracket-service

Boxing bracket and tournament advice service.

## Status

Sprint 1 core backend flow and tournament schedule management are implemented.

Implemented core areas:

- Auth login/logout/me APIs, role access policy, and BCrypt password hashing
- Audience home, notice banners, ring status, current bout, official bout list/search/detail
- SSE event stream for bout updates
- Judge score submission and judge-specific score query filter
- Supervisor score overview, penalty history, penalties, and result confirmation
- Ring Manager assigned-ring workflow with server-validated state transitions, round sequencing, and next-bout selection
- Admin tournament, ring, athlete, bout, notice, and account management APIs
- Admin bout CSV and Excel import API
- Audience React MVP with live bout updates and official bracket search
- Judge React scoring desk with authenticated login and round score submission
- Supervisor React review desk with penalty history, creation, and result confirmation
- Audience schedule list for bouts, breaks, meals, performances, and events
- Ring Manager React operations desk with ring bout control and round transitions
- Operations React monitoring desk with ring progress and exception tracking
- Audit log React desk with operational filters and paginated change history
- Tournament admin React desk with tournament CRUD management
- Ring admin React desk with per-tournament ring CRUD management
- Athlete admin React desk with searchable athlete CRUD management
- Notice admin React desk with tournament-scoped notice publishing management
- Schedule admin React desk with tournament-scoped schedule CRUD management
- Bout admin React desk with bout CRUD and CSV/Excel import
- Account admin React desk restricted to service managers, with keyword, role, and status filters
- Tournament operation status summary for game and service managers
- Idempotent bout, round, score, and result requests with transaction-safe SSE delivery
- Immutable administrator audit logs for operational, admin, and authentication mutations

## Documentation

- [Product requirements](docs/requirements.md)
- [System design](docs/design.md)
- [Frontend wide-frame architecture](docs/frontend-wide-frame.md)
- [Sprint 1 scope](docs/sprint-1.md)
- [Test inventory and verification](docs/testing.md)
- [Backend CI workflow](.github/workflows/backend-ci.yml)
- [Frontend CI workflow](.github/workflows/frontend-ci.yml)
- [Concurrency database migration](docs/database-migration-concurrency.sql)
- [Administrator audit log](docs/audit-log.md)
- [Schedule database migration](docs/database-migration-schedule.sql)
- [Audit log database migration](docs/database-migration-audit-log.sql)
- [Frontend README](front/README.md)

## Repository Layout

- `back/` - Spring Boot API, domain, persistence, and backend tests
- `front/` - React/Vite web application and frontend tests
- `docs/` - Requirements, design, sprint scope, migrations, and verification records

## Local Development

### Requirements

- Java 11
- Maven 3.9.x
- Node.js 24.x and npm

### Run tests

Run backend tests from `back/`:

```bash
cd back
mvn test
```

Run frontend verification from `front/`:

```bash
cd front
npm ci
npm test
npm run lint
npm run build
```

Current documented suite: 71 backend test classes, 380 backend test cases, and 78 frontend test cases.

### Run application

Start the backend from `back/`:

```bash
cd back
mvn spring-boot:run
```

Run the frontend from `front/` in another terminal:

```bash
cd front
npm install
npm run dev
```

Open `/judge?tournamentId=1` for the judge desk, `/supervisor?tournamentId=1` for the supervisor desk, `/ring-manager?tournamentId=1` for the ring manager desk, `/operations?tournamentId=1` for the operations desk, `/audit-logs?tournamentId=1` for the audit log desk, `/admin/tournaments?tournamentId=1` for tournament management, `/admin/rings?tournamentId=1` for ring management, `/admin/athletes?tournamentId=1` for athlete management, `/admin/notices?tournamentId=1` for notice management, `/admin/schedules?tournamentId=1` for schedule management, `/admin/bouts?tournamentId=1` for bout management, or `/admin/accounts?tournamentId=1` for account management. These APIs require the matching role account; Judge, Supervisor, and Ring Manager desks load active assigned rings before scoped operations.

## Continuous Integration

GitHub Actions keeps source verification separate from deployment:

- [Backend CI](.github/workflows/backend-ci.yml) runs Java 11 and `mvn -q test` from `back/`.
- [Frontend CI](.github/workflows/frontend-ci.yml) runs Node 24, `npm ci`, `npm test`, `npm run lint`, and `npm run build` from `front/`.
- Each workflow runs on relevant `back/` or `front/` changes, future pull requests, and manual dispatch. Pushes cancel older in-progress runs for the same workflow and ref.
- CI uses read-only repository permissions and does not deploy, create pull requests, or require secrets.

### Health check

```http
GET http://localhost:8080/api/health
```

## Main API Groups

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/events/stream?tournamentId=&ringId=`
- `GET /api/staff/assignments/rings?tournamentId=`
- `GET /api/staff/assignments/rings/{ringId}/bouts`
- `GET /api/home?tournamentId=`
- `GET /api/notices?tournamentId=`
- `GET /api/schedules?tournamentId=`
- `GET /api/bouts?tournamentId=`
- `GET /api/bouts/search?tournamentId=&keyword=`
- `GET /api/rings/status?tournamentId=`
- `POST /api/judge/bouts/{boutId}/rounds/{roundNo}/scores`
- `GET /api/judge/bouts/{boutId}/scores?judgeId=`
- `GET /api/supervisor/bouts/{boutId}/scores`
- `GET /api/supervisor/bouts/{boutId}/penalties`
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
- `/api/admin/schedules`
- `/api/admin/accounts?keyword=&role=&status=`
- `GET /api/admin/operations/status?tournamentId=`
- `GET /api/admin/audit-logs?tournamentId=&actorAccountId=&actionType=&page=&size=`
