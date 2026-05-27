# Perform Frontend

A minimal Angular frontend for submitting athlete analysis jobs and viewing results.

---

## Table of Contents

- [Author](#author)
- [Stack](#stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Component Architecture](#component-architecture)
- [API Integration](#api-integration)
- [Proxy Configuration](#proxy-configuration)
- [Deployment](#deployment)
- [Design Decisions](#design-decisions)

---

## Author

**Md Ehteshamul Haque Tamvir**
mtamvir@gmail.com

---

## Stack

| Tool | Version |
|---|---|
| Angular | 19 (standalone components) |
| TypeScript | ^5.8 |
| RxJS | ^7.8 |
| zone.js | ~0.15.0 |
| Angular CLI | ^19 |

---

## Quick Start

### Prerequisites

- Node.js 18+
- Backend running at `http://localhost:8080`

### Install & Run

```bash
npm install
npm start
```

App serves at `http://localhost:4200`.

---

## Project Structure

```
src/
├── app/
│   ├── api.service.ts            # HTTP layer — POST /analysis, GET /analysis/{id}
│   ├── app.component.ts          # Root — owns state, orchestrates child components
│   ├── athlete-input.component.ts # Input form — collects and submits athlete name
│   └── results-panel.component.ts # Results display — purely presentational
├── index.html
├── main.ts                       # Bootstrap with HttpClient provider
└── styles.css
proxy.conf.json                   # Dev proxy — forwards /api to localhost:8080
angular.json                      # Angular CLI config
```

---

## Component Architecture

```
AppComponent  (smart — owns state)
├── AthleteInputComponent  (dumb — emits jobSubmitted)
└── ResultsPanelComponent  (dumb — receives result + status, emits refresh)
```

- `AppComponent` is the single source of truth for `result` and `status`
- Child components communicate only through `@Input` / `@Output` — no shared service state, no `window` events
- `ApiService` is a pure HTTP wrapper with no UI logic

---

## API Integration

Base URL (proxied): `/api/v1` → `http://localhost:8080/api/v1`

| Method | Path | Description |
|---|---|---|
| POST | `/analysis` | Submit a new analysis job |
| GET | `/analysis/{id}` | Fetch current job status and result |

### Flow

1. User enters athlete name and clicks **Run Analysis**
2. `AthleteInputComponent` calls `ApiService.submitAnalysis()` — POST `/analysis`
3. Backend responds `202 Accepted` with a `Location` header pointing to the job URL
4. `AppComponent` receives the location URL via `(jobSubmitted)` output and calls `fetchResult()`
5. `ResultsPanelComponent` shows the current result and a **Check Status** button
6. User clicks **Check Status** to manually re-fetch — no automatic polling

### Error Handling

| Status | Behaviour |
|---|---|
| 400 | "Validation failed" message shown in input form |
| 429 | "Rate limit exceeded" message shown (limit: 20 req/min) |
| 404 | Error state shown in results panel |
| 500 / network | Generic error message shown |

---

## Proxy Configuration

The dev server proxies all `/api` requests to `http://localhost:8080` via `proxy.conf.json`.
This avoids CORS issues during development — no backend CORS config required.

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "changeOrigin": true,
    "secure": false
  }
}
```

---

## Deployment

### 1. Build for Production

```bash
npm run build
```

Output is generated in `dist/perform-frontend/`. This is a static bundle of HTML, JS, and CSS.

### 2. Environment — Backend URL

The dev proxy (`proxy.conf.json`) is only active during `npm start`. For production, the frontend must reach the backend directly.

Update the base URL in `src/app/api.service.ts` before building:

```ts
private base = 'https://your-production-api.com/api/v1';
```

Or configure your web server to proxy `/api` to the backend to avoid CORS.

### 3. Serve the Static Build

**Option A — NGINX**

```nginx
server {
  listen 80;
  root /var/www/perform-frontend;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  # Optional: proxy /api to backend to avoid CORS
  location /api {
    proxy_pass http://localhost:8080;
  }
}
```

Copy the build output:

```bash
cp -r dist/perform-frontend/* /var/www/perform-frontend/
```

**Option B — Node / serve**

```bash
npm install -g serve
serve -s dist/perform-frontend -l 4200
```

**Option C — Docker**

Create a `Dockerfile` at the project root:

```dockerfile
# Stage 1 — build
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2 — serve
FROM nginx:alpine
COPY --from=build /app/dist/perform-frontend /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Build and run:

```bash
docker build -t perform-frontend .
docker run -p 80:80 perform-frontend
```

---

## Design Decisions

- **No automatic polling** — the API is async but the UI does not fire repeated requests automatically. The user controls when to re-check status via the "Check Status" button, staying well within the 20 req/min rate limit.
- **Standalone components** — no `NgModule` boilerplate; each component declares its own imports.
- **Smart/dumb split** — `AppComponent` owns all state; child components are stateless and reusable.
- **Service as thin HTTP layer** — `ApiService` only wraps HTTP calls and returns data. No state, no side effects.
