# Testing

Last updated: 2026-07-10

## Latest Verification

- Command: `mvn test`
- Verified at: 2026-07-10T06:49:50+09:00
- Result: 256 passed, 0 failed, 0 errors, 0 skipped
- Test classes: 43
- Runtime profile: `test`
- Test database: H2 in-memory database configured by `src/test/resources/application-test.yml`

## Test Scope

- Auth API and role access policy tests for login, logout, current account lookup, protected route mapping, and interceptor behavior.
- Domain behavior tests for account, tournament, ring, athlete, bout, and scoring models.
- Repository slice tests for tournament, ring, athlete, bout, account, and round score persistence.
- Service tests for audience home, public bout/ring queries, judge scoring, supervisor scoring, ring manager workflow, and admin management flows.
- Controller tests for health, audience home, public bout/ring APIs, judge APIs, supervisor APIs, ring manager APIs, and admin APIs.

## Verification Inventory

| Area | Test class | Cases |
| --- | --- | ---: |
| Auth | `AuthControllerTest` | 6 |
| Auth | `AuthServiceTest` | 7 |
| Auth | `AuthInterceptorTest` | 4 |
| Auth | `RoleAccessPolicyTest` | 5 |
| Athlete | `AdminAthleteControllerTest` | 10 |
| Athlete | `AthleteTest` | 2 |
| Athlete | `AthleteRepositoryTest` | 1 |
| Athlete | `AdminAthleteServiceTest` | 12 |
| Bout | `AdminBoutControllerTest` | 11 |
| Bout | `AdminBoutServiceTest` | 15 |
| Bout | `BoutControllerTest` | 7 |
| Bout | `BoutTest` | 7 |
| Bout | `BoutRepositoryTest` | 1 |
| Bout | `BoutServiceTest` | 13 |
| Health | `HealthControllerTest` | 1 |
| Home | `HomeControllerTest` | 2 |
| Home | `HomeServiceTest` | 2 |
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

- Active accounts can log in, read the current account, log out, and be checked against role-specific protected API groups.
- Audience can read tournament home data, current bout information, ring status, and official bout lists/search/detail.
- Judges can submit round scores and retrieve judge-specific scores.
- Supervisors can review scores, add penalties, and confirm bout results.
- Ring managers can list bouts, start bouts, start rounds, update bout status, and advance to the next bout.
- Admin users can manage tournaments, rings, athletes, bouts, and service accounts.
