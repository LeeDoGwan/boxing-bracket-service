# Testing

Last updated: 2026-07-16

## Latest Verification

- Backend working directory: `back`
- Command: `mvn test`
- Verified at: 2026-07-16
- Result: 382 passed, 0 failed, 0 errors, 0 skipped
- Test classes: 72
- Runtime profile: `test`
- Test database: H2 in-memory database configured by `back/src/test/resources/application-test.yml`

## Continuous Integration

- [Backend CI workflow](../.github/workflows/backend-ci.yml): Temurin Java 11, Maven dependency cache, `mvn -q test` from `back/`.
- [Frontend CI workflow](../.github/workflows/frontend-ci.yml): Node.js 24, npm dependency cache, `npm ci`, `npm test`, `npm run lint`, and `npm run build` from `front/`.
- The repository has no Maven Wrapper or Node version file; the workflow pins Java 11 and Node.js 24 explicitly, while Maven is supplied by the GitHub-hosted runner.
- Triggers: relevant `back/` or `front/` branch pushes, future pull requests, and manual dispatch. Documentation-only changes do not trigger these source workflows.
- Both workflows use read-only repository permissions, a 15-minute job timeout, and cancel older runs for the same workflow and ref. Backend and frontend jobs remain independent.
- Success requires command exit codes to pass, no failed or errored tests, no ESLint errors, and a successful production build. Test counts are intentionally not hard-coded.
- These workflows verify source changes only. Deployment, Docker, infrastructure, production database credentials, performance tests, and pull request creation remain outside this stage. The backend test profile does apply the checked-in Flyway migration to H2.

## Database Migration Verification

- Flyway migration files live in `back/src/main/resources/db/migration/` and are applied before Hibernate schema validation.
- `back/src/main/resources/application-local.yml` enables MariaDB migration and sets `ddl-auto: validate`; it does not create or alter tables through Hibernate.
- `back/src/test/resources/application-test.yml` uses the same migration location with H2 MySQL compatibility mode and `ddl-auto: validate`.
- `DatabaseMigrationIntegrationTest` verifies the V1 history record, no pending or duplicate migration, entity tables, optimistic-lock columns, and operational unique constraints.
- `mvn test` is the migration test command. It does not require MariaDB credentials and does not prove every MariaDB-specific execution detail; a deployment rehearsal must run the same files against an approved MariaDB instance.
- Existing `docs/database-migration-*.sql` files are historical pointers only. They contain no executable duplicate DDL; the Flyway directory is the single execution source.

## Test Scope

- Auth API and role access policy tests for login, logout, current account lookup, BCrypt password matching, protected route mapping, and interceptor behavior.
- SSE event stream tests for stream subscription, event payloads, subscriber filtering, and broken subscriber cleanup.
- Domain behavior tests for account, tournament, ring, athlete, bout, notice, schedule, and scoring models.
- Repository slice tests for tournament, ring, athlete, bout, notice, account, round score, bout result, and schedule persistence.
- Service tests for audience home, public bout/ring/notice/schedule queries, judge scoring, supervisor scoring, ring manager workflow, admin management flows, bout CSV/Excel import, schedule reference validation, and tournament operation status aggregation.
- Controller tests for health, audience home, public bout/ring/notice/schedule APIs, judge APIs, supervisor APIs, ring manager APIs, admin APIs, tournament operation status queries, and 409 workflow conflicts.
- Concurrency tests for duplicate bout starts, score submissions, and result confirmations using `ExecutorService` and `CountDownLatch`.
- Audit tests for action resolution, sensitive-data masking, successful and failed controller mutations, query filters, paging, and idempotent operation fingerprints.
- Staff assignment tests for active account/role validation, ring/tournament mismatch, duplicate handling, and immediate unassigned-ring denial.
- Supervisor result tests for active assignment scope, authenticated actor ownership, score readiness, decision/winner validation, positive penalties, final-state locks, and the assigned-ring bout route.
- Ring Manager transition tests for assigned-ring scope, start idempotency, scheduled-bout preparation boundaries, exact round sequencing and range, scoring readiness, next-bout candidate filtering, completion ownership, conflict responses, and event suppression on failed transitions.
- Frontend tests for utility formatting, notice rotation, schedule rendering, ring cards, bout detail loading, bracket search, audience and staff SSE filtering/deduplication/cleanup, coalesced event refresh, judge login, supervisor login, ring manager login, operations manager login, audit log login, tournament admin login, ring admin login, athlete admin login, notice admin login, schedule admin login, bout admin login, account admin login, score validation and confirmation, score input preservation during refresh, penalty history loading, penalty creation, result confirmation, Ring Manager assigned-ring selection, current-bout mismatch protection, state-specific command visibility, exact next-round input, confirmation/cancel, double-click prevention, server error mapping, live command recalculation, and server-selected next-bout operations, operations refresh/retry, audit filters/pagination/retry, tournament create/update/delete, ring create/update/delete, athlete search/create/update/delete, notice create/update/delete, schedule create/update/delete, bout create/update/delete, CSV/Excel import/template download, account search/filter/create/update/delete, and empty states.

## Frontend Verification

The frontend screen, API, SSE, responsive, accessibility, and test ownership
map is maintained in the
[frontend wide-frame architecture guide](frontend-wide-frame.md).

- Working directory: `front`
- `npm test`: 79 passed across 24 test files
- `npm run lint`: passed with `dist` and `node_modules` excluded
- `npm run build`: passed with Vite production output
- Browser verification covers the public home and bracket routes, API failure and empty states, tournament selection, bracket search, the Judge, Supervisor, Ring Manager, Operations, Audit Log, Tournament Admin, Ring Admin, Athlete Admin, Notice Admin, Schedule Admin, Bout Admin, and Account Admin login routes, and invalid-credential handling. Authenticated score submission, result confirmation, ring commands, operator SSE-driven refetch, operations refresh/retry, audit filtering/pagination, tournament CRUD, ring CRUD, athlete search/CRUD, notice CRUD, schedule CRUD, bout CRUD, CSV/Excel import/template download, and account search/filter/CRUD are covered by the frontend page tests; the test profile does not seed role accounts or tournament, ring, bout, schedule, or audit data.

## Verification Inventory

| Area | Test class | Cases |
| --- | --- | ---: |
| Auth | `AuthControllerTest` | 6 |
| Auth | `AuthProtectedApiControllerTest` | 2 |
| Auth | `AuthServiceTest` | 7 |
| Auth | `AuthInterceptorTest` | 4 |
| Auth | `RoleAccessPolicyTest` | 6 |
| Audit | `AuditActionResolverTest` | 2 |
| Audit | `AuditLogControllerTest` | 1 |
| Audit | `AuditDataSerializerTest` | 2 |
| Audit | `AuditLogAspectTest` | 2 |
| Audit | `AuditLogServiceTest` | 3 |
| Athlete | `AdminAthleteControllerTest` | 10 |
| Athlete | `AthleteTest` | 2 |
| Athlete | `AthleteRepositoryTest` | 1 |
| Athlete | `AdminAthleteServiceTest` | 12 |
| Bout | `AdminBoutControllerTest` | 12 |
| Bout | `AdminBoutServiceTest` | 19 |
| Bout | `BoutControllerTest` | 7 |
| Bout | `BoutTest` | 7 |
| Bout | `BoutRepositoryTest` | 1 |
| Bout | `BoutOptimisticLockTest` | 1 |
| Bout | `BoutServiceTest` | 13 |
| Event | `BoutEventStreamControllerTest` | 3 |
| Event | `BoutEventResponseTest` | 2 |
| Event | `BoutEventPublisherTest` | 2 |
| Event | `BoutEventStreamServiceTest` | 3 |
| Health | `HealthControllerTest` | 1 |
| Home | `HomeControllerTest` | 2 |
| Home | `HomeServiceTest` | 2 |
| Notice | `AdminNoticeControllerTest` | 10 |
| Notice | `AdminNoticeServiceTest` | 10 |
| Notice | `NoticeControllerTest` | 3 |
| Notice | `NoticeTest` | 3 |
| Notice | `NoticeRepositoryTest` | 1 |
| Notice | `NoticeServiceTest` | 3 |
| Operations | `TournamentOperationStatusControllerTest` | 1 |
| Operations | `TournamentOperationStatusServiceTest` | 2 |
| Schedule | `ScheduleControllerTest` | 2 |
| Schedule | `ScheduleItemTest` | 3 |
| Schedule | `ScheduleServiceTest` | 3 |
| Schedule | `AdminScheduleControllerTest` | 4 |
| Schedule | `AdminScheduleServiceTest` | 5 |
| Ring | `AdminRingControllerTest` | 11 |
| Ring | `AdminRingServiceTest` | 12 |
| Ring | `RingControllerTest` | 4 |
| Ring | `RingTest` | 5 |
| Ring | `RingRepositoryTest` | 1 |
| Ring | `RingServiceTest` | 11 |
| Ring Manager | `RingManagerControllerTest` | 8 |
| Ring Manager | `RingManagerServiceTest` | 26 |
| Scoring | `JudgeScoreControllerTest` | 7 |
| Scoring | `SupervisorPenaltyControllerTest` | 4 |
| Scoring | `SupervisorResultControllerTest` | 3 |
| Scoring | `SupervisorScoreControllerTest` | 2 |
| Scoring | `RoundScoreTest` | 4 |
| Scoring | `RoundScoreRepositoryTest` | 2 |
| Scoring | `BoutResultRepositoryTest` | 1 |
| Scoring | `JudgeScoreServiceTest` | 13 |
| Scoring | `ScoreQueryServiceTest` | 4 |
| Assignment | `AssignedRingControllerTest` | 1 |
| Scoring | `SupervisorPenaltyServiceTest` | 10 |
| Scoring | `SupervisorResultServiceTest` | 11 |
| Tournament | `AdminTournamentControllerTest` | 9 |
| Tournament | `AdminTournamentServiceTest` | 11 |
| Tournament | `TournamentTest` | 2 |
| Tournament | `TournamentRepositoryTest` | 1 |
| User | `AdminAccountControllerTest` | 10 |
| User | `AdminAccountServiceTest` | 12 |
| User | `AccountTest` | 2 |
| User | `AccountRepositoryTest` | 1 |
| Workflow | `WorkflowConcurrencyIntegrationTest` | 3 |

## Verified User Flows

- Active accounts can log in with BCrypt-hashed credentials, read the current account, log out, and be checked against role-specific protected API groups.
- Audience clients can subscribe to bout update events by tournament and optional ring.
- Audience can read active notice banners, and admin users can manage notice lifecycle per tournament.
- Audience can read tournament schedules, and admin users can manage schedule lifecycle per tournament with optional same-tournament ring and bout references.
- Audience can read tournament home data, current bout information, ring status, and official bout lists/search/detail.
- Judges can submit round scores and retrieve judge-specific scores.
- Judge score submission rejects invalid values, unstarted/future/out-of-range rounds, and closed bouts; same-payload retries remain idempotent and successful submissions publish one event.
- Supervisors can load active assigned rings and official bouts, review score readiness and persisted penalty history, add positive penalties, and confirm results using the authenticated Supervisor actor.
- Supervisor result confirmation rejects missing/draft scores, invalid decision/winner combinations, forged actor IDs, and post-confirmation penalty mutations; successful confirmation publishes one event and locks the UI.
- Ring managers can list assigned bouts, start only prepared current bouts, start exact next rounds, enter scoring only after configured rounds, cancel eligible pre-start bouts, and advance to a server-selected next bout.
- Duplicate workflow requests return the prior result without duplicate SSE delivery; conflicting state changes and different resubmissions return HTTP 409.
- Concurrent bout starts, identical score submissions, and identical result confirmations persist one final record and publish one event.
- Admin users can manage tournaments, rings, athletes, bouts, notices, and service accounts; account passwords are hashed before storage, and service managers can filter accounts by login/name, role, and status.
- Game managers can import bout schedules from CSV or Excel files using the admin bout import endpoint.
- Game managers and service managers can read a tournament's read-only operation summary, including status counts, ring progress, registered judge score submissions, pending results, and bouts in progress for more than 15 minutes.
- Authorized administrators can filter immutable audit logs for operational, admin, and authentication mutations; sensitive credentials and session material are masked, and idempotent workflow retries retain one audit record.
- Administrators can create and deactivate staff ring assignments; Judge, Supervisor, and Ring Manager screens load assigned rings before scoped operations. Ring Manager uses the assigned ring's `currentBoutId`, command-specific state guards, confirmation steps, and SSE-driven command recalculation; Judge payloads omit `judgeId`.
