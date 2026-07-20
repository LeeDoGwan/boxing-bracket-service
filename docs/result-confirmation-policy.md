# Supervisor Result Confirmation Policy

Last updated: 2026-07-17

This document defines the current server and frontend contract for Supervisor
review and bout result confirmation. It complements [Staff ring assignment](staff-assignment.md)
and [Judge scoring policy](scoring-policy.md).

## Decision Status

| Area | Current status |
| --- | --- |
| Assignment scope | Implemented; an active Supervisor assignment for the bout's ring is required |
| Confirmation actor | Implemented; the authenticated session account is the actor |
| Score readiness | Implemented; every existing score record must be submitted with both values |
| Bout lifecycle | Implemented; confirmation requires an in-progress or scoring bout |
| Result and penalty lock | Implemented; confirmed/finished bouts reject further mutations |
| Judge-count minimum | Odd Judge count confirmed; exact count and missing-submission rule remain open |
| Boxing decision combinations | Broad multi-type result support confirmed; exact catalog remains open |
| Penalty scope | Implemented; round-level entry is stored and the aggregate remains bout-level |

## Confirmation Contract

The existing endpoint remains:

```text
POST /api/supervisor/bouts/{boutId}/result
```

The request sends the decision only. `confirmedBy` remains an optional
compatibility field for older clients, but the backend ignores a matching
value and rejects a value that differs from the authenticated Supervisor.

```json
{
  "winnerSide": "RED",
  "decisionType": "POINTS"
}
```

The server resolves the actor from `AuthSessionContext`, checks active ring
assignment, locks the bout row, checks the existing result, validates the bout
and score state, calculates score and penalty totals, saves the result and
finished bout, then publishes one `RESULT_CONFIRMED` event.

Repeated confirmation with the same winner, decision, and authenticated actor
returns the existing result. A different request returns
`409 RESULT_ALREADY_CONFIRMED`.

## State Rules

| Condition | Result confirmation |
| --- | --- |
| No active Supervisor assignment | `403 RING_ACCESS_DENIED` |
| Scheduled or ready bout | `409 BOUT_NOT_STARTED` |
| In-progress or scoring bout | Eligible after score checks |
| Finished or canceled bout | `409 INVALID_BOUT_STATE` |
| Existing result or confirmed bout | Idempotent same request, otherwise `409 RESULT_ALREADY_CONFIRMED` |
| No score records | `409 SCORES_NOT_READY` |
| Any draft score or missing red/blue value | `409 SCORES_NOT_READY` |
| Existing score records all submitted | Eligible for the next checks |

The implementation does not invent a minimum Judge count. If the tournament
model later gains a required Judge-count field, confirmation must use that
field instead of a hard-coded number.

## Decision and Penalty Rules

- `NONE` and `UNKNOWN` are rejected for final confirmation.
- `RED`, `BLUE`, and `DRAW` are accepted as winner selections.
- `DRAW` is currently accepted only with `POINTS`.
- Score totals are displayed as reference data; the server does not force the
  Supervisor's selected winner to match the higher total.
- Penalty points are positive integers. Zero and negative values return
  `400 INVALID_PENALTY_VALUE`.
- Penalties cannot be added after result confirmation.
- Penalties may be entered with a round reference, while the adjusted total is
  calculated across the whole bout.
- Duplicate penalty reasons are not blocked because the venue policy is not
  yet defined.

The following decisions remain provisional and require boxing association or
venue-official confirmation: score requirements for `WALKOVER`, whether
`UNKNOWN` may be stored for an intermediate state, exceptional decision and
winner combinations, draw handling, penalty calculation semantics, and any
post-confirmation correction workflow.

## Frontend Contract

`SupervisorAssignedPage` loads the authenticated Supervisor's active rings,
then assigned official bouts. It does not use the public bout list as the
permission source. The page:

- shows submitted and draft score counts, score totals, penalty totals, and
  adjusted comparison values;
- blocks result review until the bout is started and all existing scores are
  submitted;
- keeps penalty and result inputs during SSE-driven refetches;
- shows a final confirmation step with bout, winner, decision, score, penalty,
  and mismatch warning details;
- prevents duplicate penalty/result requests;
- removes `createdBy` and `confirmedBy` from write request bodies;
- locks penalty and result controls after `RESULT_CONFIRMED`.

## Error Contract

| Code | HTTP | Meaning |
| --- | ---: | --- |
| `RING_ACCESS_DENIED` | 403 | No active Supervisor assignment |
| `ACTOR_ID_MISMATCH` | 403 | Compatibility actor differs from session |
| `BOUT_NOT_STARTED` | 409 | Bout is not ready for scoring/result action |
| `SCORES_NOT_READY` | 409 | Score records are missing or not submitted |
| `INVALID_RESULT_DECISION` | 400 | `UNKNOWN` or invalid decision |
| `INVALID_WINNER_SELECTION` | 400 | `NONE` or invalid winner/decision combination |
| `INVALID_PENALTY_VALUE` | 400 | Penalty is not a positive integer |
| `PENALTY_NOT_ALLOWED` | 409 | Bout result is already final |
| `RESULT_ALREADY_CONFIRMED` | 409 | Existing result conflicts with the request |

Failed validation occurs before persistence and event publication. Successful
confirmation publishes one `RESULT_CONFIRMED` event after the state mutation.
