# Product Requirements

Source: ChatGPT project `복싱 대회 어드바이스 서비스`, `기능.txt`-based analysis on 2026-07-07.

## Goal

Build a digital operations service for boxing tournaments that replaces paper-based bracket, bout status, judge scoring, supervisor confirmation, and result publishing flows.

The service must keep the on-site UX simple because tournament staff may not be comfortable with complex digital tools.

## MVP Scope

### Audience

- View notices on the home screen.
- View current bout cards by ring.
- Open current bout details.
- View round scores when available.
- Preview next and later bouts.
- View event schedules such as breaks, lunch, performances, and non-bout events.
- Search official brackets by athlete name, affiliation, bout type, or bout number.

### Judge

- Sign in as a judge.
- View the currently assigned bout.
- Check red and blue athlete information.
- Submit red and blue scores by round.
- See submission status.

### Supervisor

- Sign in as a supervisor.
- View current bout information.
- Review judge scores.
- Enter referee penalties.
- Confirm the final winner and result.
- Publish confirmed results to audience home and bracket views.

### Ring Manager

- Sign in as a ring manager.
- View assigned ring status.
- Start a bout.
- Change bout status.
- Move to the next bout.
- Check ring events.

### Game Manager

- Register, update, and delete bouts.
- Register and update athlete information.
- Manage basic user data.
- Manually create bracket data in MVP. CSV and Excel bout import are available.
- Read tournament operation status, including ring progress, pending results, and registered judge score submission state.
- Read filtered administrator audit logs for tournament operations and administrative changes.

### Service Manager

- Manage operational accounts.
- Read tournament operation status across rings and stalled bouts. Server log viewing remains deferred.
- Read administrator audit logs across the service.
- This role is lower priority for MVP implementation.

## Core User Flow

The first working loop is:

1. Game manager registers bracket data.
2. Ring manager starts the current bout.
3. Audience home shows the current ring and bout.
4. Judges submit round scores.
5. Supervisor reviews scores, applies penalties, and confirms the result.
6. Audience home and full bracket reflect the confirmed result.

## Workflow Safety

- A repeated request with the same payload must return the existing bout, round, score, or result state without a duplicate SSE event.
- A request that conflicts with the current bout state or changes an already submitted score/result must return HTTP 409.
- Bout, ring, round score, and bout result updates use optimistic versions. Workflow mutations also lock the affected bout or ring for the transaction.
- A judge can have one score per bout and round, and a bout can have one confirmed result. These constraints are enforced in the database.

## Initial Data Model

### Tournament

- `id`
- `name`
- `location`
- `startDate`
- `endDate`
- `status`: `PREPARING`, `IN_PROGRESS`, `COMPLETED`
- `createdAt`
- `updatedAt`

### Ring

- `id`
- `tournamentId`
- `name`
- `status`: `WAITING`, `ACTIVE`, `CLOSED`
- `currentBoutId`
- `createdAt`
- `updatedAt`

### Athlete

- `id`
- `name`
- `affiliation`
- `gender`
- `weightClass`
- `createdAt`
- `updatedAt`

### Bout

- `id`
- `tournamentId`
- `ringId`
- `boutNumber`
- `matchType`
- `redAthleteId`
- `blueAthleteId`
- `status`: `SCHEDULED`, `WAITING`, `IN_PROGRESS`, `SCORING`, `CONFIRMED`, `COMPLETED`
- `currentRound`
- `totalRounds`
- `scheduledOrder`
- `winnerSide`: `RED`, `BLUE`, `DRAW`, `NONE`
- `resultStatus`
- `isEventBout`
- `startedAt`
- `endedAt`
- `createdAt`
- `updatedAt`

### RoundScore

- `id`
- `boutId`
- `roundNo`
- `judgeId`
- `redScore`
- `blueScore`
- `status`: `DRAFT`, `SUBMITTED`
- `submittedAt`
- `createdAt`
- `updatedAt`

### Penalty

- `id`
- `boutId`
- `targetSide`: `RED`, `BLUE`
- `penaltyPoint`
- `reason`
- `createdBy`
- `createdAt`

### BoutResult

- `id`
- `boutId`
- `redTotalScore`
- `blueTotalScore`
- `redPenaltyTotal`
- `bluePenaltyTotal`
- `winnerSide`: `RED`, `BLUE`, `DRAW`
- `decisionType`
- `confirmedBy`
- `confirmedAt`

### User

- `id`
- `loginId`
- `passwordHash` (stored as a one-way BCrypt hash)
- `name`
- `role`: `JUDGE`, `SUPERVISOR`, `GAME_MANAGER`, `RING_MANAGER`, `SERVICE_MANAGER`
- `status`: `ACTIVE`, `INACTIVE`
- `createdAt`
- `updatedAt`

### AuditLog

- `id`
- `tournamentId`
- `actorAccountId`, `actorUsername`, `actorRole`
- `actionType`, `targetType`, `targetId`
- `ringId`, `boutId`
- `beforeData`, `afterData` (JSON with sensitive fields masked)
- `ipAddress`, `userAgent`
- `success`, `failureReason`
- `createdAt`

### Assignment

- `id`
- `tournamentId`
- `userId`
- `role`
- `ringId`
- `boutId`
- `active`
- `createdAt`
- `updatedAt`

### Notice

- `id`
- `tournamentId`
- `title`
- `content`
- `displayOrder`
- `active`
- `createdAt`
- `updatedAt`

### ScheduleItem

- `id`
- `tournamentId`
- `ringId`
- `type`: `BOUT`, `BREAK`, `LUNCH`, `PERFORMANCE`, `EVENT`
- `title`
- `startTime`
- `endTime`
- `relatedBoutId`
- `status`: `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`
- `createdAt`
- `updatedAt`

## Open Questions

- How many rings can one tournament have?
- Are judges assigned per bout or per ring?
- Can judges edit submitted scores before supervisor confirmation?
- Are penalties per round or per bout?
- Does the supervisor manually confirm the winner, or should the system auto-calculate and allow override?
- Which result types are required beyond red win and blue win, such as draw, withdrawal, disqualification, or injury stoppage?
- Can the audience pages be public without login?
- Should judge screens target phones, tablets, or both?
- Should score input use buttons or numeric inputs?
- What refresh strategy should the audience home use first: polling, SSE, or WebSocket?
- Is offline or poor-network support required at tournament venues?
- Should athletes be managed per tournament or as reusable master data?
- Should bout numbers be generated automatically or entered manually?
- Can confirmed results be modified, and who can approve changes?
- Which account-to-tournament assignment rule should restrict game-manager audit access?
