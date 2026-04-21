# Backend Auth — Specification

_Feature: M0 — Foundation / Step 1_
_Last updated: 2026-04-21_

---

## Problem Statement

The Ktor backend is a bare skeleton with no database, no authentication, and no user model. No other feature (races, registrations, ranking) can be built until the data layer and identity foundation exist. This spec covers wiring Ktor to Supabase PostgreSQL, creating the `users` table, and adding JWT verification middleware so all subsequent endpoints can identify and authorize callers.

## Goals

- [ ] Ktor connects to Supabase PostgreSQL using Exposed ORM — migrations run on startup
- [ ] `public.users` table exists and is populated on first authenticated request (lazy sync from Supabase Auth)
- [ ] Every protected route verifies the Supabase-issued JWT and rejects unauthenticated callers with 401
- [ ] Authenticated callers can read and update their own profile via REST endpoints
- [ ] Role values (`runner`, `coach`, `admin`) are enforced and accessible to downstream features

## Architecture Decision

**Supabase Auth (Option A):** Supabase Auth (GoTrue) owns the auth lifecycle — email/password, Google, and Apple sign-in are configured in the Supabase dashboard and handled by the client SDK. The Ktor backend **never issues tokens**; it only **verifies** the JWT that Supabase issues, using the project's JWT secret.

```
Client App
  │
  ├── POST supabase.co/auth/v1/token  → Supabase Auth (GoTrue)
  │       returns: { access_token, refresh_token }
  │
  └── GET /users/me  →  Ktor
        Authorization: Bearer <supabase_access_token>
        Ktor verifies signature using SUPABASE_JWT_SECRET
        Ktor lazy-syncs user to public.users if first visit
```

**Consequence:** Google and Apple OAuth flows are configured entirely in Supabase dashboard — zero OAuth code in Ktor.

---

## Out of Scope

| Feature | Reason |
|---|---|
| Token issuance / refresh in Ktor | Supabase Auth owns this |
| Google / Apple OAuth implementation | Handled by Supabase Auth dashboard config |
| Password reset flow | Supabase Auth handles this via email |
| Email verification | Supabase Auth handles this |
| Maintenance mode toggle endpoint | P2 — deferred after core auth ships |
| Admin management UI | Out of v1 scope |
| Role elevation via self-service | Admin role is developer-assigned only |

---

## User Stories

### P1: Database Connection ⭐ MVP — AUTH-01

**User Story:** As a backend developer, I want Ktor to connect to Supabase PostgreSQL on startup so that all features have a working data layer.

**Why P1:** Nothing else works without a DB connection.

**Acceptance Criteria:**

1. WHEN the server starts THEN it SHALL connect to Supabase PostgreSQL using `DATABASE_URL` environment variable
2. WHEN the connection succeeds THEN it SHALL log `"Database connected"` at INFO level
3. WHEN `DATABASE_URL` is missing or invalid THEN it SHALL fail fast with a clear error message and exit

**Independent Test:** Start server with valid `DATABASE_URL` → logs show connection success. Start with invalid URL → server exits with error.

---

### P1: Users Table Migration ⭐ MVP — AUTH-02

**User Story:** As a backend developer, I want the `public.users` table to be created automatically on startup so that user profiles can be stored.

**Why P1:** Required before any user data can be read or written.

**Schema:**
```sql
CREATE TABLE IF NOT EXISTS public.users (
  id          UUID PRIMARY KEY,           -- matches Supabase auth.users.id
  name        TEXT,
  email       TEXT NOT NULL UNIQUE,
  phone       TEXT,
  photo_url   TEXT,
  role        TEXT NOT NULL DEFAULT 'runner'
                CHECK (role IN ('runner', 'coach', 'admin')),
  status      TEXT NOT NULL DEFAULT 'pending'
                CHECK (status IN ('pending', 'approved')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Acceptance Criteria:**

1. WHEN the server starts THEN it SHALL run `CREATE TABLE IF NOT EXISTS public.users` migration
2. WHEN the table already exists THEN it SHALL skip creation without error (idempotent)
3. WHEN `role` is inserted without a value THEN it SHALL default to `'runner'`
4. WHEN `role` is inserted with a value outside `('runner', 'coach', 'admin')` THEN the DB SHALL reject it with a constraint error

**Independent Test:** Drop `public.users`, start server → table exists. Restart server → no error.

---

### P1: JWT Verification Middleware ⭐ MVP — AUTH-03

**User Story:** As a backend developer, I want every protected route to verify the Supabase JWT so that only authenticated users can access protected resources.

**Why P1:** Foundation for all authorization logic.

**How it works:** Supabase signs JWTs with a project-level secret (`SUPABASE_JWT_SECRET`). Ktor verifies the `Authorization: Bearer <token>` header using this secret. The JWT payload contains `sub` (Supabase user UUID) and `email`.

**Acceptance Criteria:**

1. WHEN a request arrives with a valid Bearer token THEN the middleware SHALL extract `sub` and `email` from the JWT claims and attach them to the call context
2. WHEN a request arrives with an expired token THEN it SHALL return `401 Unauthorized` with body `{"error": "token_expired"}`
3. WHEN a request arrives with an invalid/tampered token THEN it SHALL return `401 Unauthorized` with body `{"error": "invalid_token"}`
4. WHEN a request arrives with no Authorization header THEN it SHALL return `401 Unauthorized` with body `{"error": "missing_token"}`
5. WHEN `SUPABASE_JWT_SECRET` env var is missing THEN the server SHALL fail to start with a clear error

**Independent Test:** Call `GET /users/me` with no token → 401. Call with valid token → 200.

---

### P1: Lazy User Profile Sync ⭐ MVP — AUTH-04

**User Story:** As a runner, I want my profile to be automatically created in the system the first time I make a request so that I don't need a separate registration step.

**Why P1:** Supabase Auth creates the identity; Ktor needs to mirror it to `public.users` for app data.

**Acceptance Criteria:**

1. WHEN an authenticated request arrives and no row exists in `public.users` for that `sub` THEN Ktor SHALL insert a new row with `id = sub`, `email` from JWT claims, and `role = 'runner'`
2. WHEN an authenticated request arrives and a row already exists THEN Ktor SHALL NOT insert or update
3. WHEN the insert fails due to a race condition (duplicate key) THEN Ktor SHALL handle the conflict gracefully (upsert or ignore) and continue

**Independent Test:** New Supabase user → call `GET /users/me` → row appears in `public.users` with role `runner`.

---

### P1: Get Own Profile ⭐ MVP — AUTH-05

**User Story:** As a runner, I want to fetch my profile so that the app can display my name, photo, and role.

**Endpoint:** `GET /users/me`

**Acceptance Criteria:**

1. WHEN authenticated user calls `GET /users/me` THEN it SHALL return `200` with their `public.users` row as JSON
2. WHEN unauthenticated user calls `GET /users/me` THEN it SHALL return `401`

**Response shape:**
```json
{
  "id": "uuid",
  "name": "string | null",
  "email": "string",
  "phone": "string | null",
  "photo_url": "string | null",
  "role": "runner | coach | admin",
  "created_at": "ISO8601"
}
```

**Independent Test:** Authenticated call → 200 with correct user data.

---

### P1: Update Own Profile ⭐ MVP — AUTH-06

**User Story:** As a runner, I want to update my name, phone, and photo so that my profile is complete.

**Endpoint:** `PATCH /users/me`

**Updatable fields:** `name`, `phone`, `photo_url` (role and email are NOT user-updatable)

**Acceptance Criteria:**

1. WHEN authenticated user sends valid PATCH body THEN it SHALL update only the provided fields and return `200` with the updated profile
2. WHEN request body includes `role` or `email` THEN it SHALL ignore those fields silently
3. WHEN unauthenticated user calls `PATCH /users/me` THEN it SHALL return `401`
4. WHEN request body is empty `{}` THEN it SHALL return `200` with the unchanged profile (no-op)

**Independent Test:** PATCH `{"name": "João"}` → GET /users/me → name is "João".

---

### P1: Account Approval — Status Field ⭐ MVP — AUTH-09

**User Story:** As a backend developer, I want every new user to start with `status = 'pending'` so that coach approval is required before access is granted.

**Why P1:** Foundation for the approval flow; all other approval stories depend on this column.

**Acceptance Criteria:**

1. WHEN a new user is lazy-synced (AUTH-04) THEN their `status` SHALL be set to `'pending'` by default
2. WHEN a user is approved by a coach THEN their `status` SHALL be updated to `'approved'`
3. WHEN `status` is inserted with a value outside `('pending', 'approved')` THEN the DB SHALL reject it with a constraint error

**Independent Test:** New user lazy-sync → `public.users.status = 'pending'`. After approval → `status = 'approved'`.

---

### P1: Pending Account Access Block ⭐ MVP — AUTH-10

**User Story:** As a runner, when my account is awaiting coach approval, I want to receive a clear message so that I know to wait instead of seeing a broken app.

**Why P1:** Without this gate, pending users access features they shouldn't until the coach acts.

**Acceptance Criteria:**

1. WHEN an authenticated user with `status = 'pending'` calls any protected endpoint THEN it SHALL return `403 Forbidden` with:
   ```json
   {
     "error": "account_pending",
     "message": "Espere o treinador aprovar sua criação de conta"
   }
   ```
2. WHEN an authenticated user with `status = 'approved'` calls a protected endpoint THEN the request SHALL proceed normally
3. WHEN a user with `role = 'coach'` or `role = 'admin'` is created THEN their initial `status` is still `'pending'` until explicitly approved (no automatic bypass)

**Independent Test:** New user (pending) → `GET /users/me` → 403 with PT-BR message. Coach approves → `GET /users/me` → 200.

---

### P1: Coach Views Pending Accounts ⭐ MVP — AUTH-11

**User Story:** As a coach, I want to see all accounts awaiting approval in my dashboard so that I can act on them.

**Endpoint:** `GET /coach/pending-users`

**Access:** Requires `role = 'coach'` or `role = 'admin'`

**Acceptance Criteria:**

1. WHEN a coach calls `GET /coach/pending-users` THEN it SHALL return `200` with a list of all `public.users` rows where `status = 'pending'`
2. WHEN there are no pending users THEN it SHALL return `200` with an empty array `[]`
3. WHEN a runner calls `GET /coach/pending-users` THEN it SHALL return `403 Forbidden` with `{"error": "insufficient_role"}`
4. WHEN an unauthenticated caller hits this endpoint THEN it SHALL return `401`

**Response shape:**
```json
[
  {
    "id": "uuid",
    "name": "string | null",
    "email": "string",
    "phone": "string | null",
    "photo_url": "string | null",
    "role": "runner | coach | admin",
    "status": "pending",
    "created_at": "ISO8601"
  }
]
```

**Independent Test:** 2 pending users exist → `GET /coach/pending-users` → list of 2. Runner calls → 403.

---

### P1: Coach Approves Account ⭐ MVP — AUTH-12

**User Story:** As a coach, I want to approve a pending account so that the runner gains access to the app.

**Endpoint:** `POST /users/{id}/approve`

**Access:** Requires `role = 'coach'` or `role = 'admin'`

**Acceptance Criteria:**

1. WHEN a coach calls `POST /users/{id}/approve` with a valid pending user ID THEN it SHALL set `status = 'approved'` and return `200` with the updated user profile
2. WHEN the user is already `status = 'approved'` THEN it SHALL return `200` with the profile unchanged (idempotent)
3. WHEN the target user ID does not exist THEN it SHALL return `404` with `{"error": "user_not_found"}`
4. WHEN a runner calls this endpoint THEN it SHALL return `403 Forbidden` with `{"error": "insufficient_role"}`
5. WHEN an unauthenticated caller hits this endpoint THEN it SHALL return `401`

**Independent Test:** Coach approves pending user → runner's next request succeeds (200). Second approve call → 200 (idempotent).

---

### P2: Role-Based Route Guard — AUTH-07

**User Story:** As a backend developer, I want to protect certain routes by role so that only coaches/admins can access admin features.

**Why P2:** Not needed until admin endpoints exist (races CRUD), but the guard must be designed now.

**Acceptance Criteria:**

1. WHEN a route requires role `coach` and the caller has role `runner` THEN it SHALL return `403 Forbidden` with `{"error": "insufficient_role"}`
2. WHEN a route requires role `admin` and the caller has role `coach` THEN it SHALL return `403 Forbidden`
3. WHEN the caller's role satisfies the requirement THEN the request SHALL proceed normally

---

### P2: Maintenance Mode — AUTH-08

**User Story:** As the admin developer, I want to flag the system as under maintenance so that I can deploy updates without corrupting live data.

**Why P2:** Required before first production deploy but not before local development.

**Acceptance Criteria:**

1. WHEN maintenance mode is active THEN all non-admin requests SHALL return `503 Service Unavailable` with `{"error": "maintenance"}`
2. WHEN maintenance mode is active THEN requests with `role = 'admin'` SHALL pass through normally
3. WHEN maintenance mode is toggled THEN no server restart is required (runtime flag)

---

## Edge Cases

- WHEN Supabase JWT contains a `sub` that is a valid UUID but has no matching `email` claim THEN sync SHALL use empty string as email fallback and log a warning
- WHEN `public.users` insert fails for any reason other than duplicate key THEN Ktor SHALL return `500` with `{"error": "profile_sync_failed"}`
- WHEN database is unreachable during a request THEN Ktor SHALL return `503` with `{"error": "database_unavailable"}`
- WHEN `PATCH /users/me` body is malformed JSON THEN Ktor SHALL return `400` with `{"error": "invalid_body"}`

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | Yes | PostgreSQL connection string (from Supabase dashboard) |
| `SUPABASE_JWT_SECRET` | Yes | JWT signing secret (from Supabase → Settings → API) |

Both must be present at startup or the server exits.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---|---|---|---|
| AUTH-01 | DB Connection | Design | Pending |
| AUTH-02 | Users Table Migration (+ status column) | Design | Pending |
| AUTH-03 | JWT Verification Middleware | Design | Pending |
| AUTH-04 | Lazy User Profile Sync | Design | Pending |
| AUTH-05 | GET /users/me | Design | Pending |
| AUTH-06 | PATCH /users/me | Design | Pending |
| AUTH-09 | Account Approval — Status Field | Design | Pending |
| AUTH-10 | Pending Account Access Block | Design | Pending |
| AUTH-11 | Coach Views Pending Accounts | Design | Pending |
| AUTH-12 | Coach Approves Account | Design | Pending |
| AUTH-07 | Role-Based Route Guard | — | Pending |
| AUTH-08 | Maintenance Mode | — | Pending |

**Coverage:** 12 total, 0 mapped to tasks, 12 unmapped ⚠️

---

## Success Criteria

- [ ] Server starts and connects to Supabase PostgreSQL (AUTH-01)
- [ ] `public.users` table exists after startup with `status` column (AUTH-02 + AUTH-09)
- [ ] New user lazy-synced → `status = 'pending'` (AUTH-04 + AUTH-09)
- [ ] Pending user calls any protected endpoint → 403 with PT-BR message (AUTH-10)
- [ ] `GET /users/me` with valid Supabase token returns user profile for approved users (AUTH-03 + AUTH-04 + AUTH-05)
- [ ] `GET /users/me` with no token returns 401 (AUTH-03)
- [ ] `PATCH /users/me` updates name/phone/photo_url for approved users (AUTH-06)
- [ ] `GET /coach/pending-users` returns pending list to coach; 403 to runner (AUTH-11)
- [ ] `POST /users/{id}/approve` sets status to approved; runner can access app after (AUTH-12)
- [ ] Role field persists correctly in DB (AUTH-02 + AUTH-04)
