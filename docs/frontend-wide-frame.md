# Frontend Wide-Frame Architecture Guide

Last updated: 2026-07-14

This is the implementation-oriented wide frame for the frontend in front/.
It connects screens, state, API calls, components, realtime events, tests,
responsive behavior, accessibility, and operations in one place.

## 1. Purpose and scope

The guide is intended to let a new developer understand the main frontend
structure and audience data flow in about ten minutes.

Current boundaries:

- Public audience views: home, notices, ring status, current and next bout,
  confirmed results, schedules, bracket search, and bout detail.
- Authenticated operational views: judge, supervisor, ring manager, operations,
  audit logs, and administration.

This guide records current behavior and marks future work explicitly. It does
not claim that future Judge, Supervisor, Ring Manager, Game Manager, or Service
Manager capabilities are already implemented.

## 2. Application map

front/src/main.jsx mounts the React application. front/src/App.jsx owns route
selection and the tournamentId query parameter. A missing or invalid positive
integer currently falls back to tournament 1. AppHeader renders the shared
navigation and tournament selector.

~~~mermaid
flowchart TD
    Browser[Browser] --> Main[main.jsx]
    Main --> App[App.jsx]
    App --> Header[AppHeader]
    App --> Public[Audience routes]
    App --> Staff[Role routes]
    App --> Admin[Admin routes]
    Public --> Hooks[Audience hooks]
    Hooks --> Api[API modules]
    Api --> Client[API client]
    Client --> Backend[Backend API]
    Hooks --> Shared[Shared UI components]
~~~

Text fallback:

Browser -> main.jsx -> App.jsx -> route page -> hook or API module -> backend -> page and shared components

### 2.1 Route and screen responsibilities

| Screen | Route | Users | Purpose | Current APIs | Main components | State coverage |
| --- | --- | --- | --- | --- | --- | --- |
| Audience home | / | Audience | Notices, rings, current/next bout, confirmed results, schedule | /api/home, /api/notices, /api/rings/status, /api/events/stream | AudienceHome, NoticeCarousel, RingCard, ScheduleList, BoutDetailDialog, StatePanel | Loading, error, empty sections, connected/reconnecting/offline |
| Bracket | /bracket | Audience | List, search, highlight, and inspect bracket status/result | /api/bouts, /api/bouts/search | BracketPage, StatePanel | Loading, error, empty, search, selected row |
| Bout detail | Home dialog | Audience | Inspect a selected audience bout | /api/bouts/{boutId} | BoutDetailDialog, StatePanel | Loading, error, empty detail |
| Judge | /judge | Judge | Enter round scores | Judge score read and submit APIs | JudgePage, StatePanel | Session/role guard, loading, error, action feedback |
| Supervisor | /supervisor | Supervisor | Review scores, penalties, and confirm result | Supervisor score, penalty, result APIs | SupervisorPage, StatePanel | Session/role guard, loading, error, action feedback |
| Ring manager | /ring-manager | Ring Manager | Operate ring bouts, starts, rounds, status, and next bout | Ring manager list and action APIs | RingManagerPage, StatePanel | Session/role guard, loading, error, action feedback |
| Operations | /operations | Game Manager, Service Manager | Monitor tournament operations | /api/admin/operations/status | OperationsPage, StatePanel | Session/role guard, loading, error |
| Audit logs | /audit-logs | Game Manager, Service Manager | Search operational audit records | /api/admin/audit-logs | AuditLogPage, StatePanel | Session/role guard, loading, error, empty |
| Tournament admin | /admin/tournaments | Game Manager, Service Manager | Tournament CRUD | /api/admin/tournaments | AdminTournamentPage, StatePanel | Guard, list, form, action errors |
| Ring admin | /admin/rings | Game Manager, Service Manager | Tournament ring CRUD | /api/admin/rings | AdminRingPage, StatePanel | Guard, list, form, action errors |
| Athlete admin | /admin/athletes | Game Manager, Service Manager | Athlete search and CRUD | /api/admin/athletes | AdminAthletePage, StatePanel | Guard, search, form, action errors |
| Notice admin | /admin/notices | Game Manager, Service Manager | Notice publishing CRUD | /api/admin/notices | AdminNoticePage, StatePanel | Guard, list, form, action errors |
| Schedule admin | /admin/schedules | Game Manager, Service Manager | Schedule CRUD | /api/admin/schedules | AdminSchedulePage, StatePanel | Guard, list, form, action errors |
| Bout admin | /admin/bouts | Game Manager, Service Manager | Bout CRUD and import | /api/admin/bouts, /api/admin/bouts/import | AdminBoutPage, StatePanel | Guard, list, form, import, errors |
| Account admin | /admin/accounts | Service Manager | Account and role CRUD | /api/admin/accounts | AdminAccountPage, StatePanel | Guard, list, form, action errors |

Authentication and role checks are performed by role pages with sessionStorage.
Public pages do not require a session.

## 3. State model

| State | Meaning | UI expression | Current status |
| --- | --- | --- | --- |
| INITIAL | No request has started | Initial loading panel | Partial; page state starts before the first effect |
| LOADING | Request is in flight with no usable data | StatePanel loading message | Implemented |
| SUCCESS | Data is usable | Normal screen content | Implemented |
| EMPTY | Successful response has no records | Empty message or section | Implemented in bracket and list sections |
| ERROR | Request failed | Error and retry or action feedback | Implemented |
| STALE | Existing data remains while refresh failed | Existing content plus error/connection indicator | Partial; audience reload preserves current data |
| RECONNECTING | SSE is retrying or unavailable | Live connection status | Implemented by useBoutEventStream |
| OFFLINE | EventSource is unavailable in the environment | Offline status | Implemented by the hook |

Important transitions:

| Situation | Current behavior | Rule |
| --- | --- | --- |
| First load | Audience hook starts loading; page shows a loading panel | Do not show empty before the first response |
| Partial audience failure | Promise.all fails as one reload; existing data is preserved | Keep usable data and expose retry |
| SSE disconnect | Stream becomes reconnecting; content remains | Never clear last confirmed API data |
| Event received | Event is parsed and deduplicated, then audience reload runs | No browser refresh |
| Duplicate event | Key uses event type, bout id, round, and occurrence time | No duplicate reload |
| Invalid tournamentId | App normalizes it to 1 | Future validation must retain a usable default |
| Result pending | Only API-confirmed data is displayed | Never infer a winner from an event payload |
| Server maintenance | StatePanel or action feedback displays the API failure | Preserve existing data where possible |

Every new screen must define loading, empty, error, and refresh behavior
independently. A loading state is not an empty state.

## 4. API mapping

API modules are under front/src/api/ and use the shared behavior in
front/src/api/client.js. This table maps screen behavior, not every DTO field.

### 4.1 Audience

| API | Trigger | UI affected | Failure/retry | SSE behavior |
| --- | --- | --- | --- | --- |
| GET /api/home?tournamentId= | Initial load and reload | Current/next bout, results, schedule, fallback sections | StatePanel when no home; retain data on refresh failure | Called by event callback |
| GET /api/notices?tournamentId= | Initial load and reload | NoticeCarousel | Empty section or page error | Called by event callback |
| GET /api/rings/status?tournamentId= | Initial load and reload | RingCard list | Retain current ring data | Called by event callback |
| GET /api/schedules?tournamentId= | API capability for schedule flows | Schedule data when directly requested | Page retry | No direct audience stream call today |
| GET /api/bouts?tournamentId= | Bracket mount | Bout rows and status/result | Bracket retry panel | No bracket stream subscription |
| GET /api/bouts/search?tournamentId=&keyword= | Bracket submit | Filtered rows | Search feedback and prior result where supported | No direct stream |
| GET /api/bouts/{boutId} | Audience selection | BoutDetailDialog | Dialog loading/error | No direct dialog stream |
| GET /api/events/stream?tournamentId= | Audience mount | Connection status | Reconnecting or offline status | Named events handled by hook |

The audience reload is centralized in useAudienceData and currently re-fetches
home, notices, and ring status together. The event payload is not rendered as
the source of truth.

### 4.2 Authenticated and operations

| Screen | Read APIs | Write APIs | Session and role |
| --- | --- | --- | --- |
| Judge | /api/judge/bouts/{boutId}/scores?judgeId= | /api/judge/bouts/{boutId}/rounds/{roundNo}/scores | boxing.judge.session, JUDGE |
| Supervisor | /api/supervisor/bouts/{id}/scores, /penalties | /penalties, /result | boxing.supervisor.session, SUPERVISOR |
| Ring manager | /api/ring-manager/rings/{ringId}/bouts | start, status, round start, next-bout actions | boxing.ring-manager.session, RING_MANAGER |
| Operations | /api/admin/operations/status?tournamentId= | None in current page | boxing.operations.session, GAME_MANAGER or SERVICE_MANAGER |
| Audit logs | /api/admin/audit-logs with filters | None in current page | boxing.operations.session, GAME_MANAGER or SERVICE_MANAGER |

### 4.3 Administration

| Screen | Collection API | Actions |
| --- | --- | --- |
| Tournament admin | /api/admin/tournaments | Create, update, delete |
| Ring admin | /api/admin/rings?tournamentId= | Create, update, delete |
| Athlete admin | /api/admin/athletes?keyword= | Search, create, update, delete |
| Notice admin | /api/admin/notices?tournamentId= | Create, update, delete |
| Schedule admin | /api/admin/schedules?tournamentId= | Create, update, delete |
| Bout admin | /api/admin/bouts?tournamentId= | Create, update, delete, multipart import |
| Account admin | /api/admin/accounts with filters | Create, update, delete |

When a field changes, update the API module, page form, state behavior, and
page test together. Do not depend on an undocumented response field.

## 5. SSE design

front/src/hooks/useBoutEventStream.js owns the EventSource lifecycle. It
registers named events and the default message event. Invalid JSON is ignored.

| Event | Affected screen | Current reload | UI effect | Duplicate/visibility rule |
| --- | --- | --- | --- | --- |
| BOUT_STARTED | Audience home | useAudienceData.reload | Ring and current/next bout update after API response | Dedupe; show connection state, not raw payload |
| BOUT_STATUS_CHANGED | Audience home | useAudienceData.reload | Ring status and bout sections update | Same |
| ROUND_STARTED | Audience home | useAudienceData.reload | Current bout and ring status update | Same |
| NEXT_BOUT_READY | Audience home | useAudienceData.reload | Next bout and schedule update | Same |
| SCORE_SUBMITTED | Audience home | useAudienceData.reload | Results update after backend state | Do not show unconfirmed result |
| RESULT_CONFIRMED | Audience home | useAudienceData.reload | Results, ring, and current/next sections update | Same |

Current behavior is a full audience data reload for each recognized event. It
does not refresh the browser, calculate a winner, or create duplicate timers.
Cleanup calls EventSource.close().

SSE invariants:

- Keep the last usable data while reconnecting.
- Re-fetch from the backend instead of trusting event payloads.
- Make delivery idempotent.
- Create one stream per audience page and close it on unmount.
- Announce connection state, not every raw event.
- If partial reloads are introduced, document and test each affected API.

## 6. Component structure

| Location | Responsibility | State/API boundary | Test |
| --- | --- | --- | --- |
| src/App.jsx | Route and tournament query selection | Route state | Page routing tests |
| src/components/AppHeader.jsx | Shared navigation and tournament selector | Navigation/query state | Indirect page coverage |
| src/components/StatePanel.jsx | Loading, empty, and error presentation | loading, error, retry | Page coverage |
| src/components/NoticeCarousel.jsx | Notice rotation and controls | Active index | NoticeCarousel.test.jsx |
| src/components/RingCard.jsx | One ring status and bout summary | Presentation and selection callback | RingCard.test.jsx |
| src/components/ScheduleList.jsx | Schedule list and empty state | Presentation | ScheduleList.test.jsx |
| src/components/BoutDetailDialog.jsx | Selected bout detail request and dialog | Selected id and request state | BoutDetailDialog.test.jsx |
| src/hooks/useAudienceData.js | Home, notice, and ring loading | Audience data and reload | Audience page coverage |
| src/hooks/useBoutEventStream.js | EventSource, parsing, dedupe, cleanup | connected, reconnecting, offline | useBoutEventStream.test.js |
| src/api/client.js | URL, headers, JSON parsing, bearer token, errors | Shared transport | API page coverage |
| src/api/audience.js | Audience endpoints and stream URL | Endpoint contract | Audience/bracket coverage |
| src/pages/AudienceHome.jsx | Compose public home | Hook data, selected bout, stream status | Audience page tests |
| src/pages/BracketPage.jsx | Load, search, select bracket | List, keyword, request state | BracketPage.test.jsx |

Role and admin pages follow the same page-to-API-module pattern. Keep domain
actions in src/api/ and rendering/local interaction state in the owning page or
shared component.

## 7. Data flow

~~~mermaid
flowchart LR
    Query[Route parameter] --> Page[Page component]
    Page --> Service[Hook or API service]
    Service --> Backend[Backend API]
    Backend --> Mapping[Response mapping]
    Mapping --> State[Component state]
    State --> Render[UI render]
~~~

Initial flow text:

Route parameter -> page component -> hook/page state -> API service -> backend API -> response mapping -> component state -> UI

~~~mermaid
flowchart LR
    Source[EventSource] --> Parser[Event parser]
    Parser --> Type[Event type and dedupe key]
    Type --> Reload[Related API reload]
    Reload --> State[Audience state]
    State --> Render[Page re-render]
~~~

Realtime flow text:

EventSource -> event parser -> event type -> related API reload -> state -> re-render

## 8. Responsive wide frame

The target is four viewport bands. Current CSS has an explicit mobile rule at
max-width 760px; desktop is fluid and capped by the page shell rather than
having explicit 768, 1024, and 1440 media queries.

| Viewport | Target | Current status | Acceptance rule |
| --- | --- | --- | --- |
| Mobile 360 | One column, vertical rings, core bout information first, no horizontal scroll | Implemented by mobile layout and overflow protection | No clipping in header, notices, rings, results, or schedule |
| Tablet 768 | Two-column content where space allows | Partial; desktop fluid layout begins above 760px | No desktop row may escape the viewport |
| Desktop 1024 | Two or three scannable columns | Partial; capped fluid shell | Current/next bout and ring details remain readable |
| Wide 1440 | Centered max-width content and side margins | Implemented by 1180px page shell | Do not stretch long rows or shrink type for density |

Rules:

- Use min-width 0 on flexible children and retain a constrained page shell.
- Stack ring cards and core information on narrow screens.
- Do not introduce ordinary page-level horizontal scrolling.
- Keep controls and fixed-format cards dimensionally stable.
- Add explicit tablet/wide breakpoints when a layout depends on them.
- Verify 360, 768, 1024, and 1440 pixels before changing the contract.

## 9. Accessibility frame

| Requirement | Status | Implementation or next check |
| --- | --- | --- |
| Keyboard focus | Implemented | Visible focus outline for buttons, inputs, and links |
| Text labels | Implemented | Forms and navigation have accessible names |
| Status not conveyed by color alone | Implemented in core audience/bracket labels | Keep red/blue text alongside color |
| Touch target size | Partial | Verify new controls at mobile widths |
| Heading hierarchy | Partial | Preserve page heading order |
| Error announcement | Implemented | role alert or aria-live where appropriate |
| Realtime announcement | Partial | Connection state is announced; raw event spam is avoided |
| Decorative icons | Partial | Hide non-content icons from assistive technology |
| Search focus | Partial | Preserve visible focus and selected-result navigation |
| Contrast | Partial | Re-check colors when changing the palette |
| Carousel disruption | Implemented for current notices | Keep manual controls and low-disruption updates |
| Dialog semantics | Implemented in BoutDetailDialog | Keep name, close, and focus behavior tested |

Prefer semantic headings, buttons, and links before adding ARIA. A live status
should announce a meaningful state change, not every transport event.

## 10. Test map

The current frontend baseline is 19 test files and 47 passing tests.

| Area | Actual files | Current assertions | Additional coverage |
| --- | --- | --- | --- |
| Shared audience components | components/BoutDetailDialog.test.jsx, NoticeCarousel.test.jsx, RingCard.test.jsx, ScheduleList.test.jsx | Detail loading/error/content, notice controls, ring rendering, schedule states | Keyboard and dialog focus assertions |
| Realtime hook | hooks/useBoutEventStream.test.js | Event registration, parsing, dedupe, state, cleanup | Reconnection timing and multi-event reload |
| Audience and bracket | pages/AudienceHome.test.jsx, BracketPage.test.jsx | Composition, loading/error, live status, list/search/selection | Stale data and invalid query |
| Role pages | pages/JudgePage.test.jsx, SupervisorPage.test.jsx, RingManagerPage.test.jsx | Session guard, workflows, API feedback | Expired token and duplicate submit |
| Operations | pages/OperationsPage.test.jsx, AuditLogPage.test.jsx | Protected views, filters, empty/error | Responsive table and live status checks |
| Administration | pages/AdminTournamentPage.test.jsx, AdminRingPage.test.jsx, AdminAthletePage.test.jsx, AdminNoticePage.test.jsx, AdminSchedulePage.test.jsx, AdminBoutPage.test.jsx, AdminAccountPage.test.jsx | CRUD, filters, import, role restrictions, errors | Field validation and retry-after-failure |
| Utilities | utils.test.js | Shared formatting and utility behavior | Add coverage with each normalization change |

Frontend verification:

~~~text
cd front
npm test
npm run lint
npm run build
~~~

Backend regression verification:

~~~text
cd back
mvn test
~~~

For screen changes, update the owning page test and shared component or hook
test. For SSE changes, test event type, dedupe, reload callback, reconnecting,
and cleanup.

## 11. Operations and deployment

| Item | Contract |
| --- | --- |
| API base URL | VITE_API_BASE_URL; empty means same-origin relative requests |
| Local API proxy | VITE_DEV_PROXY_TARGET, default http://localhost:8080 |
| Development | npm run dev from front/ |
| Production build | npm run build from front/; output is Vite dist/ |
| Verification | Test, lint, and build before publishing |

Vite environment values are read at build time. Do not put secrets in frontend
environment variables.

Deployment checks:

- Configure the API origin or same-origin reverse proxy.
- Configure CORS when VITE_API_BASE_URL is absolute.
- Ensure the reverse proxy forwards SSE without buffering.
- Ensure auth cookies or bearer-token flows match the deployed origin.
- Configure cache headers so an old application shell does not hide a new build.
- Configure history fallback for /bracket and role routes.
- Record the frontend build version with the backend deployment.
- Keep browser, API error, and SSE reconnect logs available without secrets.

Minimum post-deploy smoke:

1. Open / with a valid tournament and confirm notices, rings, and schedule.
2. Open /bracket, search for a bout, and select a result.
3. Change a bout in an operational flow and confirm audience SSE status.
4. Confirm a result and verify the audience result updates from the API.
5. Check public screens at 360 pixels for horizontal overflow.
6. Verify an API failure has a usable error and retry path.

## 12. Future role expansion

| Role | Route or scope | Reuse | New work required |
| --- | --- | --- | --- |
| Judge | /judge | Judge session, score API, StatePanel | Assignment selection, score validation, conflicts |
| Supervisor | /supervisor | Session, score/penalty/result APIs | Result policy and audit feedback |
| Ring Manager | /ring-manager | Ring session and action APIs | Ring assignment and transition safeguards |
| Game Manager | Operations and admin routes | Operations session, admin modules, header | Tournament workflow and permissions |
| Service Manager | Account/admin routes | Session and CRUD patterns | Account lifecycle and elevated audit visibility |

For each new role, document route, session key, role, APIs, state behavior,
realtime needs, responsive behavior, and tests before marking it complete.

## 13. Maintenance rules

- Update this guide when a route, API module, SSE event, shared component, or
  responsive contract changes.
- Keep README.md, docs/design.md, docs/testing.md, and front/README.md linked
  to this guide instead of duplicating detailed frontend architecture.
- Mark partial or future behavior explicitly.
- Preserve the baseline of 19 frontend test files and 47 tests unless coverage
  is intentionally changed.
- Run link checks, frontend test/lint/build, and backend tests before commit.
