# LifeSpan

Visualize your life in weeks and estimate how much time remains. Enter a date of
birth and a life expectancy, and LifeSpan renders a "life in weeks" grid plus
summary statistics (age, weeks lived, weeks remaining, estimated final year, and
more).

It is a small full-stack TypeScript app:

- **`server/`** — an Express + TypeScript API that computes the statistics
  (`GET /api/lifespan?birthdate=YYYY-MM-DD&expectancy=80`).
- **`web/`** — a React + Vite + TypeScript single-page app that calls the API and
  renders the visualization.

## Prerequisites

- Node.js >= 20 (the repo is developed on Node 22)
- npm >= 10

## Getting started

```bash
npm install       # install all workspace dependencies
npm run dev        # start the API (:3001) and the web app (:5173) together
```

Then open http://localhost:5173. The Vite dev server proxies `/api` requests to
the Express server on port 3001.

## Common commands

| Command | Description |
| --- | --- |
| `npm run dev` | Run the API and web dev servers concurrently. |
| `npm run build` | Type-check/compile the server and build the web bundle. |
| `npm run start` | Run the compiled API server (`server/dist`). |
| `npm run typecheck` | Type-check both workspaces. |
| `npm run lint` | Lint the whole repo with ESLint. |
| `npm test` | Run the server unit and API tests with Vitest. |

## API

`GET /api/health` → `{ "status": "ok" }`

`GET /api/lifespan?birthdate=1990-05-20&expectancy=80` →

```json
{
  "birthdate": "1990-05-20",
  "lifeExpectancyYears": 80,
  "ageYears": 34.03,
  "daysLived": 12432,
  "weeksLived": 1776,
  "totalWeeks": 4174,
  "weeksRemaining": 2398,
  "percentLived": 42.55,
  "estimatedEndDate": "2070-05-08",
  "approxHeartbeats": 1253000000
}
```

Invalid input (bad date format, impossible date, future birthdate, or an
out-of-range expectancy) returns `400` with an `{ "error": "..." }` body.
