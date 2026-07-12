# Audience Web MVP

React and Vite frontend for the public tournament home and official bracket.

## Run Locally

```bash
npm install
npm run dev
```

The Vite development server proxies `/api` requests to `http://localhost:8080` by default. Set `VITE_DEV_PROXY_TARGET` in `.env.local` when the backend runs elsewhere. Set `VITE_API_BASE_URL` when the frontend must call an absolute API origin instead of the Vite proxy.

## API Mapping

- Home: `/api/home`, `/api/notices`, `/api/rings/status`
- Bracket: `/api/bouts`, `/api/bouts/search`, `/api/bouts/{boutId}`
- Live updates: `/api/events/stream?tournamentId={id}`

The audience APIs are public and do not require authentication.

## Verification

```bash
npm test
npm run lint
npm run build
```

The UI handles initial loading, API failure, empty data, dialog loading, SSE reconnecting, duplicate SSE events, and EventSource cleanup.
