# Staff Ring Assignment

Last updated: 2026-07-16

## Purpose

Staff assignment is the server-side scope for Judge, Supervisor, and Ring
Manager desks. A desk must receive an active assignment for the tournament and
ring before it can read or mutate that ring's bouts.

## Decisions

| Decision | Current rule | Status |
| --- | --- | --- |
| Assignment unit | `account + tournament + ring + role` | Implemented; validate at field operation |
| Judge granularity | Ring, not individual bout | Confirmed |
| Multiple rings | One account may have multiple ring rows in one tournament | Implemented |
| Role changes | An account role must match the assignment role | Implemented |
| Deactivation | Existing scores and audit history remain; subsequent requests are denied | Implemented |
| Manager ownership | Existing Game Manager admin policy remains; tournament ownership records are deferred | Provisional |
| Operator realtime | One selected-ring SSE stream per staff screen; events invalidate REST data and do not authorize writes | Implemented |

`SERVICE_MANAGER` has global administrative authority. `GAME_MANAGER` and
`SERVICE_MANAGER` retain the existing `/api/admin/**` policy. A separate
tournament ownership model is deferred until the product defines how a manager
receives tournament scope.

## Entity and constraints

`staff_assignments` stores scalar IDs to match the rest of the backend model.
The table, unique constraint, and indexes are created by the V1 Flyway
migration documented in [Database migration policy](database-migration.md).

Required fields are `id`, `version`, `accountId`, `tournamentId`, `ringId`,
`role`, `active`, `createdAt`, and `updatedAt`. MariaDB rejects duplicate
`(account_id, tournament_id, ring_id)` rows, including inactive rows. Account,
tournament, and ring existence, ring/tournament ownership, active account
status, and role compatibility are checked before insertion.

## APIs

All responses use the common `{ success, data, message }` envelope.

| Method | Path | Purpose | Access |
| --- | --- | --- | --- |
| `GET` | `/api/admin/assignments?tournamentId=&accountId=&role=&active=` | Filter assignments | Game Manager, Service Manager |
| `POST` | `/api/admin/assignments` | Create a ring assignment | Game Manager, Service Manager |
| `PUT` | `/api/admin/assignments/{assignmentId}/active` | Activate/deactivate | Game Manager, Service Manager |
| `GET` | `/api/staff/assignments/rings?tournamentId=` | Current account's active rings | Judge, Supervisor, Ring Manager |
| `GET` | `/api/staff/assignments/rings/{ringId}/bouts` | Assigned ring's official bouts | Judge, Supervisor, Ring Manager |

Duplicate creation returns `409 ASSIGNMENT_ALREADY_EXISTS`. Invalid role or
account state is `400`; a missing account, tournament, ring, or assignment is
`404`; a ring from another tournament returns `400 RING_TOURNAMENT_MISMATCH`.

## Access decision

1. `AuthInterceptor` resolves the bearer token and stores the authenticated
   `AuthSession` in the request context.
2. The service loads the bout or ring from the server-side ID.
3. For ring-scoped roles it checks an active row matching `accountId`,
   `tournamentId`, `ringId`, and `role`.
4. A missing row raises `403 RING_ACCESS_DENIED`; a missing bout remains `404`.
5. Score submission uses the authenticated Judge account ID. A supplied
   `judgeId` is optional for compatibility and is rejected when it differs
   from the authenticated account.

Staff screens load assigned rings first and then use the assigned-ring bout
endpoint. The public tournament bout API is not the staff permission API.

## Existing data and audit

Deactivation takes effect on the next request. Persisted round scores,
penalties, results, and audit entries are not deleted or rewritten. Supervisors
and service managers retain their existing read permissions for persisted
records; an unassigned Judge or Ring Manager cannot continue the scoped action.

Assignment create and active-state changes are audited as
`ASSIGNMENT_CREATED` and `ASSIGNMENT_UPDATED` with target type `ASSIGNMENT`.
Sensitive credentials and session values are not written.

## Frontend behavior

Judge and Supervisor flow is login, assigned-ring load, ring selection, ring
bout load, bout selection, then score/review actions. One ring is selected
automatically; multiple rings show a selector. Empty assignments, revoked
access (`403`), empty bouts, and expired sessions have explicit states.

Judge score lifecycle validation is documented separately in [Judge scoring
policy](scoring-policy.md). Assignment scope remains the authorization boundary;
score value and bout/round lifecycle validation are applied after that boundary.

Supervisor result and penalty lifecycle rules are documented separately in
[Supervisor result confirmation policy](result-confirmation-policy.md). The
Supervisor write services resolve `createdBy` and `confirmedBy` from the
authenticated session; compatibility request fields are not a permission
source.

Ring Manager uses the same assigned-ring selector. The assigned-ring response
also exposes the server-owned `currentBoutId`; the routed page uses it to
disable commands for a different selected bout. State-specific commands,
confirmation steps, round sequencing, and next-bout ordering are defined in
[Bout state transition policy](bout-state-transition-policy.md).
Judge, Supervisor, and Ring Manager screens subscribe to
`GET /api/events/stream?tournamentId={tournamentId}&ringId={selectedRingId}`
when an active ring is selected. The stream is an invalidation signal only:
relevant events trigger debounced assigned-bout/detail refetches, while the
current form state remains intact. A ring change closes the previous
`EventSource`; unmount and disabled/no-ring states close or avoid the connection.
SSE failure preserves the current data and does not block write APIs. Audience
SSE behavior remains unchanged.
