# Testing

Last updated: 2026-07-11

## Latest Verification

- Command: `mvn test`
- Verified at: 2026-07-11T13:47:36+09:00
- Result: 305 passed, 0 failed, 0 errors, 0 skipped
- Test classes: 56
- Runtime profile: `test`
- Test database: H2 in-memory database configured by `src/test/resources/application-test.yml`

## Test Scope

- Auth API and role access policy tests for login, logout, current account lookup, BCrypt password matching, protected route mapping, and interceptor behavior.
- SSE event stream tests for stream subscription, event payloads, subscriber filtering, and broken subscriber cleanup.
- Domain behavior tests for account, tournament, ring, athlete, bout, notice, and scoring models.
- Repository slice tests for tournament, ring, athlete, bout, notice, account, and round score persistence.
- Service tests for audience home, public bout/ring/notice queries, judge scoring, supervisor scoring, ring manager workflow, admin management flows, bout CSV import, and tournament operation status aggregation.
- Controller tests for health, audience home, public bout/ring/notice APIs, judge APIs, supervisor APIs, ring manager APIs, admin APIs, and tournament operation status queries.

## Verification Inventory

| Area | Test class | Cases |
| --- | --- | ---: |
| Auth | `AuthControllerTest` | 6 |
| Auth | `AuthProtectedApiControllerTest` | 2 |
| Auth | `AuthServiceTest` | 7 |
| Auth | `AuthInterceptorTest` | 4 |
| Auth | `RoleAccessPolicyTest` | 6 |
| Athlete | `AdminAthleteControllerTest` | 10 |
| Athlete | `AthleteTest` | 2 |
| Athlete | `AthleteRepositoryTest` | 1 |
| Athlete | `AdminAthleteServiceTest` | 12 |
| Bout | `AdminBoutControllerTest` | 12 |
| Bout | `AdminBoutServiceTest` | 17 |
| Bout | `BoutControllerTest` | 7 |
| Bout | `BoutTest` | 7 |
| Bout | `BoutRepositoryTest` | 1 |
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
| Ring Manager | `RingManagerServiceTest` | 16 |
| Scoring | `JudgeScoreControllerTest` | 6 |
| Scoring | `SupervisorPenaltyControllerTest` | 3 |
| Scoring | `SupervisorResultControllerTest` | 3 |
| Scoring | `SupervisorScoreControllerTest` | 2 |
| Scoring | `RoundScoreTest` | 3 |
| Scoring | `RoundScoreRepositoryTest` | 1 |
| Scoring | `JudgeScoreServiceTest` | 6 |
| Scoring | `ScoreQueryServiceTest` | 4 |
| Scoring | `SupervisorPenaltyServiceTest` | 4 |
| Scoring | `SupervisorResultServiceTest` | 4 |
| Tournament | `AdminTournamentControllerTest` | 9 |
| Tournament | `AdminTournamentServiceTest` | 11 |
| Tournament | `TournamentTest` | 2 |
| Tournament | `TournamentRepositoryTest` | 1 |
| User | `AdminAccountControllerTest` | 9 |
| User | `AdminAccountServiceTest` | 11 |
| User | `AccountTest` | 2 |
| User | `AccountRepositoryTest` | 1 |

## Verified User Flows

- Active accounts can log in with BCrypt-hashed credentials, read the current account, log out, and be checked against role-specific protected API groups.
- Audience clients can subscribe to bout update events by tournament and optional ring.
- Audience can read active notice banners, and admin users can manage notice lifecycle per tournament.
- Audience can read tournament home data, current bout information, ring status, and official bout lists/search/detail.
- Judges can submit round scores and retrieve judge-specific scores.
- Supervisors can review scores, add penalties, and confirm bout results.
- Ring managers can list bouts, start bouts, start rounds, update bout status, and advance to the next bout.
- Admin users can manage tournaments, rings, athletes, bouts, notices, and service accounts; account passwords are hashed before storage.
- Game managers can import bout schedules from CSV files using the admin bout import endpoint.
- Game managers and service managers can read a tournament's read-only operation summary, including status counts, ring progress, registered judge score submissions, pending results, and bouts in progress for more than 15 minutes.
