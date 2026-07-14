# Tournament Operations Web MVP

React and Vite frontend for the public tournament home, official bracket, judge scoring desk, supervisor review desk, ring manager operations desk, and tournament schedule administration.

This application is the `front/` component of the repository. The Spring Boot API is maintained separately under `back/`.

Detailed screen responsibilities, state transitions, API mapping, SSE behavior,
responsive targets, accessibility status, test ownership, and deployment checks
are documented in [frontend wide-frame architecture](../docs/frontend-wide-frame.md).

## Structure

~~~text
src/
  api/         API modules and shared request client
  components/  Shared audience and state UI
  hooks/       Audience data and SSE lifecycle hooks
  pages/       Audience, role, operations, and admin screens
  test/        Vitest and Testing Library setup
~~~

## Run Locally

```bash
npm install
npm run dev
```

The Vite development server proxies `/api` requests to `http://localhost:8080` by default. Set `VITE_DEV_PROXY_TARGET` in `.env.local` when the backend runs elsewhere. Set `VITE_API_BASE_URL` when the frontend must call an absolute API origin instead of the Vite proxy.

## Routes

- `/` - public tournament status and ring view
- `/bracket` - official bracket search
- `/judge` - authenticated judge scoring desk
- `/supervisor` - authenticated score review, penalty history/entry, and result confirmation desk
- `/ring-manager` - authenticated ring bout operations desk
- `/operations` - authenticated tournament operations monitoring desk
- `/audit-logs` - authenticated administrator audit log desk
- `/admin/tournaments` - authenticated tournament CRUD management desk
- `/admin/rings` - authenticated tournament-scoped ring CRUD management desk
- `/admin/athletes` - authenticated searchable athlete CRUD management desk
- `/admin/notices` - authenticated tournament-scoped notice publishing management desk
- `/admin/schedules` - authenticated tournament-scoped schedule CRUD management desk
- `/admin/bouts` - authenticated tournament-scoped bout CRUD and CSV/Excel import desk
- `/admin/accounts` - service-manager-only account CRUD management desk with search and role/status filters
- `/admin/assignments` - game/service manager staff ring assignment desk

## API Mapping

- Home: `/api/home`, `/api/notices`, `/api/schedules`, `/api/rings/status`
- Bracket: `/api/bouts`, `/api/bouts/search`, `/api/bouts/{boutId}`
- Auth: `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`
- Judge: `/api/judge/bouts/{boutId}/scores`, `/api/judge/bouts/{boutId}/rounds/{roundNo}/scores`
- Supervisor: `/api/supervisor/bouts/{boutId}/scores`, `/api/supervisor/bouts/{boutId}/penalties`, `/api/supervisor/bouts/{boutId}/result`
- Ring Manager: `/api/ring-manager/rings/{ringId}/bouts`, `/api/ring-manager/bouts/{boutId}/start`, `/api/ring-manager/bouts/{boutId}/rounds/{roundNo}/start`, `/api/ring-manager/bouts/{boutId}/status`, `/api/ring-manager/rings/{ringId}/next`
- Operations: `/api/admin/operations/status?tournamentId={id}`
- Audit Logs: `/api/admin/audit-logs?tournamentId=&actorAccountId=&actorRole=&actionType=&targetType=&ringId=&boutId=&success=&from=&to=&page=&size=`
- Tournament Admin: `/api/admin/tournaments`, `/api/admin/tournaments/{tournamentId}`
- Ring Admin: `/api/admin/rings?tournamentId={id}`, `/api/admin/rings/{ringId}`
- Athlete Admin: `/api/admin/athletes?keyword=`, `/api/admin/athletes/{athleteId}`
- Notice Admin: `/api/admin/notices?tournamentId={id}`, `/api/admin/notices/{noticeId}`
- Schedule Admin: `/api/admin/schedules?tournamentId={id}`, `/api/admin/schedules/{scheduleId}`
- Bout Admin: `/api/admin/bouts?tournamentId={id}`, `/api/admin/bouts`, `/api/admin/bouts/import`, `/api/admin/bouts/{boutId}`
- Account Admin: `/api/admin/accounts?keyword=&role=&status=`, `/api/admin/accounts/{accountId}`
- Staff assignments: `/api/staff/assignments/rings?tournamentId={id}`, `/api/staff/assignments/rings/{ringId}/bouts`, `/api/admin/assignments`
- Live updates: `/api/events/stream?tournamentId={id}`

Audience APIs are public. Judge, Supervisor, and Ring Manager desks load active assigned rings first and use the assigned-ring bout API. The backend checks the bearer account and active assignment on every scoped request; Judge score submission does not send `judgeId`.
Operations status requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session.
Audit log queries require a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session and retain the server's masked before/after snapshots.
Tournament management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session.
Ring management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session and is scoped to the selected tournament.
Athlete management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session.
Notice management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session and is scoped to the selected tournament.
Schedule management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session and is scoped to the selected tournament. Schedule items can optionally reference a ring and bout from the same tournament.
Bout management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session and is scoped to the selected tournament. Manual bout forms load ring and athlete selector data from the corresponding admin APIs. CSV and Excel import require the documented header row on the first row/sheet; the desk provides a matching CSV template download.
Account management requires a `SERVICE_MANAGER` bearer session. Account lists support login/name keyword search and role/status filters. Password inputs are sent to the server for BCrypt encoding and are never rendered from API responses. Assignment management requires a `GAME_MANAGER` or `SERVICE_MANAGER` bearer session.

## Verification

```bash
npm test
npm run lint
npm run build
```

The UI handles initial loading, API failure, empty data, dialog loading, SSE reconnecting, duplicate SSE events, EventSource cleanup, assigned-ring loading, assignment revocation, Judge score payload ownership, judge/supervisor/ring manager/operations/audit log/tournament admin/ring admin/athlete admin/notice admin/schedule admin/bout admin/account admin login, score loading, submitted score locking, penalty history loading, penalty creation, result confirmation, bout start, round start, status updates, next-bout transitions, operations status refresh, audit filtering/pagination, expandable snapshots, tournament create/update/delete, ring create/update/delete, athlete search/update, notice create/update/delete, schedule create/update/delete, bout create/update/delete, CSV/Excel import, account search/filter/create/update/delete, assignment create/deactivate, and retry states.

The test profile does not seed accounts or tournament data. Use active `JUDGE`, `SUPERVISOR`, `RING_MANAGER`, `GAME_MANAGER`, and `SERVICE_MANAGER` accounts with registered bouts to exercise authenticated desks end to end. Supervisor penalty history is loaded from the API when a bout is selected.
