# Boxing Bracket Service Design

Last updated: 2026-07-15

## 1. Purpose

This document describes the architecture and runtime behavior of the current MVP. It is the implementation-oriented companion to [Product requirements](requirements.md) and [Sprint 1 scope](sprint-1.md).

The service replaces paper-based tournament operations with a shared workflow for:

- Public audience status, notices, schedules, brackets, and confirmed results.
- Judge round-score submission.
- Supervisor penalty review and result confirmation.
- Ring-manager bout control.
- Game-manager and service-manager administration.

The design favors explicit tournament IDs, small role-specific desks, server-side validation, and conflict responses that are easy to understand at a venue.

## 2. Current Scope

Implemented MVP capabilities:

- Spring Boot MVC backend with JPA repositories and MariaDB runtime configuration.
- React/Vite single-page frontend with public, judge, supervisor, ring-manager, operations, audit, and admin routes.
- In-memory bearer sessions with a 12-hour lifetime and BCrypt password verification.
- Tournament, ring, athlete, bout, notice, schedule, account, scoring, operation-status, and audit-log modules.
- CSV and Excel bout import with a matching CSV template download.
- Audience and operator ring-filtered SSE bout-update events, transaction-safe dispatch, reconnect handling, and duplicate event protection.
- Optimistic version checks, workflow row locks, idempotent retries, unique constraints, and HTTP 409 conflict responses.

Known MVP boundaries:

- Judge, supervisor, and ring-manager ring assignments are enforced server-side. The assignment unit and API details are in [Staff ring assignment](staff-assignment.md).
- Assigned Judge, Supervisor, and Ring Manager screens subscribe to one selected-ring SSE stream and refetch API state after relevant events.
- Sessions are process-local. A shared session store is required for multiple backend instances.
- Schedule mutations do not publish a dedicated schedule SSE event. Audience clients see schedule changes on a full reload.
- Audience tournament discovery is not implemented. The frontend currently accepts a positive `tournamentId` query parameter.
- Server log viewing, advanced statistics, offline support, and Game Manager tournament ownership rules remain deferred.

## 3. System Context

```mermaid
flowchart LR
    audience[Audience browser]
    staff[Staff browser]
    frontend[React and Vite SPA]
    api[Spring Boot API]
    db[(MariaDB)]
    stream[SSE bout event stream]
    audit[Audit log writer]

    audience --> frontend
    staff --> frontend
    frontend -->|HTTP JSON| api
    frontend -->|EventSource| stream
    api --> db
    api --> stream
    api --> audit
    audit --> db
```

The frontend and backend can be deployed separately. During local development, Vite proxies `/api` to `http://localhost:8080`; production deployment should provide an equivalent reverse-proxy or configure `VITE_API_BASE_URL`.

## 4. Backend Architecture

The backend is organized by business capability rather than by technical layer alone.

```text
controller -> service -> repository -> entity
     |            |
     +--> DTO     +--> domain validation and workflow rules
```

Each module normally contains:

- `controller`: HTTP mapping and `ApiResponse` wrapping.
- `service`: transaction boundary, input validation, aggregate orchestration, and response mapping.
- `repository`: Spring Data JPA queries and workflow locks where needed.
- `domain`: entity state, invariants, and state transitions.
- `dto`: request and response contracts.

Core modules:

| Module | Responsibility |
| --- | --- |
| `auth` | Login, logout, session lookup, role checks, interceptor integration |
| `tournament` | Tournament metadata and admin CRUD |
| `athlete` | Reusable athlete master data and admin CRUD |
| `ring` | Tournament ring status, current bout context, and admin CRUD |
| `bout` | Official bout list/search/detail, bout lifecycle, admin CRUD/import |
| `scoring` | Judge scores, supervisor penalties, and confirmed results |
| `schedule` | Public and admin tournament schedule items |
| `notice` | Public active notices and admin publishing lifecycle |
| `home` | Aggregated audience response |
| `operation` | Tournament-wide operational monitoring |
| `event` | Tournament/ring-filtered SSE bout updates |
| `audit` | Mutation resolution, snapshots, masking, persistence, and search |
| `assignment` | Staff ring assignments, assigned-ring lists, and staff scope enforcement |

Controllers do not access repositories directly. Cross-module references use IDs and are validated by the owning service. The current model intentionally avoids JPA entity associations for tournament, ring, athlete, bout, account, and schedule references.

## 5. Frontend Architecture

The detailed frontend screen, state, API, SSE, component, responsive,
accessibility, test, and deployment map is maintained in the
[frontend wide-frame architecture guide](frontend-wide-frame.md).

`front/src/App.jsx` owns route composition and reads `tournamentId` from the URL. `AppHeader` preserves the selected tournament ID while navigating between desks.

```text
App
|-- AppHeader and tournament query state
|-- AudienceHome -> useAudienceData + useBoutEventStream
|-- BracketPage
|-- JudgeAssignedPage
|-- SupervisorAssignedPage
|-- RingManagerAssignedPage
|-- OperationsPage
|-- AuditLogPage
|-- Admin*Page routes
`-- shared components, API clients, hooks, and styles
```

Each authenticated desk owns its login/session validation and limits the UI by role before making protected API calls. API clients share `requestApi`, which adds JSON headers, optional bearer authorization, parses the common response envelope, and turns server failures into JavaScript errors.

The public home aggregates notices, ring status, confirmed results, and schedules from `/api/home`. It opens bout details through the public bout detail API. SSE reconnects trigger a fresh audience data load, so the stream is an invalidation signal rather than the source of truth.

Assigned staff screens reuse the same stream with `tournamentId` and the selected
`ringId`. They keep one `EventSource` per screen, close it when the ring changes
or the screen unmounts, and refetch assigned bouts and selected detail data after
relevant events. SSE payloads never replace REST responses; an unavailable stream
leaves write actions usable and preserves the last API-confirmed state.

## 6. Authentication and Authorization

Authentication is enabled by the local profile and disabled by the test profile.

```mermaid
sequenceDiagram
    participant B as Browser
    participant A as AuthController
    participant S as AuthService
    participant I as AuthInterceptor
    participant C as ProtectedController

    B->>A: POST /api/auth/login
    A->>S: verify login ID, BCrypt password, ACTIVE status
    S-->>B: bearer token and account summary
    B->>I: protected request with Authorization header
    I->>S: require or requireRole
    S-->>I: active in-memory session
    I->>C: continue request
    C-->>B: ApiResponse
```

Current route policy:

| Path prefix | Required role |
| --- | --- |
| `/api/auth/logout`, `/api/auth/me` | Any authenticated session |
| `/api/admin/accounts` | `SERVICE_MANAGER` |
| `/api/admin/**` | `GAME_MANAGER` or `SERVICE_MANAGER` |
| `/api/judge/**` | `JUDGE` |
| `/api/supervisor/**` | `SUPERVISOR` |
| `/api/ring-manager/**` | `RING_MANAGER` |
| `/api/staff/**` | `JUDGE`, `SUPERVISOR`, or `RING_MANAGER` |
| Public audience, bracket, notice, schedule, health, and event routes | No role rule |

The interceptor applies to `/api/**`, excluding health and login. A request with no matching policy is allowed to continue, which is how public routes remain unauthenticated.

## 7. Domain Model

All entities inherit `createdAt` and `updatedAt` from `BaseTimeEntity`. Most cross-entity relationships are stored as scalar IDs.

```mermaid
erDiagram
    TOURNAMENT ||--o{ RING : contains
    TOURNAMENT ||--o{ BOUT : schedules
    TOURNAMENT ||--o{ NOTICE : publishes
    TOURNAMENT ||--o{ SCHEDULE_ITEM : plans
    RING ||--o{ BOUT : hosts
    BOUT ||--o{ ROUND_SCORE : receives
    BOUT ||--o{ PENALTY : records
    BOUT ||--o| BOUT_RESULT : confirms
    ATHLETE ||--o{ BOUT : red_or_blue
    ACCOUNT ||--o{ ROUND_SCORE : judge
    ACCOUNT ||--o{ STAFF_ASSIGNMENT : receives
    TOURNAMENT ||--o{ STAFF_ASSIGNMENT : scopes
    RING ||--o{ STAFF_ASSIGNMENT : scopes

    TOURNAMENT {
        bigint id PK
        string name
        string location
        date startDate
        date endDate
        string status
    }
    RING {
        bigint id PK
        bigint tournamentId
        string name
        string status
        bigint currentBoutId
        bigint version
    }
    BOUT {
        bigint id PK
        bigint tournamentId
        bigint ringId
        bigint redAthleteId
        bigint blueAthleteId
        string status
        boolean resultConfirmed
        bigint version
    }
    SCHEDULE_ITEM {
        bigint id PK
        bigint tournamentId
        bigint ringId
        string type
        datetime startTime
        datetime endTime
        bigint relatedBoutId
        string status
    }
    STAFF_ASSIGNMENT {
        bigint id PK
        bigint accountId
        bigint tournamentId
        bigint ringId
        string role
        boolean active
        bigint version
    }
```

Current implementation status values are authoritative for the MVP:

| Aggregate | Values |
| --- | --- |
| Tournament | `READY`, `IN_PROGRESS`, `FINISHED` |
| Ring | `READY`, `IN_PROGRESS`, `CLOSED` |
| Bout | `SCHEDULED`, `READY`, `IN_PROGRESS`, `SCORING`, `FINISHED`, `CANCELED` |
| Schedule item | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED` |
| Round score | `DRAFT`, `SUBMITTED` |

The product requirements use some different target vocabulary, such as `PREPARING` or `COMPLETED`. That vocabulary is retained as product direction; API clients and database values must use the current implementation values until a deliberate migration is planned.

## 8. Core Workflow

```mermaid
sequenceDiagram
    participant GM as Game manager
    participant RM as Ring manager
    participant J as Judges
    participant SV as Supervisor
    participant API as Backend services
    participant DB as Database
    participant AU as Audience

    GM->>API: Create tournament, athletes, rings, bouts, notices, schedules
    API->>DB: Validate references and persist setup
    RM->>API: Start bout or change ring bout status
    API->>DB: Lock bout/ring and apply transition
    API-->>AU: Publish committed bout-update SSE event
    J->>API: Submit one score per round
    API->>DB: Lock bout, validate version and uniqueness
    SV->>API: Read scores and penalty history
    SV->>API: Add penalties and confirm winner/decision
    API->>DB: Persist result and publish state after commit
    API-->>AU: Publish committed bout-update SSE event
    AU->>API: Reload home, bracket, and bout detail
```

Workflow rules:

1. Equivalent repeated requests return the existing state where the operation is idempotent.
2. A different payload against an already submitted or completed state returns HTTP 409.
3. Bout, ring, score, and result aggregates use optimistic versions.
4. Mutating bout and ring operations use transaction-scoped pessimistic locks.
5. Database uniqueness protects one score per judge/bout/round and one result per bout.
6. SSE dispatch is registered after transaction commit, so rolled-back state is not broadcast.

## 9. API Contract

Successful controller responses use:

```json
{
  "success": true,
  "data": {},
  "message": "OK"
}
```

Failures use the same envelope with `success: false`, `data: null`, and a stable message. The global handler maps validation to `400`, missing authentication to `401`, role failures to `403`, missing resources to `404`, workflow and optimistic conflicts to `409`, and unexpected failures to `500`.

API groups:

| Group | Main endpoints | Access |
| --- | --- | --- |
| Auth | `/api/auth/login`, `/logout`, `/me` | Login public; logout/me authenticated |
| Audience home | `/api/home`, `/api/notices`, `/api/schedules`, `/api/rings/status`, `/api/bouts` | Public |
| Live events | `/api/events/stream?tournamentId=&ringId=` | Public |
| Judge | `/api/judge/bouts/{boutId}/scores`, score submit endpoint | `JUDGE` |
| Supervisor | scores, penalties, result endpoints | `SUPERVISOR` |
| Ring manager | ring bout list and lifecycle commands | `RING_MANAGER` |
| Staff scope | `/api/staff/assignments/rings`, assigned ring bouts | `JUDGE`, `SUPERVISOR`, `RING_MANAGER` |
| Operations | `/api/admin/operations/status` | `GAME_MANAGER`, `SERVICE_MANAGER` |
| Audit | `/api/admin/audit-logs` | `GAME_MANAGER`, `SERVICE_MANAGER` |
| Administration | tournaments, rings, athletes, bouts, notices, schedules | `GAME_MANAGER`, `SERVICE_MANAGER` |
| Account administration | `/api/admin/accounts` | `SERVICE_MANAGER` |
| Staff assignment administration | `/api/admin/assignments` | `GAME_MANAGER`, `SERVICE_MANAGER` |

The detailed endpoint list remains in [Sprint 1 scope](sprint-1.md). Frontend-specific route and API usage remains in [frontend README](../front/README.md).

## 10. Persistence and Deployment

The local profile expects MariaDB at `boxing_bracket` with `ddl-auto: none`. The service therefore assumes that the base schema and migrations have already been applied.

Migration documents currently cover:

1. `database-migration-concurrency.sql`: version columns and workflow uniqueness constraints.
2. `database-migration-audit-log.sql`: immutable audit storage and indexes.
3. `database-migration-schedule.sql`: tournament schedule storage and indexes.
4. `database-migration-staff-assignment.sql`: staff ring assignments and uniqueness/indexes.

The test profile uses an H2 in-memory database with `ddl-auto: create-drop`; it discovers the JPA model directly and does not apply the MariaDB migration files.

Operational prerequisites:

- Java 11, Maven 3.9.x, Node.js, and npm.
- MariaDB schema migration execution before local-profile startup.
- Active role accounts and tournament reference data for authenticated end-to-end testing.
- A shared session store and external event delivery strategy before running multiple backend instances.

## 11. Audit and Observability

`AuditLogAspect` resolves mutation paths, captures before/after snapshots, masks sensitive fields, and persists audit records through a separate writer transaction. The audit query supports tournament, actor, role, action, target, ring, bout, success, time range, and pagination filters.

Audit records are intentionally not foreign-key cascaded. This preserves history when an account, notice, schedule, or bout is deleted. Audit persistence failures are logged without rolling back the business operation.

Server log viewing is intentionally deferred. The current operational UI reads structured tournament status and administrator audit data instead.

## 12. Verification

The latest documented verification is:

- Backend: 70 test classes, 358 test cases, zero failures, errors, or skips.
- Frontend: 24 test files, 59 test cases, ESLint passed, and Vite production build passed.
- Test inventory and user-flow coverage: [Testing](testing.md).

The test profile does not seed production accounts or tournament data. Authenticated desks require test fixtures or a running local database with active accounts.

## 13. Deferred Decisions

The following decisions should be made before expanding beyond the MVP:

- Assignment model: ring-level assignments are implemented; manager ownership per tournament remains to be decided.
- Session storage: Redis or another shared store, token revocation, and operational session monitoring.
- Public tournament discovery: directory endpoint, default tournament selection, and closed/completed tournament visibility.
- Event model: whether schedule, notice, and ring-status changes should use SSE in addition to bout updates.
- Result policy: allowed decision types, confirmed-result correction workflow, and approval requirements.
- Data ownership: whether athletes remain global master data or become tournament-scoped records.
- Production migration tooling: repeatable versioned migrations and rollback policy.
