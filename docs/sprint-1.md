# Sprint 1 Scope

Source: ChatGPT project `복싱 대회 어드바이스 서비스`, `기능.txt`-based analysis on 2026-07-07.

## Objective

Validate the core tournament operation loop:

`register bracket -> show current ring bout -> submit judge scores -> confirm supervisor result -> publish audience result`

## Completion Criteria

One ring can run one bout end to end:

1. A bout exists with red and blue athletes.
2. The bout is visible on the audience home and full bracket.
3. Three judges can submit scores for the bout.
4. A supervisor can review scores and confirm the result.
5. The confirmed result appears on the audience home and full bracket.

## Verification Status

- Test documentation: [Testing](testing.md)
- Latest `mvn test` result: 354 passed, 0 failed, 0 errors, 0 skipped.
- Covered areas: auth, BCrypt password hashing, role access policy, SSE events, notices, schedules, audience home, bracket, bout CSV/Excel import, judge scoring, supervisor scoring, ring manager workflow, tournament operation status, administrator audit logging, admin management, workflow concurrency, domain rules, repositories, and health check.
- Audience, Judge, Supervisor, Ring Manager, Operations, Audit Log, Tournament Admin, Ring Admin, Athlete Admin, Notice Admin, Schedule Admin, Bout Admin, and Account Admin MVP verification: 47 frontend tests passed across 19 test files, ESLint passed, and the Vite production build passed.
- Workflow safety: bout, ring, round score, and result aggregates use optimistic versions; mutating workflow paths use transaction-scoped row locks, idempotent retries, DB unique constraints, and post-commit SSE delivery.

## Screens

### Audience Home

- Notice banner.
- Current bout card by ring.
- Current bout detail.
- Next bout preview.
- Confirmed result display.

### Full Bracket

- Official bout list.
- Bout number.
- Red and blue athlete information.
- Match type.
- Bout status.
- Result.
- Search by athlete name, affiliation, or bout number.

### Judge

- Assigned current bout.
- Red and blue athlete information.
- Round score input.
- Score submission.
- Submission status.

### Supervisor

- Current bout information.
- Judge score overview.
- Penalty input.
- Winner selection.
- Result confirmation.

### Ring Manager

- Assigned ring current bout.
- Bout start.
- Bout status change.
- Next bout transition.

For sprint 1, ring manager functionality can stay minimal and focus on status changes.

Duplicate requests return the existing state when the payload is equivalent. Conflicting requests return HTTP 409 and do not publish a duplicate SSE event.

### Game Manager

- Manual bout registration.
- Manual bout update.
- Manual bout deletion.
- Athlete input.
- Read tournament operation status by ring, result confirmation, and registered judge score submission state.

CSV and Excel upload are available for admin bout import.

### Audience Web MVP

- React/Vite public home screen.
- Notice rotation, ring cards, current and next bouts, confirmed results, and bout detail dialog.
- Official bracket list with athlete, affiliation, type, status, result, and search.
- SSE refresh with reconnect state, duplicate event protection, and cleanup on unmount.
- Tournament schedule list for bouts, breaks, meals, performances, and events.

### Judge Web MVP

- Judge login with session persistence and role validation.
- Tournament bout selection, red/blue athlete context, round score entry, and submitted-state locking.
- Authenticated Judge API calls for score query and submission. Assignment-specific API remains deferred.

### Supervisor Web MVP

- Supervisor login with role validation and session persistence.
- All-judge score review, score totals, penalty creation, and winner/decision selection.
- Result confirmation with published-state feedback and persisted penalty history retrieval.

### Ring Manager Web MVP

- Ring manager login with role validation and session persistence.
- Direct ring ID loading with scheduled bout list and current bout selection.
- Bout start, round start, status update, and next-bout transition commands.
- Ring assignment API is deferred; the desk loads a ring directly from the ring manager API.

### Operations Web MVP

- Game manager and service manager login with role validation and session persistence.
- Tournament-wide bout status counts and ring-by-ring current/next bout monitoring.
- Judge submission progress, pending result, and stalled bout exception lists.
- Manual refresh and retry states for the read-only operations status API.

### Audit Log Web MVP

- Game manager and service manager login with role validation and session reuse.
- Filters for tournament, actor, role, action, target, ring, bout, result, and time range.
- Newest-first paginated audit history with success/failure display.
- Expandable before/after snapshots using the server's masked audit payloads.

### Tournament Admin Web MVP

- Game manager and service manager login with role validation and session reuse.
- Tournament list with create, update, delete, and status management.
- Date and location editing with save, delete, refresh, and error states.

### Ring Admin Web MVP

- Game manager and service manager login with role validation and session reuse.
- Tournament-scoped ring list with current bout context.
- Ring create, update, delete, status management, refresh, and error states.

### Athlete Admin Web MVP

- Game manager and service manager login with role validation and session reuse.
- Athlete search by name or affiliation.
- Athlete create, update, delete, refresh, and error states.

### Notice Admin Web MVP

- Game manager and service manager login with role validation and session reuse.
- Tournament-scoped notice list with active/inactive publishing state and display order.
- Notice create, update, delete, refresh, and error states.

### Schedule Admin Web MVP

- Game manager and service manager login with role validation and session reuse.
- Tournament-scoped schedule list with type, status, time range, ring, and related bout fields.
- Schedule create, update, delete, refresh, and error states.

### Bout Admin Web MVP

- Game manager and service manager login with role validation and session reuse.
- Tournament-scoped bout list with ring, athlete IDs, schedule order, status, and event flag.
- Manual bout forms use tournament ring and athlete selector data from the admin APIs.
- Bout create, update, delete, and CSV/Excel import with success/error feedback.
- CSV template download uses the same required header order as the import API.

### Account Admin Web MVP

- Service manager-only login with role validation and session reuse.
- Account list with login ID, name, role, and active status; password data is never displayed.
- Account search by login ID/name and exact role/status filters.
- Account create, update, delete, refresh, and error states with server-side BCrypt handling.

## API Draft

### Auth

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Audience Home

- `GET /api/events/stream?tournamentId=&ringId=`
- `GET /api/home`
- `GET /api/notices?tournamentId=`
- `GET /api/schedules?tournamentId=`
- `GET /api/rings/status`
- `GET /api/rings/{ringId}/current-bout`

### Bracket

- `GET /api/bouts`
- `GET /api/bouts/search`
- `GET /api/bouts/{boutId}`
- `POST /api/admin/bouts`
- `POST /api/admin/bouts/import`
- `PUT /api/admin/bouts/{boutId}`
- `DELETE /api/admin/bouts/{boutId}`

### Judge

- Judge current-bout assignment API is deferred; the web desk selects from `GET /api/bouts?tournamentId=`.
- `POST /api/judge/bouts/{boutId}/rounds/{roundNo}/scores`
- `GET /api/judge/bouts/{boutId}/scores`

### Supervisor

- `GET /api/supervisor/bouts/{boutId}/scores`
- `GET /api/supervisor/bouts/{boutId}/penalties`
- `POST /api/supervisor/bouts/{boutId}/penalties`
- `POST /api/supervisor/bouts/{boutId}/result`

### Ring Manager

- `GET /api/ring-manager/rings/{ringId}/bouts`
- `POST /api/ring-manager/bouts/{boutId}/start`
- `POST /api/ring-manager/bouts/{boutId}/status`
- `POST /api/ring-manager/bouts/{boutId}/rounds/{roundNo}/start`
- `POST /api/ring-manager/rings/{ringId}/next`
- Ring assignment API is deferred; the web desk selects a ring by ID.

### Operations

- `GET /api/admin/operations/status?tournamentId=`

### Audit Log

- `GET /api/admin/audit-logs?tournamentId=&actorAccountId=&actorRole=&actionType=&targetType=&ringId=&boutId=&success=&from=&to=&page=&size=`

### Tournament Admin

- `GET /api/admin/tournaments`
- `POST /api/admin/tournaments`
- `PUT /api/admin/tournaments/{tournamentId}`
- `DELETE /api/admin/tournaments/{tournamentId}`

### Ring Admin

- `GET /api/admin/rings?tournamentId=`
- `POST /api/admin/rings`
- `PUT /api/admin/rings/{ringId}`
- `DELETE /api/admin/rings/{ringId}`

### Athlete Admin

- `GET /api/admin/athletes?keyword=`
- `POST /api/admin/athletes`
- `PUT /api/admin/athletes/{athleteId}`
- `DELETE /api/admin/athletes/{athleteId}`

### Notice Admin

- `GET /api/admin/notices?tournamentId=`
- `POST /api/admin/notices`
- `PUT /api/admin/notices/{noticeId}`
- `DELETE /api/admin/notices/{noticeId}`

### Schedule Admin

- `GET /api/admin/schedules?tournamentId=`
- `GET /api/admin/schedules/{scheduleId}`
- `POST /api/admin/schedules`
- `PUT /api/admin/schedules/{scheduleId}`
- `DELETE /api/admin/schedules/{scheduleId}`

### Bout Admin

- `GET /api/admin/bouts?tournamentId=`
- `POST /api/admin/bouts`
- `POST /api/admin/bouts/import`
- `PUT /api/admin/bouts/{boutId}`
- `DELETE /api/admin/bouts/{boutId}`

### Account Admin

- `GET /api/admin/accounts`
- `GET /api/admin/accounts?keyword=&role=&status=`
- `POST /api/admin/accounts`
- `PUT /api/admin/accounts/{accountId}`
- `DELETE /api/admin/accounts/{accountId}`

### Game Manager

- `GET /api/admin/tournaments`
- `GET /api/admin/tournaments/{tournamentId}`
- `POST /api/admin/tournaments`
- `PUT /api/admin/tournaments/{tournamentId}`
- `DELETE /api/admin/tournaments/{tournamentId}`
- `GET /api/admin/rings?tournamentId=`
- `GET /api/admin/rings/{ringId}`
- `POST /api/admin/rings`
- `PUT /api/admin/rings/{ringId}`
- `DELETE /api/admin/rings/{ringId}`
- `GET /api/admin/athletes?keyword=`
- `GET /api/admin/athletes/{athleteId}`
- `POST /api/admin/athletes`
- `PUT /api/admin/athletes/{athleteId}`
- `DELETE /api/admin/athletes/{athleteId}`
- `GET /api/admin/bouts?tournamentId=`
- `GET /api/admin/bouts/{boutId}`
- `POST /api/admin/bouts`
- `POST /api/admin/bouts/import`
- `PUT /api/admin/bouts/{boutId}`
- `DELETE /api/admin/bouts/{boutId}`
- `GET /api/admin/notices?tournamentId=`
- `GET /api/admin/notices/{noticeId}`
- `POST /api/admin/notices`
- `PUT /api/admin/notices/{noticeId}`
- `DELETE /api/admin/notices/{noticeId}`
- `GET /api/admin/accounts`
- `GET /api/admin/accounts/{accountId}`
- `POST /api/admin/accounts`
- `PUT /api/admin/accounts/{accountId}`
- `DELETE /api/admin/accounts/{accountId}`
- `GET /api/admin/operations/status?tournamentId=`
- `GET /api/admin/audit-logs?tournamentId=&actorAccountId=&actionType=&page=&size=`

## Deferred

- Server log viewer.
- Advanced statistics.
- Advanced user search.
