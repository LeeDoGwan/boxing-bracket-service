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
- Tournament operation status summary for game and service managers

## Documentation

- [Product requirements](docs/requirements.md)
- [Sprint 1 scope](docs/sprint-1.md)
- [Test inventory and verification](docs/testing.md)

## Local Development

### Requirements

- Java 11
- Maven 3.9.x

### Run tests

```bash
mvn test
```

Current documented suite: 56 test classes, 305 test cases.

### Run application

```bash
mvn spring-boot:run
```

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
