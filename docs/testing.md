# Testing

Last updated: 2026-10-10

## Latest Verification

- Backend working directory: `back`
- Command: `mvn -q clean test`
- Verified at: 2026-10-06
- Result: 410 passed, 0 failed, 0 errors, 0 skipped
- `AccountRepositoryTest` also verifies that JPA auditing populates both `createdAt` and `updatedAt`.
- Test classes: 73 from fresh Surefire XML reports. The separate `MariaDbMigrationSmokeIT` is outside the default `*Test` discovery pattern and was not executed locally.
- Runtime profile: `test`
- Test database: H2 in-memory database configured by `back/src/test/resources/application-test.yml`

Old Surefire XML files can survive `mvn test` and inflate aggregate counts. The clean run above replaces the earlier incorrectly documented 411-case/74-class local baseline. The inventory below contains the 73 default test classes; the MariaDB smoke class is tracked separately.

## Continuous Integration

- [Backend CI workflow](../.github/workflows/backend-ci.yml): Temurin Java 11, Maven dependency cache, `mvn -q test` from `back/`, and a MariaDB 10.11 migration smoke test.
- [Frontend CI workflow](../.github/workflows/frontend-ci.yml): Node.js 24, npm dependency cache, `npm ci`, `npm test`, `npm run lint`, and `npm run build` from `front/`.
- The repository has no Maven Wrapper or Node version file; the workflow pins Java 11 and Node.js 24 explicitly, while Maven is supplied by the GitHub-hosted runner.
- Triggers: relevant `back/` or `front/` branch pushes, future pull requests, and manual dispatch. Documentation-only changes do not trigger these source workflows.
- Both workflows use read-only repository permissions, a 15-minute job timeout, and cancel older runs for the same workflow and ref. Backend and frontend jobs remain independent.
- Success requires command exit codes to pass, no failed or errored tests, no ESLint errors, and a successful production build. The default local Maven run does not select the MariaDB smoke test; CI selects it explicitly with `-Dtest=MariaDbMigrationSmokeIT`. Test counts are intentionally not hard-coded.
- These workflows verify source changes only. Deployment, Docker, infrastructure, production database credentials, performance tests, and pull request creation remain outside this stage. The backend test profile does apply the checked-in Flyway migration to H2.

## Database Migration Verification

- Flyway 9.22.3 and its `flyway-mysql` support module apply migrations from `back/src/main/resources/db/migration/` before Hibernate schema validation.
- `back/src/main/resources/application-local.yml` enables MariaDB migration and sets `ddl-auto: validate`; it does not create or alter tables through Hibernate.
- `back/src/test/resources/application-test.yml` uses the same migration location with H2 MySQL compatibility mode and `ddl-auto: validate`.
- `DatabaseMigrationIntegrationTest` verifies V1 through V6 history records, no pending or duplicate migration, entity tables, optimistic-lock columns, the tournament `judge_count` column, MariaDB-compatible audit payload mapping, the `penalties.round_no` column, the per-tournament bout-number unique constraint, import idempotency columns and constraint, bout schedule indexes, the athlete tournament-scope column/index, and operational unique constraints.
- `mvn test` remains the fast H2 migration test command. The CI-only `MariaDbMigrationSmokeIT` runs against the MariaDB service with `-Dtest=MariaDbMigrationSmokeIT -Dmariadb.integration=true` and verifies the database product plus the current Flyway V6 version.
- Existing `docs/database-migration-*.sql` files are historical pointers only. They contain no executable duplicate DDL; the Flyway directory is the single execution source.

## Test Scope

- Auth API and role access policy tests for login, logout, current account lookup, BCrypt password matching, protected route mapping, and interceptor behavior.
- SSE event stream tests for stream subscription, event payloads, subscriber filtering, and broken subscriber cleanup.
- Domain behavior tests for account, tournament, ring, athlete, bout, notice, schedule, and scoring models.
- Repository slice tests for tournament, ring, athlete, bout, notice, account, round score, bout result, and schedule persistence.
- Service tests for audience home, public bout/ring/notice/schedule queries, judge scoring, supervisor scoring, ring manager workflow, admin management flows, bout CSV/Excel import, schedule reference validation, tournament operation status aggregation, and pre-start bout lifecycle guards.
- Admin bout tests verify server-generated numbering for create/import, number preservation on update, and the import template contract without a manual `boutNumber` column.
- Controller tests for health, audience home, public bout/ring/notice/schedule APIs, judge APIs, supervisor APIs, ring manager APIs, admin APIs, tournament operation status queries, and 409 workflow conflicts.
- Concurrency tests for duplicate bout starts, score submissions, and result confirmations using `ExecutorService` and `CountDownLatch`.
- Audit tests for action resolution, sensitive-data masking, successful and failed controller mutations, query filters, paging, and idempotent operation fingerprints.
- Staff assignment tests for active account/role validation, ring/tournament mismatch, duplicate handling, and immediate unassigned-ring denial.
- Supervisor result tests for active assignment scope, authenticated actor ownership, 3/5 Judge readiness, decision/winner validation, positive penalties, confirmed-result correction reasons, final-state locks, and the assigned-ring bout route.
- Ring Manager transition tests for assigned-ring scope, start idempotency, scheduled-bout preparation boundaries, exact round sequencing and range, scoring readiness, next-bout candidate filtering, completion ownership, conflict responses, and event suppression on failed transitions.
- Concurrency tests verify duplicate bout creation receives distinct per-tournament numbers under the tournament-row lock.
- Ring-manager lock-order tests verify ring-before-bout acquisition for start and next-bout transitions.
- Scalar-reference guard tests verify tournament, ring, athlete, account, and bout deletes reject orphan-producing mutations.
- Import tests verify the required idempotency key, persistent key/row mapping, and repeated-key response reuse.
- Auth tests verify sessions are rejected after account deletion, deactivation, identity changes, or role changes.
- Frontend tests for utility formatting, staff session persistence and cleanup, notice rotation, schedule rendering, ring cards, bout detail loading and submitted public round-score display, public home composition/loading/fatal-error/stale-retry/ring-order/result-detail flows backed by the aggregate home response, bracket search, audience and staff SSE filtering/deduplication/cleanup, coalesced event refresh, shared staff authentication and role-aware navigation, score validation and confirmation including the 0-10 maximum, score input preservation during refresh, penalty round selection/history/creation, result confirmation and reasoned result correction, Ring Manager assigned-ring selection, current-bout mismatch protection, state-specific command visibility, exact next-round input, confirmation/cancel, double-click prevention, server error mapping, live command recalculation, and server-selected next-bout operations, operations refresh/retry/auto-refresh, audit filters/pagination/retry, tournament create/update/delete, ring create/update/delete, athlete search/create/update/delete, notice create/update/delete, schedule create/update/delete, bout create/update/delete, CSV/Excel import/template download, account search/filter/create/update/delete, and empty states.

## Frontend Verification

The frontend screen, API, SSE, responsive, accessibility, and test ownership
map is maintained in the
[frontend wide-frame architecture guide](frontend-wide-frame.md).

- Working directory: `front`
- Verified at: 2026-10-10
- `npm test`: 123 passed across 28 test files
- Multilingual coverage checks matching ko/en/ja keys and placeholders, language detection/overrides/storage failures, cross-tab and device-language changes, document language/title, deferred notification translation, venue-time formatting, and input/confirmation preservation for Judge, Supervisor, and Ring Manager workflows. `npm run check:i18n` runs the resource checks independently; they are included in normal CI tests.
- Added regression coverage for Korean decision/connection labels and the Game Manager assignment-account lookup restriction; penalty history assertions verify opponent addition rather than subtraction.
- `npm run lint`: passed with `dist` and `node_modules` excluded
- `npm run build`: passed with Vite production output
- Browser smoke check: public error state and login language switching; English desktop (1280x900), Japanese tablet (768x1024), Korean/English mobile (390x844). Login input survives language switching, and the mobile page has no horizontal overflow. No live backend/account workflow was exercised. See [i18n design](i18n-design.md).
- `npm audit --json` (2026-10-10): 15 dependency entries flagged, comprising 6 moderate, 7 high, and 2 critical. The reported packages do not include i18next or react-i18next; they include the existing router and build/test toolchain. Audit did not pass. Dependency remediation needs a separately tested upgrade; do not expose the Vite/Vitest development servers publicly or treat a successful build as a security clearance.
- Automated frontend coverage includes the public home and bracket routes, API failure and empty states, public submitted round-score rendering, startup account revalidation, authenticated 401 session cleanup, the configured one-tournament context, bracket search, the shared `/staff/login` route, protected-route return paths, invalid-credential handling, and role-aware navigation. Authenticated score submission, result confirmation, ring commands, operator SSE-driven refetch, operations refresh/retry/auto-refresh, audit filtering/pagination, tournament CRUD, ring CRUD, athlete search/CRUD, notice CRUD, schedule CRUD, bout CRUD, CSV/Excel import/template download, and account search/filter/CRUD are covered by frontend page and session tests; the test profile does not seed role accounts or tournament, ring, bout, schedule, or audit data. Manual browser verification remains a release smoke-test task and is tracked in [Deployment runbook](deployment-runbook.md).

## Verification Inventory

| Area | Test class | Cases |
| --- | --- | ---: |
| Auth | `AuthControllerTest` | 6 |
| Auth | `AuthProtectedApiControllerTest` | 2 |
| Auth | `AuthServiceTest` | 11 |
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
| Athlete | `AdminAthleteServiceTest` | 14 |
| Bout | `AdminBoutControllerTest` | 12 |
| Bout | `AdminBoutServiceTest` | 24 |
| Bout | `BoutControllerTest` | 8 |
| Bout | `BoutTest` | 7 |
| Bout | `BoutRepositoryTest` | 1 |
| Bout | `BoutOptimisticLockTest` | 1 |
| Bout | `BoutServiceTest` | 15 |
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
| Ring | `AdminRingServiceTest` | 14 |
| Ring | `RingControllerTest` | 4 |
| Ring | `RingTest` | 5 |
| Ring | `RingRepositoryTest` | 1 |
| Ring | `RingServiceTest` | 12 |
| Ring Manager | `RingManagerControllerTest` | 8 |
| Ring Manager | `RingManagerServiceTest` | 26 |
| Scoring | `JudgeScoreControllerTest` | 7 |
| Scoring | `SupervisorPenaltyControllerTest` | 4 |
| Scoring | `SupervisorResultControllerTest` | 4 |
| Scoring | `SupervisorScoreControllerTest` | 2 |
| Scoring | `RoundScoreTest` | 5 |
| Scoring | `RoundScoreRepositoryTest` | 2 |
| Scoring | `BoutResultRepositoryTest` | 1 |
| Scoring | `JudgeScoreServiceTest` | 14 |
| Scoring | `ScoreQueryServiceTest` | 4 |
| Assignment | `AssignedRingControllerTest` | 1 |
| Assignment | `StaffAssignmentServiceTest` | 4 |
| Database | `DatabaseMigrationIntegrationTest` | 2 |
| Scoring | `SupervisorPenaltyServiceTest` | 11 |
| Scoring | `SupervisorResultServiceTest` | 13 |
| Scoring | `SupervisorJudgeReadinessTest` | 1 |
| Tournament | `AdminTournamentControllerTest` | 9 |
| Tournament | `AdminTournamentServiceTest` | 12 |
| Tournament | `TournamentTest` | 3 |
| Tournament | `TournamentRepositoryTest` | 1 |
| User | `AdminAccountControllerTest` | 10 |
| User | `AdminAccountServiceTest` | 13 |
| User | `AccountTest` | 2 |
| User | `AccountRepositoryTest` | 1 |
| Workflow | `WorkflowConcurrencyIntegrationTest` | 4 |

## Verified User Flows

- Active accounts can log in with BCrypt-hashed credentials, read the current account, log out, and be checked against role-specific protected API groups.
- Audience clients can subscribe to bout update events by tournament and optional ring.
- Audience can read active notice banners, and admin users can manage notice lifecycle per tournament.
- Audience can read tournament schedules, and admin users can manage schedule lifecycle per tournament with optional same-tournament ring and bout references.
- Audience can read tournament home data, current bout information, ring status, and official bout lists/search/detail.
- Judges can submit round scores and retrieve judge-specific scores.
- Judge score submission rejects invalid values, unstarted/future/out-of-range rounds, and closed bouts; same-payload retries remain idempotent and successful submissions publish one event.
- Supervisors can load active assigned rings and official bouts, review score readiness and persisted penalty history, add positive penalties, and confirm results using the authenticated Supervisor actor.
- Supervisor result confirmation rejects missing/draft/incomplete Judge scores, invalid decision/winner combinations, forged actor IDs, and post-confirmation penalty mutations; successful confirmation publishes one event. Confirmed-result corrections require a Supervisor reason, update the audit log, publish `RESULT_CORRECTED`, and keep score submissions immutable.
- Ring managers can list assigned bouts, start only prepared current bouts, start exact next rounds, enter scoring only after configured rounds, cancel eligible pre-start bouts, and advance to a server-selected next bout.
- Duplicate workflow requests return the prior result without duplicate SSE delivery; conflicting state changes and different resubmissions return HTTP 409.
- Concurrent bout starts, identical score submissions, and identical result confirmations persist one final record and publish one event.
- Admin users can manage tournaments, rings, athletes, bouts, notices, and service accounts; account passwords are hashed before storage, service managers can filter accounts by login/name, role, and status, and bout mutations are blocked after operational or scoring state begins.
- Game managers can import bout schedules from CSV or Excel files using the admin bout import endpoint.
- Game managers and service managers can read a tournament's read-only operation summary, including status counts, ring progress, registered judge score submissions, pending results, and bouts in progress for more than 15 minutes.
- Authorized administrators can filter immutable audit logs for operational, admin, and authentication mutations; sensitive credentials and session material are masked, and idempotent workflow retries retain one audit record.
- The assignment API permits Game Managers and Service Managers, but the current UI account lookup requires a Service Manager; this UI limitation is documented rather than treated as a successful Game Manager flow. Service Managers can create and deactivate staff ring assignments; Judge, Supervisor, and Ring Manager screens load assigned rings before scoped operations. Ring Manager uses the assigned ring's `currentBoutId`, command-specific state guards, confirmation steps, and SSE-driven command recalculation; Judge payloads omit `judgeId`.
