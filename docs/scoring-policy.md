# Judge Scoring Policy

Last updated: 2026-07-16

This document describes the score submission rules currently enforced by the
backend and the corresponding Judge screen behavior. It separates generic
safety rules from boxing-association rules that still require venue confirmation.

## Decision Status

| Area | Status |
| --- | --- |
| Non-negative whole-number input | Implemented |
| Zero score | Implemented and accepted |
| Bout and round lifecycle checks | Implemented |
| One submitted score per Judge, bout, and round | Implemented |
| Same-payload retry idempotency | Implemented |
| Maximum score | Confirmed as 10; backend/frontend enforcement pending |
| Ten-point-must rule | Provisional; validation required from boxing association officials |
| Tie-round policy | Provisional; validation required from boxing association officials |
| Deduction interaction with Judge input | Provisional; validation required from boxing association officials |

## Applied Rules

The existing `POST /api/judge/bouts/{boutId}/rounds/{roundNo}/scores` endpoint
enforces the following rules:

1. Both red and blue scores are required integers greater than or equal to zero.
2. A bout must be `IN_PROGRESS` or `SCORING` and must not be finished or result-confirmed.
3. A bout in `SCHEDULED` or `READY` rejects scores with `BOUT_NOT_STARTED`.
4. When `totalRounds` is configured, the requested round must be within that range.
5. The requested round must be started. Previous unsubmitted rounds remain valid; future rounds are rejected.
6. A submitted score cannot be changed. Resubmitting the same red/blue pair returns the stored score without another save or SSE event.
7. A different payload for a submitted score returns `SCORE_ALREADY_SUBMITTED`.
8. The authenticated Judge assignment remains the authorization source; the frontend does not send `judgeId`.
9. Validation failures do not publish `SCORE_SUBMITTED`. Successful persistence publishes one event after the existing transaction/event flow.

## Confirmed Target Rules

The product decision is that each red or blue athlete score is an integer from
0 through 10. A submitted score remains immutable. The event's Judge count is
odd, so a tie is expected to be uncommon, but the exact count and any
association-specific tie handling remain open.

## State Matrix

| Bout state | Previous unsubmitted round | Current round | Future round |
| --- | --- | --- | --- |
| `SCHEDULED`, `READY` | Reject: `BOUT_NOT_STARTED` | Reject: `BOUT_NOT_STARTED` | Reject: `BOUT_NOT_STARTED` |
| `IN_PROGRESS`, `SCORING` | Allow | Allow | Reject: `ROUND_NOT_STARTED` |
| `FINISHED` or result-confirmed | Reject: `INVALID_BOUT_STATE` | Reject: `INVALID_BOUT_STATE` | Reject: `INVALID_BOUT_STATE` |
| `CANCELED` | Reject: `INVALID_BOUT_STATE` | Reject: `INVALID_BOUT_STATE` | Reject: `INVALID_BOUT_STATE` |

An out-of-range round returns `INVALID_ROUND_NUMBER` before score persistence.

## Error Contract

| Message | HTTP status | Meaning |
| --- | ---: | --- |
| `INVALID_SCORE_VALUE` | 400 | Negative or non-whole score |
| `INVALID_ROUND_NUMBER` | 400 | Round is less than one or outside configured bout range |
| `BOUT_NOT_STARTED` | 409 | Bout has not entered scoring workflow |
| `ROUND_NOT_STARTED` | 409 | Requested future round has not started |
| `INVALID_BOUT_STATE` | 409 | Bout is finished, confirmed, or canceled |
| `SCORE_ALREADY_SUBMITTED` | 409 | Submitted score differs from the retry payload |
| `BOUT_ACCESS_DENIED` / `RING_ACCESS_DENIED` | 403 | Judge assignment does not cover the target |

## Frontend Contract

`JudgeAssignedPage` keeps the current selected bout and local unsubmitted values
while an SSE invalidation refreshes REST data. It highlights the current round,
disables future and submitted rounds, validates non-negative whole numbers,
shows a confirmation step, and prevents duplicate confirmation requests. A
successful response locks the round; a server error maps the stable message to
a short operator-facing message without exposing stack traces.

Browser validation is convenience only. The backend remains the final authority
for score values, bout state, round state, and assignment access.

## Required Venue Decisions

Before enforcing association-specific rules, confirm:

- minimum per-athlete round scores;
- whether every round must include a ten-point score;
- whether tied rounds are valid;
- whether deductions are entered separately and how they affect final totals;
- whether late submission of an earlier round is allowed at the venue;
- whether score entry timing changes for stoppages or exceptional bouts.

Any confirmed rule must be added to this matrix, the service tests, the Judge
screen tests, and the API error contract before implementation.
