# Audience Web MVP

React and Vite frontend for the public tournament home, official bracket, and judge scoring desk.

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

## API Mapping

- Home: `/api/home`, `/api/notices`, `/api/rings/status`
- Bracket: `/api/bouts`, `/api/bouts/search`, `/api/bouts/{boutId}`
- Auth: `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`
- Judge: `/api/judge/bouts/{boutId}/scores`, `/api/judge/bouts/{boutId}/rounds/{roundNo}/scores`
- Live updates: `/api/events/stream?tournamentId={id}`

Audience APIs are public. Judge score APIs require a `JUDGE` bearer session. A dedicated assignment endpoint is not implemented yet, so the judge desk selects a bout from the tournament's official bout list.

## Verification

```bash
npm test
npm run lint
npm run build
```

The UI handles initial loading, API failure, empty data, dialog loading, SSE reconnecting, duplicate SSE events, EventSource cleanup, judge login, score loading, and submitted score locking.

The test profile does not seed accounts or tournament data. Use an active `JUDGE` account and registered bouts to exercise authenticated score submission end to end.
