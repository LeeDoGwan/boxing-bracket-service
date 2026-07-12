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
- Latest `mvn test` result: 329 passed, 0 failed, 0 errors, 0 skipped.
- Covered areas: auth, BCrypt password hashing, role access policy, SSE events, notices, audience home, bracket, bout CSV import, judge scoring, supervisor scoring, ring manager workflow, tournament operation status, administrator audit logging, admin management, workflow concurrency, domain rules, repositories, and health check.
- Audience MVP verification: 9 frontend tests passed, ESLint passed, and the Vite production build passed.
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

CSV upload is available for admin bout import. Excel upload is deferred.

### Audience Web MVP

- React/Vite public home screen.
- Notice rotation, ring cards, current and next bouts, confirmed results, and bout detail dialog.
- Official bracket list with athlete, affiliation, type, status, result, and search.
- SSE refresh with reconnect state, duplicate event protection, and cleanup on unmount.

## API Draft

### Auth

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Audience Home

- `GET /api/events/stream?tournamentId=&ringId=`
- `GET /api/home`
- `GET /api/notices?tournamentId=`
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

- `GET /api/judge/me/bouts/current`
- `POST /api/judge/bouts/{boutId}/rounds/{roundNo}/scores`
- `GET /api/judge/bouts/{boutId}/scores`

### Supervisor

- `GET /api/supervisor/bouts/{boutId}/scores`
- `POST /api/supervisor/bouts/{boutId}/penalties`
- `POST /api/supervisor/bouts/{boutId}/result`

### Ring Manager

- `GET /api/ring-manager/rings/{ringId}/bouts`
- `POST /api/ring-manager/bouts/{boutId}/start`
- `POST /api/ring-manager/bouts/{boutId}/status`
- `POST /api/ring-manager/bouts/{boutId}/rounds/{roundNo}/start`
- `POST /api/ring-manager/rings/{ringId}/next`

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

- Service monitoring UI.
- Server log viewer.
- Excel bracket upload.
- Advanced statistics.
- Advanced user search.
