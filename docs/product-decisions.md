# Product Decisions

Last updated: 2026-07-17

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
| Judge count | The number of Judges is odd, so a tie is unlikely | Do not hard-code a tie rule or minimum count until the exact venue count is confirmed |
| Score editing | A submitted score cannot be edited | Make submission final in the UI and provide a separate correction process if required later |
| Penalties | Penalties may be entered per round but affect the bout total | Store the round reference and show both round history and bout-level adjusted totals |
| Result authority | Supervisor directly selects the winner | Present calculated totals as reference and require an explicit Supervisor confirmation |
| Result types | The product must support multiple win and draw variants | Keep the decision catalog extensible; the exact catalog remains open |
| Device priority | Staff screens are tablet-first and also usable on mobile | Use large touch targets, compact two-column tablet layouts, and one-column mobile fallbacks |
| Offline mode | Offline operation is outside the service purpose | Preserve reconnect, stale-data, retry, and conflict feedback; do not build offline writes |
| Tournament scope | Current target is management of one tournament | Keep the current tournament context simple; defer multi-tournament ownership UX |
| Athlete scope | Athletes are managed per tournament | Admin forms and search should remain tournament-scoped |
| Bout numbering | Bout numbers are generated automatically | Admin creation/import should preview the next number and prevent manual collisions |

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

The following are confirmed product targets but are not all implemented yet:

- Add maximum-score validation to the backend and frontend.
- Add round-level penalty reference while retaining bout-level totals.
- Consolidate role-specific login forms into the shared staff login flow.
- Replace the raw public tournament ID control with a tournament context that
  is meaningful to the current one-tournament experience.
- Define and implement the complete exceptional decision-type catalog after
  venue confirmation.
