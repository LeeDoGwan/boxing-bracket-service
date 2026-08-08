# Product Decisions

Last updated: 2026-08-08

This document records product and UX decisions confirmed after reviewing the
requirements for the boxing tournament service. It is the source for frontend
workflow assumptions until a venue or boxing association supplies a more
specific rule.

## Confirmed Decisions

| Area | Decision | UX or implementation consequence |
| --- | --- | --- |
| Audience access | Public screens require no login | Keep audience home and bracket immediately readable; do not expose protected controls |
| Staff assignment | Judge, Supervisor, and Ring Manager assignments are ring-scoped | Staff first selects or receives an assigned ring, then works from that ring's bouts |
| Judge maximum | Each athlete's round score has a maximum of 10 | Enforce the range in backend and browser validation; show the 0-10 range beside the inputs |
| Judge count | Each tournament uses either 3 or 5 Judges | Store the count on the tournament; every active assigned Judge must submit each started round before Supervisor confirmation |
| Score editing | A submitted Judge score cannot be edited | Make score submission final; corrections apply to the confirmed bout result only |
| Penalties | Penalties may be entered per round but affect the bout total | Store the round reference and show both round history and bout-level adjusted totals |
| Result authority | Supervisor directly selects the winner; a tie is resolved by Supervisor | Show calculated totals as a recommendation, then require explicit Supervisor confirmation |
| Result types | A result type records how the bout ended, such as points decision, KO, referee stoppage, withdrawal, disqualification, walkover, or draw | Keep the code catalog extensible; the association-specific final labels remain a policy item |
| Device priority | Staff screens are tablet-first and also usable on mobile | Use large touch targets, compact two-column tablet layouts, and one-column mobile fallbacks |
| Offline mode | Offline operation is outside the service purpose | Preserve reconnect, stale-data, retry, and conflict feedback; do not build offline writes |
| Tournament scope | Current target is management of one tournament | Keep the current tournament context simple; defer multi-tournament ownership UX |
| Athlete scope | Athletes are managed per tournament | Admin forms and search should remain tournament-scoped |
| Deployment shape | MVP runs on one backend server | Keep process-local session/event assumptions for MVP; introduce shared session/event infrastructure before horizontal scaling |
| Bout numbering | Bout numbers are generated automatically per tournament | Hide manual number input; the server assigns the next number for create/import and preserves it on update |

## Operator Login UX

The public audience experience should remain uncluttered without making staff
access undiscoverable or relying on a secret URL.

1. Keep only audience links in the public header.
2. Provide a low-emphasis `스태프 로그인` link in the footer or account area;
   do not label it `관리자 로그인`, because Judges and other staff also use it.
3. Use one shared `/staff/login` screen. Determine the role after login and
   route the account to its assigned workspace.
4. If a staff member opens a protected route directly, redirect to the shared
   login and preserve the intended return path.
5. After authentication, show the role, tournament, assigned ring, connection
   state, and logout action in a shared staff header.
6. Expose only role-appropriate navigation. Administrative screens are visible
   only to Game Manager or Service Manager sessions.
7. On logout, session expiry, or revoked assignment, remove staff navigation
   immediately and explain the next action in Korean.

This keeps the public UI focused while preserving a predictable entry point for
authorized staff and a clear distinction between discoverability and security.

## Implementation Notes

The following confirmed targets are implemented in the current baseline:

- Maximum score `10` is enforced by backend validation and browser validation.
- Penalties accept an optional round reference, while totals remain bout-level;
  the frontend sends the selected round for new entries.
- Role-specific entry points are consolidated into `/staff/login`, with a
  shared session, protected routes, role-aware navigation, and logout cleanup.
- The public header no longer exposes a raw tournament ID control; the current
  one-tournament context is carried by the public navigation and staff session.
- Bout creation and CSV/XLS/XLSX import omit `boutNumber`. The server locks the
  tournament row, assigns the next positive number, and the database enforces
  uniqueness for `(tournament_id, bout_number)`.

Still open after venue confirmation:

- The association-specific display labels and allowed winner combinations for
  each exceptional result type. A result type is the bout-ending method, not a
  Judge score or an additional score category.
- Whether the venue later needs more result types than the current provisional
  codes (`POINTS`, `KO`, `RSC`, `ABD`, `DSQ`, and `WALKOVER`).

Implemented result correction rule:

- Submitted Judge scores remain immutable.
- A confirmed result may be corrected only by the authenticated Supervisor.
- The correction request requires a reason of 1-500 characters.
- The same reason is included in the correction audit record, and a
  `RESULT_CORRECTED` event refreshes connected staff and audience screens.
