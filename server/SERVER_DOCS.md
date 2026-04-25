# PompeiaRunners — Server Documentation

A **Ktor REST API** backend written in Kotlin, running on the **Netty** engine and backed by a **PostgreSQL** database (managed via Supabase). Authentication is handled entirely by verifying **Supabase JWTs**.

---

## How to Run

### Prerequisites

| Requirement | Details |
|---|---|
| JDK | 17 or later |
| Environment variables | `DATABASE_URL` and `SUPABASE_JWT_SECRET` (see below) |
| PostgreSQL | Provided by your Supabase project |

### Required Environment Variables

```
DATABASE_URL        # Full JDBC connection string, e.g.:
                    # jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?user=postgres&password=<pw>&sslmode=require

SUPABASE_JWT_SECRET # Found in Supabase dashboard → Project Settings → API → JWT Secret
```

### Starting the Server

From the **root** of the project (where `gradlew.bat` lives):

```bash
# Windows
.\gradlew.bat :server:run

# macOS / Linux
./gradlew :server:run
```

The server will start on **port 8080** (`http://0.0.0.0:8080`).

To pass the env vars inline instead of setting them in your system:

```bash
.\gradlew.bat :server:run `
  -DDATABASE_URL="jdbc:postgresql://..." `
  -DSUPABASE_JWT_SECRET="your-secret"
```

### Running Tests

```bash
.\gradlew.bat :server:test
```

---

## Architecture Overview

```
Application.kt          ← Entry point: wires everything together
│
├── config/             ← Environment / configuration loading
├── plugins/            ← Cross-cutting concerns (DB, auth, serialization, errors)
├── db/                 ← Database table schema definitions
├── models/             ← Data classes (DB rows + API DTOs)
├── repositories/       ← Data-access layer (all SQL queries live here)
├── routes/             ← HTTP route handlers
└── utils/              ← Shared helpers and extension functions
```

Each request flows through this pipeline:

```
HTTP Request
    → Netty engine
    → JWT Authentication (AuthPlugin)
    → Route handler
        → currentUser() util  (resolves JWT → UserRow, auto-creates user if new)
        → requireRole() guard (optional, for coach/admin routes)
        → UserRepository      (executes DB query via Exposed + HikariCP)
    → JSON serialization (SerializationPlugin)
    → HTTP Response
```

---

## Layer-by-Layer Breakdown

### 1. Entry Point — `Application.kt`

The `main()` function boots an embedded Netty server:

```kotlin
embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
    .start(wait = true)
```

`SERVER_PORT` (value: `8080`) is defined in the `shared` module at `shared/src/commonMain/kotlin/.../Constants.kt`, shared across all targets.

`Application.module()` is the composition root — it loads config and installs every plugin and route group in order:

```kotlin
fun Application.module() {
    val config = AppConfig.load()       // 1. Load env vars
    val repo   = UserRepository()       // 2. Create repository
    configureDatabase(config)           // 3. Connect DB & run migrations
    configureSerialization()            // 4. Enable JSON
    configureStatusPages()              // 5. Global error handling
    configureAuth(config)               // 6. JWT validation
    configureRouting(repo)              // 7. Register routes
}
```

Order matters: the database must be ready before auth, and auth must be ready before routes.

---

### 2. Config — `config/AppConfig.kt`

A simple data class that reads from environment variables (or JVM system properties as a fallback). The server **refuses to start** if either value is missing.

```kotlin
data class AppConfig(
    val databaseUrl: String,
    val jwtSecret: String,
)
```

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | JDBC URL for the PostgreSQL database |
| `SUPABASE_JWT_SECRET` | HMAC-256 secret used to verify all Supabase-issued JWTs |

---

### 3. Plugins — `plugins/`

Plugins are Ktor's mechanism for modular, cross-cutting behaviour. They are installed once and apply to the entire application.

#### `DatabasePlugin.kt`

Connects to PostgreSQL using **HikariCP** (a high-performance connection pool) and the **Exposed** ORM:

- Creates a pool of up to **5 connections**.
- Calls `SchemaUtils.create(UsersTable)` on startup — creates the `public.users` table if it doesn't exist.
- Adds two `CHECK` constraints on `role` (`runner | coach | admin`) and `status` (`pending | approved`) using `IF NOT EXISTS` so it is safe to re-run on every startup.

#### `AuthPlugin.kt`

Installs Ktor's `Authentication` plugin with a `jwt` provider named `"supabase"`:

- Verifies the token signature using **HMAC-256** with the `SUPABASE_JWT_SECRET`.
- On success, wraps the JWT payload in a `JWTPrincipal` available to route handlers.
- On failure, returns a structured JSON error:
  - `{ "error": "missing_token" }` → 401 if no `Authorization` header.
  - `{ "error": "invalid_token" }` → 401 for any other JWT problem.

All protected routes must call `authenticate("supabase") { ... }` in their definition.

#### `SerializationPlugin.kt`

Installs `ContentNegotiation` with `kotlinx.serialization` JSON. This means:
- Responses are automatically serialized to JSON.
- Request bodies are automatically deserialized from JSON.

#### `StatusPagesPlugin.kt`

Global exception handler. Catches typed exceptions thrown anywhere in the request pipeline and returns appropriate HTTP responses instead of letting the server crash:

| Exception | HTTP Status | Response Body |
|---|---|---|
| `JsonConvertException` | 400 Bad Request | `{ "error": "invalid_body" }` |
| `DatabaseUnavailableException` | 503 Service Unavailable | `{ "error": "database_unavailable" }` |
| `ProfileSyncException` | 500 Internal Server Error | `{ "error": "profile_sync_failed" }` |

---

### 4. Database Schema — `db/UsersTable.kt`

Defines the `public.users` table as an Exposed `Table` object:

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key — matches the Supabase Auth user ID |
| `email` | text | Required |
| `name` | text? | Nullable, set by user later |
| `phone` | text? | Nullable |
| `photo_url` | text? | Nullable |
| `role` | text | Default `"runner"`. Allowed: `runner`, `coach`, `admin` |
| `status` | text | Default `"pending"`. Allowed: `pending`, `approved` |
| `created_at` | timestamp | Set at insert time |

---

### 5. Models — `models/`

Three data classes represent data at different stages of the pipeline.

**`UserRow`** — internal representation, used between the repository and the rest of the server. Uses Kotlin/JVM types (`UUID`, `Instant`). Never serialized directly.

**`UserResponse`** — the JSON shape sent back to clients. All fields are strings, making it safe and predictable across platforms. Maps `photoUrl → photo_url` and `createdAt → created_at` via `@SerialName`.

**`UpdateProfileRequest`** — the JSON body accepted by `PATCH /users/me`. All fields are optional (nullable with defaults), so clients can send only the fields they want to change.

The `toResponse()` extension function on `UserRow` converts between the internal and API representations.

---

### 6. Repository — `repositories/UserRepository.kt`

The single data-access class. All database queries are here, wrapped in `newSuspendedTransaction` (Exposed's coroutine-safe transaction API):

| Method | Description |
|---|---|
| `upsertIfAbsent(id, email)` | Inserts a new user row if no row with that UUID exists (`INSERT IGNORE`). Then fetches and returns the row. Used on every authenticated request to auto-provision new users. |
| `findById(id)` | Looks up a single user by UUID. Returns `null` if not found. |
| `updateProfile(id, name, phone, photoUrl)` | Updates only the non-null fields provided. Returns the updated row. |
| `findPendingUsers()` | Returns all users with `status = "pending"`. Used by coaches to manage approval. |
| `approveUser(id)` | Sets `status = "approved"` for the given user UUID. Returns the updated row, or `null` if not found. |

---

### 7. Routes — `routes/`

Route handlers are defined as extension functions on `Route` and registered in `Application.kt`.

#### `UserRoutes.kt`

Both routes require a valid Supabase JWT.

**`GET /users/me`**
- Resolves the caller from the JWT (auto-creating the DB row if this is a first login).
- Returns the caller's `UserResponse`.

**`PATCH /users/me`**
- Reads an `UpdateProfileRequest` body.
- Updates only the provided fields (name, phone, photo_url).
- Returns the updated `UserResponse`.

#### `CoachRoutes.kt`

Both routes require a valid JWT **and** a role of `coach` or `admin`.

**`GET /coach/pending-users`**
- Returns a list of all users with `status = "pending"`.

**`POST /users/{id}/approve`**
- Accepts a user UUID as a path parameter.
- Sets that user's status to `"approved"`.
- Returns the updated `UserResponse`, or 404 if the user doesn't exist.

#### `RoleGuard.kt`

A helper extension function on `ApplicationCall`:

```kotlin
suspend fun ApplicationCall.requireRole(repo: UserRepository, vararg allowed: String): Boolean
```

Resolves the current user, checks their `role` against the allowed list, and responds with `403 Forbidden` (`{ "error": "insufficient_role" }`) if they don't qualify. Route handlers use it as an early-return guard:

```kotlin
if (!call.requireRole(repo, "coach", "admin")) return@get
```

---

### 8. Utils — `utils/CallExtensions.kt`

**`currentUser(repo)`** is an extension on `ApplicationCall` that:

1. Checks if a `UserRow` is already cached in the call's attributes (avoiding duplicate DB hits within the same request).
2. If not cached, reads the `sub` (subject = Supabase user UUID) and `email` from the JWT payload.
3. Calls `repo.upsertIfAbsent()` — this is what auto-creates users on their very first request.
4. Caches the result in the call attributes and returns it.

This design means every route handler can simply call `call.currentUser(repo)` without worrying about whether the user exists yet.

---

## API Reference

All endpoints except `GET /` require an `Authorization: Bearer <supabase-jwt>` header.

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `GET` | `/` | None | — | Health check, returns a greeting string |
| `GET` | `/users/me` | JWT | Any | Get the current user's profile |
| `PATCH` | `/users/me` | JWT | Any | Update name, phone, or photo_url |
| `GET` | `/coach/pending-users` | JWT | coach, admin | List all pending users |
| `POST` | `/users/{id}/approve` | JWT | coach, admin | Approve a user by UUID |

---

## Logging

Configured via `src/main/resources/logback.xml`. Logs are written to stdout in the format:

```
YYYY-MM-dd HH:mm:ss.SSS [thread] LEVEL logger - message
```

The root log level is `TRACE`. Netty and Jetty internals are quieted to `INFO` to reduce noise.

---

## Dependencies (Key Libraries)

| Library | Purpose |
|---|---|
| Ktor Server (Netty) | HTTP server framework and engine |
| Ktor Auth + JWT | JWT validation middleware |
| Ktor ContentNegotiation | JSON request/response serialization |
| Ktor StatusPages | Global exception-to-HTTP-response mapping |
| Exposed (core + jdbc + datetime) | Kotlin SQL ORM / DSL |
| HikariCP | High-performance JDBC connection pool |
| PostgreSQL JDBC Driver | Database driver |
| kotlinx.serialization | JSON serialization for data classes |
| Logback | Logging implementation |
