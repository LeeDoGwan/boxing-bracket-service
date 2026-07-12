# Testing

Last updated: 2026-07-12

## Latest Verification

- Command: `mvn test`
- Verified at: 2026-07-12T13:59:48+09:00
- Result: 329 passed, 0 failed, 0 errors, 0 skipped
- Test classes: 64
- Runtime profile: `test`
- Test database: H2 in-memory database configured by `src/test/resources/application-test.yml`

## Test Scope

- Auth API and role access policy tests for login, logout, current account lookup, BCrypt password matching, protected route mapping, and interceptor behavior.
- SSE event stream tests for stream subscription, event payloads, subscriber filtering, and broken subscriber cleanup.
- Domain behavior tests for account, tournament, ring, athlete, bout, notice, and scoring models.
- Repository slice tests for tournament, ring, athlete, bout, notice, account, round score, and bout result persistence.
- Service tests for audience home, public bout/ring/notice queries, judge scoring, supervisor scoring, ring manager workflow, admin management flows, bout CSV import, and tournament operation status aggregation.
- Controller tests for health, audience home, public bout/ring/notice APIs, judge APIs, supervisor APIs, ring manager APIs, admin APIs, tournament operation status queries, and 409 workflow conflicts.
- Concurrency tests for duplicate bout starts, score submissions, and result confirmations using `ExecutorService` and `CountDownLatch`.
- Audit tests for action resolution, sensitive-data masking, successful and failed controller mutations, query filters, paging, and idempotent operation fingerprints.
- Frontend tests for utility formatting, notice rotation, ring cards, bout detail loading, bracket search, SSE deduplication/cleanup, judge login, supervisor login, ring manager login, operations manager login, audit log login, tournament admin login, ring admin login, athlete admin login, notice admin login, score submission, penalty creation, result confirmation, ring commands, operations refresh/retry, audit filters/pagination/retry, tournament create/update/delete, ring create/update/delete, athlete search/create/update/delete, notice create/update/delete, and empty states.

## Frontend Verification

- Working directory: `frontend`
- `npm test`: 33 passed across 15 test files
- `npm run lint`: passed with `dist` and `node_modules` excluded
- `npm run build`: passed with Vite production output
- Browser verification covers the public home and bracket routes, API failure and empty states, tournament selection, bracket search, the Judge, Supervisor, Ring Manager, Operations, Audit Log, Tournament Admin, Ring Admin, Athlete Admin, and Notice Admin login routes, and invalid-credential handling. Authenticated score submission, result confirmation, ring commands, operations refresh/retry, audit filtering/pagination, tournament CRUD, ring CRUD, athlete search/CRUD, and notice CRUD are covered by the frontend page tests; the test profile does not seed role accounts or tournament, ring, bout, or audit data.

## Verification Inventory

| Area | Test class | Cases |
| --- | --- | ---: |
| Auth | `AuthControllerTest` | 6 |
| Auth | `AuthProtectedApiControllerTest` | 2 |
| Auth | `AuthServiceTest` | 7 |
| Auth | `AuthInterceptorTest` | 4 |
| Auth | `RoleAccessPolicyTest` | 6 |
| Audit | `AuditActionResolverTest` | 1 |
| Audit | `AuditLogControllerTest` | 1 |
| Audit | `AuditDataSerializerTest` | 2 |
| Audit | `AuditLogAspectTest` | 2 |
| Audit | `AuditLogServiceTest` | 3 |
| Athlete | `AdminAthleteControllerTest` | 10 |
| Athlete | `AthleteTest` | 2 |
| Athlete | `AthleteRepositoryTest` | 1 |
| Athlete | `AdminAthleteServiceTest` | 12 |
| Bout | `AdminBoutControllerTest` | 12 |
| Bout | `AdminBoutServiceTest` | 17 |
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
| Ring | `AdminRingControllerTest` | 11 |
| Ring | `AdminRingServiceTest` | 12 |
| Ring | `RingControllerTest` | 4 |
| Ring | `RingTest` | 5 |
| Ring | `RingRepositoryTest` | 1 |
| Ring | `RingServiceTest` | 11 |
| Ring Manager | `RingManagerControllerTest` | 8 |
| Ring Manager | `RingManagerServiceTest` | 20 |
| Scoring | `JudgeScoreControllerTest` | 7 |
| Scoring | `SupervisorPenaltyControllerTest` | 3 |
| Scoring | `SupervisorResultControllerTest` | 3 |
| Scoring | `SupervisorScoreControllerTest` | 2 |
| Scoring | `RoundScoreTest` | 3 |
| Scoring | `RoundScoreRepositoryTest` | 2 |
| Scoring | `BoutResultRepositoryTest` | 1 |
| Scoring | `JudgeScoreServiceTest` | 9 |
| Scoring | `ScoreQueryServiceTest` | 4 |
| Scoring | `SupervisorPenaltyServiceTest` | 4 |
| Scoring | `SupervisorResultServiceTest` | 5 |
| Tournament | `AdminTournamentControllerTest` | 9 |
| Tournament | `AdminTournamentServiceTest` | 11 |
| Tournament | `TournamentTest` | 2 |
| Tournament | `TournamentRepositoryTest` | 1 |
| User | `AdminAccountControllerTest` | 9 |
| User | `AdminAccountServiceTest` | 11 |
| User | `AccountTest` | 2 |
| User | `AccountRepositoryTest` | 1 |
| Workflow | `WorkflowConcurrencyIntegrationTest` | 3 |

## Verified User Flows

- Active accounts can log in with BCrypt-hashed credentials, read the current account, log out, and be checked against role-specific protected API groups.
- Audience clients can subscribe to bout update events by tournament and optional ring.
- Audience can read active notice banners, and admin users can manage notice lifecycle per tournament.
- Audience can read tournament home data, current bout information, ring status, and official bout lists/search/detail.
- Judges can submit round scores and retrieve judge-specific scores.
- Supervisors can review scores, add penalties, and confirm bout results.
- Ring managers can list bouts, start bouts, start rounds, update bout status, and advance to the next bout.
- Duplicate workflow requests return the prior result without duplicate SSE delivery; conflicting state changes and different resubmissions return HTTP 409.
- Concurrent bout starts, identical score submissions, and identical result confirmations persist one final record and publish one event.
- Admin users can manage tournaments, rings, athletes, bouts, notices, and service accounts; account passwords are hashed before storage.
- Game managers can import bout schedules from CSV files using the admin bout import endpoint.
- Game managers and service managers can read a tournament's read-only operation summary, including status counts, ring progress, registered judge score submissions, pending results, and bouts in progress for more than 15 minutes.
- Authorized administrators can filter immutable audit logs for operational, admin, and authentication mutations; sensitive credentials and session material are masked, and idempotent workflow retries retain one audit record.
