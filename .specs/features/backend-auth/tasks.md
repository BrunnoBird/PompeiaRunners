# Backend Auth — Tasks

**Design**: `.specs/features/backend-auth/design.md`
**Spec**: `.specs/features/backend-auth/spec.md`
**Status**: Draft

---

## Execution Plan

### Phase 1: Gradle Setup (Sequential)

Must complete before any compilation.

```
T1
```

### Phase 2: Core Structures (Parallel after T1)

All independent of each other — pure Kotlin classes.

```
     ┌→ T2 (AppConfig)
     ├→ T3 (UsersTable)
T1 ──┼→ T4 (Models)
     ├→ T6 (SerializationPlugin)
     └→ T7 (StatusPagesPlugin)
```

### Phase 3: Plugins (Mixed, after Phase 2 subsets)

T5 needs T2+T3. T8 needs T2 only (starts as soon as T2 done).

```
T2+T3 ──→ T5 (DatabasePlugin)
T2    ──→ T8 (AuthPlugin)      ← can start in parallel with T5
```

### Phase 4: Repository (Sequential, after T5)

Needs the DB connection live + table + models.

```
T3+T4+T5 ──→ T9 (UserRepository)
```

### Phase 5: Shared Helper (Sequential, after T8+T9)

`currentUser()` helper needed by both T10 and T11 — define it first.

```
T8+T9 ──→ T10 (CallExtensions + AccountStatusPlugin)
```

### Phase 6: Role Guard (Sequential, after T10)

RoleGuard reuses `currentUser()` from T10's CallExtensions.kt.

```
T10 ──→ T11 (RoleGuard)
```

### Phase 7: Routes (Parallel, after T10+T11)

Both routes depend on T10 (pipeline) and T11 (role guard in CoachRoutes).

```
T10+T11 ──┬→ T12 (UserRoutes)   [P]
           └→ T13 (CoachRoutes)  [P]
```

### Phase 8: Wire Application.kt (Sequential, after all)

```
T2+T5+T6+T7+T8+T10+T12+T13 ──→ T14 (Application.kt)
```

---

## Task Breakdown

### T1: Add Gradle Dependencies

**What**: Add all new library versions and dependencies required by backend-auth to the version catalog and server build script.
**Where**:
- `gradle/libs.versions.toml` — add versions + library aliases
- `server/build.gradle.kts` — add dependencies + apply serialization plugin
**Depends on**: None
**Reuses**: Existing `ktor = "3.4.1"` version catalog entry
**Requirement**: AUTH-01, AUTH-02, AUTH-03, AUTH-05, AUTH-06

**New versions to add** (verify latest stable at task time on Maven Central):
- `exposed` (exposed-core, exposed-jdbc, exposed-kotlin-datetime)
- `postgresql` (JDBC driver)
- `hikaricp`

**New library aliases to add**:
```toml
exposed-core = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed" }
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc", version.ref = "exposed" }
exposed-kotlin-datetime = { module = "org.jetbrains.exposed:exposed-kotlin-datetime", version.ref = "exposed" }
postgresql = { module = "org.postgresql:postgresql", version.ref = "postgresql" }
hikaricp = { module = "com.zaxxer:HikariCP", version.ref = "hikaricp" }
ktor-server-auth = { module = "io.ktor:ktor-server-auth-jvm", version.ref = "ktor" }
ktor-server-auth-jwt = { module = "io.ktor:ktor-server-auth-jwt-jvm", version.ref = "ktor" }
ktor-server-content-negotiation = { module = "io.ktor:ktor-server-content-negotiation-jvm", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json-jvm", version.ref = "ktor" }
ktor-server-status-pages = { module = "io.ktor:ktor-server-status-pages-jvm", version.ref = "ktor" }
```

**New dependencies in `server/build.gradle.kts`**:
```kotlin
plugins {
    kotlin("plugin.serialization") version libs.versions.kotlin.get()  // add if not present
}

dependencies {
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
}
```

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] All version entries added to `libs.versions.toml`
- [ ] All library aliases added to `libs.versions.toml`
- [ ] All `implementation(...)` entries added to `server/build.gradle.kts`
- [ ] `kotlin("plugin.serialization")` applied in server module
- [ ] Gate check passes: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T2: AppConfig.kt [P]

**What**: Implement `AppConfig` — loads and validates required env vars at startup; fails fast with clear message if any are missing.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/config/AppConfig.kt`
**Depends on**: T1
**Reuses**: stdlib only
**Requirement**: AUTH-01, AUTH-03

**Interface**:
```kotlin
data class AppConfig(
    val databaseUrl: String,
    val jwtSecret: String,
) {
    companion object {
        fun load(): AppConfig {
            val databaseUrl = System.getenv("DATABASE_URL")
                ?: error("Missing required environment variable: DATABASE_URL")
            val jwtSecret = System.getenv("SUPABASE_JWT_SECRET")
                ?: error("Missing required environment variable: SUPABASE_JWT_SECRET")
            return AppConfig(databaseUrl, jwtSecret)
        }
    }
}
```

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `AppConfig.kt` created in `config/` package
- [ ] `load()` throws `IllegalStateException` with missing var name if `DATABASE_URL` is absent
- [ ] `load()` throws `IllegalStateException` with missing var name if `SUPABASE_JWT_SECRET` is absent
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T3: UsersTable.kt [P]

**What**: Define Exposed DSL Table object mapping to `public.users` — includes all columns from spec (AUTH-02, AUTH-09).
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/db/UsersTable.kt`
**Depends on**: T1 (Exposed + datetime libs)
**Reuses**: nothing (new)
**Requirement**: AUTH-02, AUTH-09

**Schema** (exact from design):
```kotlin
object UsersTable : Table("public.users") {
    val id        = uuid("id")
    val name      = text("name").nullable()
    val email     = text("email")
    val phone     = text("phone").nullable()
    val photoUrl  = text("photo_url").nullable()
    val role      = text("role").default("runner")
    val status    = text("status").default("pending")
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

Note: DB-level `CHECK` constraints on `role` and `status` are enforced by the raw SQL in the schema, not by Exposed's DSL. The migration SQL includes the `CHECK` clauses.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `UsersTable.kt` created in `db/` package
- [ ] All columns present: `id`, `name`, `email`, `phone`, `photo_url`, `role`, `status`, `created_at`
- [ ] `role` defaults to `"runner"`, `status` defaults to `"pending"`
- [ ] `primaryKey` set to `id`
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T4: Data Models [P]

**What**: Create the three data model files — `UserRow` (domain), `UserResponse` (API output), `UpdateProfileRequest` (PATCH input) — plus a `toResponse()` mapping extension.
**Where**:
- `server/src/main/kotlin/com/example/pompeiarunners/models/UserRow.kt`
- `server/src/main/kotlin/com/example/pompeiarunners/models/UserResponse.kt`
- `server/src/main/kotlin/com/example/pompeiarunners/models/UpdateProfileRequest.kt`
**Depends on**: T1 (`@Serializable` needs kotlinx.serialization plugin)
**Reuses**: nothing (new)
**Requirement**: AUTH-05, AUTH-06

**Exact shapes** (from design):
```kotlin
// UserRow.kt
data class UserRow(
    val id: UUID,
    val name: String?,
    val email: String,
    val phone: String?,
    val photoUrl: String?,
    val role: String,
    val status: String,
    val createdAt: Instant,   // kotlinx.datetime.Instant
)

// UserResponse.kt
@Serializable
data class UserResponse(
    val id: String,
    val name: String?,
    val email: String,
    val phone: String?,
    @SerialName("photo_url") val photoUrl: String?,
    val role: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

fun UserRow.toResponse() = UserResponse(
    id = id.toString(),
    name = name,
    email = email,
    phone = phone,
    photoUrl = photoUrl,
    role = role,
    status = status,
    createdAt = createdAt.toString(),
)

// UpdateProfileRequest.kt
@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)
```

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] All three files created in `models/` package
- [ ] `UserRow` fields match spec schema exactly (including `status`)
- [ ] `UserResponse` has `@Serializable` + `@SerialName` for snake_case fields
- [ ] `UpdateProfileRequest` does NOT include `role` or `email`
- [ ] `toResponse()` extension maps all fields
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T5: DatabasePlugin.kt

**What**: Implement `configureDatabase(config)` — creates HikariCP datasource, connects Exposed, runs idempotent `UsersTable` migration, logs success.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/plugins/DatabasePlugin.kt`
**Depends on**: T1 (HikariCP/Exposed/PG libs), T2 (`AppConfig`), T3 (`UsersTable`)
**Reuses**: Existing `logback.xml` logger
**Requirement**: AUTH-01, AUTH-02

**Implementation guidance**:
```kotlin
fun Application.configureDatabase(config: AppConfig) {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.databaseUrl
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 5
    }
    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)
    transaction {
        SchemaUtils.create(UsersTable)
    }
    log.info("Database connected")
}
```

Migration: `SchemaUtils.create(UsersTable)` uses `CREATE TABLE IF NOT EXISTS` — idempotent.
DB-level `CHECK` constraints for `role` and `status` are NOT enforced by Exposed's `SchemaUtils.create()` unless the column definition includes them. The spec requires DB-level constraint enforcement (AUTH-02 AC-4). Add a raw SQL migration step after `SchemaUtils.create()`:
```kotlin
transaction {
    SchemaUtils.create(UsersTable)
    exec("""
        ALTER TABLE public.users
        ADD CONSTRAINT IF NOT EXISTS users_role_check CHECK (role IN ('runner', 'coach', 'admin')),
        ADD CONSTRAINT IF NOT EXISTS users_status_check CHECK (status IN ('pending', 'approved'))
    """)
}
```

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `DatabasePlugin.kt` created in `plugins/` package
- [ ] `configureDatabase(config: AppConfig)` extension on `Application`
- [ ] HikariCP datasource created from `config.databaseUrl`
- [ ] `Database.connect(dataSource)` called
- [ ] `SchemaUtils.create(UsersTable)` called in transaction (idempotent)
- [ ] `CHECK` constraints added via raw SQL with `IF NOT EXISTS`
- [ ] `log.info("Database connected")` logged on success
- [ ] Startup crash propagates naturally if DB unreachable (no swallowed exceptions)
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T6: SerializationPlugin.kt [P]

**What**: Implement `configureSerialization()` — installs ContentNegotiation with kotlinx.serialization JSON.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/plugins/SerializationPlugin.kt`
**Depends on**: T1
**Reuses**: nothing (new)
**Requirement**: AUTH-05, AUTH-06 (JSON response serialization for all routes)

```kotlin
fun Application.configureSerialization() {
    install(ContentNegotiation) { json() }
}
```

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `SerializationPlugin.kt` created in `plugins/` package
- [ ] `configureSerialization()` extension on `Application`
- [ ] `ContentNegotiation` installed with `json()` converter
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T7: StatusPagesPlugin.kt [P]

**What**: Implement `configureStatusPages()` — centralized exception-to-HTTP mapping. Define custom exception types. No try/catch needed in routes.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/plugins/StatusPagesPlugin.kt`
**Depends on**: T1
**Reuses**: nothing (new)
**Requirement**: AUTH-01 (DB unavailable), AUTH-04 (sync failed), AUTH-06 (malformed body)

**Custom exceptions** (define in same file):
```kotlin
class DatabaseUnavailableException(message: String = "Database unavailable") : Exception(message)
class ProfileSyncException(message: String = "Profile sync failed") : Exception(message)
```

**Mappings**:
| Exception | Status | Body |
|---|---|---|
| `ContentTransformationException` | 400 | `{"error": "invalid_body"}` |
| `DatabaseUnavailableException` | 503 | `{"error": "database_unavailable"}` |
| `ProfileSyncException` | 500 | `{"error": "profile_sync_failed"}` |

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `StatusPagesPlugin.kt` created in `plugins/` package
- [ ] `DatabaseUnavailableException` and `ProfileSyncException` defined
- [ ] `configureStatusPages()` installs `StatusPages` plugin
- [ ] All three exception mappings present with correct status codes and JSON bodies
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none
**Gate**: build

---

### T8: AuthPlugin.kt

**What**: Implement `configureAuth(config)` — installs Ktor `Authentication` with a `jwt("supabase")` provider; returns structured JSON on auth failure.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/plugins/AuthPlugin.kt`
**Depends on**: T1, T2 (`AppConfig` for JWT secret)
**Reuses**: nothing (new)
**Requirement**: AUTH-03

**Implementation guidance**:
```kotlin
fun Application.configureAuth(config: AppConfig) {
    install(Authentication) {
        jwt("supabase") {
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwtSecret)).build()
            )
            validate { credential -> JWTPrincipal(credential.payload) }
            challenge { _, _ ->
                val errorBody = when {
                    call.request.headers["Authorization"] == null ->
                        mapOf("error" to "missing_token")
                    else ->
                        mapOf("error" to "invalid_token")
                }
                call.respond(HttpStatusCode.Unauthorized, errorBody)
            }
        }
    }
}
```

**Challenge error mapping**:
- No `Authorization` header → `{"error": "missing_token"}`
- Invalid/tampered token → `{"error": "invalid_token"}`
- Expired token: `java-jwt` throws `TokenExpiredException` — catch it and respond `{"error": "token_expired"}`

**Note**: Token expiry detection may require wrapping the verifier call or catching in the challenge block. Consult `java-jwt` docs (Context7 or web) at task time for exact API.

**Tools**:
- MCP: context7 (for java-jwt / ktor-auth-jwt API)
- Skill: NONE

**Done when**:
- [ ] `AuthPlugin.kt` created in `plugins/` package
- [ ] `configureAuth(config: AppConfig)` extension on `Application`
- [ ] `Authentication` plugin installed with `jwt("supabase")` provider
- [ ] HS256 verifier built from `config.jwtSecret`
- [ ] `validate` returns `JWTPrincipal` for valid tokens
- [ ] Challenge block returns 401 with correct JSON body for all 3 failure modes
- [ ] Integration test added to `server/src/test/kotlin/ApplicationTest.kt`:
  - `GET /users/me` with no token → 401 `{"error":"missing_token"}`
  - `GET /users/me` with tampered token → 401 `{"error":"invalid_token"}`
- [ ] Gate check passes: `.\gradlew.bat server:test`
- [ ] Test count: ≥3 tests pass (existing 1 + 2 new auth tests)

**Tests**: integration
**Gate**: full

**Commit**: `feat(server): add JWT authentication plugin (AUTH-03)`

---

### T9: UserRepository.kt

**What**: Implement `UserRepository` — all DB access for `public.users`; no SQL outside this class.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/repositories/UserRepository.kt`
**Depends on**: T3 (`UsersTable`), T4 (`UserRow`, `UserResponse`), T5 (DB connection live at runtime), T7 (`ProfileSyncException`)
**Reuses**: Exposed DSL from T3
**Requirement**: AUTH-04, AUTH-05, AUTH-06, AUTH-11, AUTH-12

**Interface** (exact from design):
```kotlin
class UserRepository {
    suspend fun upsertIfAbsent(id: UUID, email: String): UserRow
    suspend fun findById(id: UUID): UserRow?
    suspend fun updateProfile(id: UUID, name: String?, phone: String?, photoUrl: String?): UserRow
    suspend fun findPendingUsers(): List<UserRow>
    suspend fun approveUser(id: UUID): UserRow?   // null = not found
}
```

**`upsertIfAbsent` strategy**:
1. `newSuspendedTransaction { UsersTable.insertIgnore { ... } }` — ON CONFLICT DO NOTHING
2. `newSuspendedTransaction { UsersTable.selectAll().where { id eq sub }.singleOrNull() }` — always returns row after step 1
3. If `singleOrNull()` returns null after insert → throw `ProfileSyncException`

**`updateProfile`**: Only update non-null fields (partial update). Wrap in `newSuspendedTransaction`. Return updated row via `findById`.

**`approveUser`**: Set `status = "approved"` for target id. Return null if no row found. Idempotent — already-approved users return 200 with unchanged profile (handled in CoachRoutes).

**Missing `email` fallback**: If JWT `email` claim is null/empty, log WARN and use `""` as fallback (per edge cases in spec).

**Tools**:
- MCP: context7 (Exposed DSL API — `insertIgnore`, `newSuspendedTransaction`)
- Skill: NONE

**Done when**:
- [ ] `UserRepository.kt` created in `repositories/` package
- [ ] All 5 methods implemented
- [ ] All queries use `newSuspendedTransaction` (non-blocking)
- [ ] `upsertIfAbsent` uses `insertIgnore` + SELECT; throws `ProfileSyncException` on sync failure
- [ ] `updateProfile` only updates non-null fields
- [ ] Row mapped from `ResultRow` to `UserRow` (all fields including `status`)
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none (tested via route integration tests in T12/T13)
**Gate**: build

---

### T10: AccountStatusPlugin.kt + CallExtensions.kt

**What**: (1) Extract `currentUser()` helper to a shared extensions file; (2) Implement `configureAccountStatus()` — pipeline interceptor that blocks pending users with 403 + PT-BR message.
**Where**:
- `server/src/main/kotlin/com/example/pompeiarunners/utils/CallExtensions.kt`
- `server/src/main/kotlin/com/example/pompeiarunners/plugins/AccountStatusPlugin.kt`
**Depends on**: T8 (`AuthPlugin` — JWT principal), T9 (`UserRepository`)
**Reuses**: `UserRepository.upsertIfAbsent()` from T9
**Requirement**: AUTH-10, AUTH-04

**`currentUser()` helper** (`CallExtensions.kt`):
```kotlin
private val UserRowKey = AttributeKey<UserRow>("UserRow")

suspend fun ApplicationCall.currentUser(repo: UserRepository): UserRow {
    return attributes.getOrNull(UserRowKey) ?: run {
        val principal = principal<JWTPrincipal>()!!
        val sub = UUID.fromString(principal.payload.subject)
        val email = principal.payload.getClaim("email").asString() ?: ""
        val user = repo.upsertIfAbsent(sub, email)
        attributes.put(UserRowKey, user)
        user
    }
}
```

**`AccountStatusPlugin`** (`AccountStatusPlugin.kt`):
```kotlin
fun Application.configureAccountStatus(repo: UserRepository) {
    intercept(ApplicationCallPipeline.Plugins) {
        val principal = call.principal<JWTPrincipal>() ?: return@intercept
        val user = call.currentUser(repo)
        if (user.status == "pending") {
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf(
                    "error" to "account_pending",
                    "message" to "Espere o treinador aprovar sua criação de conta"
                )
            )
            finish()
        }
    }
}
```

**Note**: `configureAccountStatus` must be installed AFTER `configureAuth` in startup sequence (requires JWT principal to be available).

**Tools**:
- MCP: context7 (Ktor pipeline interceptor API, `AttributeKey`)
- Skill: NONE

**Done when**:
- [ ] `CallExtensions.kt` created in `utils/` package with `currentUser()` extension
- [ ] `UserRowKey` attribute key defined; caches `UserRow` in call attributes to avoid repeated DB hits
- [ ] `AccountStatusPlugin.kt` created in `plugins/` package
- [ ] Interceptor only acts when `JWTPrincipal` is present (unauthenticated calls pass through)
- [ ] Pending user → 403 with exact JSON and PT-BR message
- [ ] `finish()` called to halt pipeline
- [ ] Integration test added to `ApplicationTest.kt`:
  - Pending user calls `GET /users/me` → 403 `{"error":"account_pending","message":"Espere o treinador aprovar sua criação de conta"}`
  - Approved user calls `GET /users/me` → 200 (mocked repo)
- [ ] Gate check passes: `.\gradlew.bat server:test`
- [ ] Test count: ≥5 tests pass (prior 3 + 2 new status tests)

**Tests**: integration
**Gate**: full

**Commit**: `feat(server): add account status gate and currentUser helper (AUTH-10, AUTH-04)`

---

### T11: RoleGuard.kt

**What**: Implement `requireRole()` — extension on `ApplicationCall` that returns 403 if the authenticated user's DB role does not satisfy the requirement.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/routes/RoleGuard.kt`
**Depends on**: T9 (`UserRepository`), T10 (`currentUser()` helper from `CallExtensions.kt`)
**Reuses**: `currentUser()` from T10
**Requirement**: AUTH-07, AUTH-11, AUTH-12

**Interface**:
```kotlin
suspend fun ApplicationCall.requireRole(repo: UserRepository, vararg allowed: String) {
    val user = currentUser(repo)
    if (user.role !in allowed) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "insufficient_role"))
        finish()  // must be called in pipeline context — use PipelineContext extension or throw RespondException
    }
}
```

**Note**: `finish()` in Ktor requires a `PipelineContext` receiver. Design the function signature accordingly — either as a `PipelineContext` extension or throw a `RespondException`. Consult Ktor docs at task time.

**Tools**:
- MCP: context7 (Ktor `ApplicationCall` / `PipelineContext` API)
- Skill: NONE

**Done when**:
- [ ] `RoleGuard.kt` created in `routes/` package
- [ ] `requireRole()` calls `currentUser()` to get role from DB
- [ ] Caller with disallowed role → 403 `{"error": "insufficient_role"}` + pipeline halted
- [ ] Caller with allowed role → continues normally
- [ ] File compiles: `.\gradlew.bat server:compileKotlin`

**Tests**: none (tested via T13 integration tests)
**Gate**: build

---

### T12: UserRoutes.kt [P]

**What**: Implement `GET /users/me` and `PATCH /users/me` under `authenticate("supabase")` guard; uses `currentUser()` for lazy sync; includes integration tests.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/routes/UserRoutes.kt`
**Depends on**: T4 (models), T8 (`AuthPlugin` for `authenticate`), T9 (`UserRepository`), T10 (`currentUser()`)
**Reuses**: `currentUser()` from T10, `UpdateProfileRequest` + `toResponse()` from T4
**Requirement**: AUTH-05, AUTH-06

**Route structure**:
```kotlin
fun Route.userRoutes(repo: UserRepository) {
    authenticate("supabase") {
        get("/users/me") {
            val user = call.currentUser(repo)
            call.respond(user.toResponse())
        }
        patch("/users/me") {
            val req = call.receive<UpdateProfileRequest>()
            val user = call.currentUser(repo)
            val updated = repo.updateProfile(user.id, req.name, req.phone, req.photoUrl)
            call.respond(updated.toResponse())
        }
    }
}
```

**Edge cases**:
- Malformed JSON body on PATCH → `ContentTransformationException` → caught by `StatusPages` → 400
- Empty `{}` body → `UpdateProfileRequest` with all nulls → `updateProfile` no-ops → 200 unchanged

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `UserRoutes.kt` created in `routes/` package
- [ ] `GET /users/me` returns 200 with `UserResponse` JSON for authenticated approved user
- [ ] `PATCH /users/me` updates only non-null fields; returns 200 with updated profile
- [ ] `role` and `email` absent from `UpdateProfileRequest` — silently ignored by design
- [ ] Integration tests added to `ApplicationTest.kt`:
  - `GET /users/me` with valid token + approved user → 200 with user JSON
  - `PATCH /users/me` with `{"name":"João"}` → 200 with updated name
  - `PATCH /users/me` with `{}` body → 200 with unchanged profile
- [ ] Gate check passes: `.\gradlew.bat server:test`
- [ ] Test count: ≥8 tests pass (prior 5 + 3 new user route tests)

**Tests**: integration
**Gate**: full

**Commit**: `feat(server): add GET/PATCH /users/me endpoints (AUTH-05, AUTH-06)`

---

### T13: CoachRoutes.kt [P]

**What**: Implement `GET /coach/pending-users` and `POST /users/{id}/approve` under `authenticate("supabase")` guard with `requireRole` enforcement; includes integration tests.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/routes/CoachRoutes.kt`
**Depends on**: T4 (models), T8 (`AuthPlugin`), T9 (`UserRepository`), T10 (`currentUser()`), T11 (`requireRole()`)
**Reuses**: `requireRole()` from T11, `toResponse()` from T4
**Requirement**: AUTH-11, AUTH-12

**Route structure**:
```kotlin
fun Route.coachRoutes(repo: UserRepository) {
    authenticate("supabase") {
        get("/coach/pending-users") {
            requireRole(repo, "coach", "admin")
            val pending = repo.findPendingUsers()
            call.respond(pending.map { it.toResponse() })
        }
        post("/users/{id}/approve") {
            requireRole(repo, "coach", "admin")
            val id = call.parameters["id"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
                return@post
            }
            val updated = repo.approveUser(id)
                ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "user_not_found"))
                    return@post
                }
            call.respond(updated.toResponse())
        }
    }
}
```

**Edge cases**:
- Malformed UUID path param → 400 `{"error": "invalid_body"}`
- Already approved user → `approveUser` returns row unchanged → 200 (idempotent)
- `requireRole` runs AFTER `AccountStatusPlugin` — pending coaches cannot reach these endpoints

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `CoachRoutes.kt` created in `routes/` package
- [ ] `GET /coach/pending-users` returns 200 list for coach/admin; 403 for runner
- [ ] Empty list returns `[]` not error
- [ ] `POST /users/{id}/approve` approves target user; idempotent; 404 for unknown id
- [ ] Malformed UUID → 400
- [ ] Unauthenticated → 401 (handled by `authenticate`)
- [ ] Integration tests added to `ApplicationTest.kt`:
  - Coach calls `GET /coach/pending-users` with 2 pending users → 200 list of 2
  - Runner calls `GET /coach/pending-users` → 403 `{"error":"insufficient_role"}`
  - Coach approves pending user → 200 with `status="approved"`
  - Second approve call → 200 unchanged (idempotent)
  - `POST /users/not-a-uuid/approve` → 400
- [ ] Gate check passes: `.\gradlew.bat server:test`
- [ ] Test count: ≥13 tests pass (prior 8 + 5 new coach route tests)

**Tests**: integration
**Gate**: full

**Commit**: `feat(server): add coach approval endpoints (AUTH-11, AUTH-12)`

---

### T14: Update Application.kt

**What**: Wire all `configure*()` calls in correct startup sequence; validate env vars via `AppConfig.load()` before anything starts; inject `UserRepository` where needed.
**Where**: `server/src/main/kotlin/com/example/pompeiarunners/Application.kt`
**Depends on**: T2 (AppConfig), T5 (DatabasePlugin), T6 (SerializationPlugin), T7 (StatusPagesPlugin), T8 (AuthPlugin), T10 (AccountStatusPlugin), T12 (UserRoutes), T13 (CoachRoutes)
**Reuses**: Existing `Application.kt` `module()` function
**Requirement**: AUTH-01, AUTH-03

**Startup sequence** (exact order matters):
```kotlin
fun Application.module() {
    val config = AppConfig.load()           // fail-fast env validation
    val repo = UserRepository()
    configureDatabase(config)               // HikariCP + Exposed + migration
    configureSerialization()                // ContentNegotiation BEFORE auth (challenge uses JSON)
    configureStatusPages()                  // centralized exception mapping
    configureAuth(config)                   // JWT("supabase") provider
    configureAccountStatus(repo)            // pending-account gate interceptor
    configureRouting(repo)                  // mount all routes
}

fun Application.configureRouting(repo: UserRepository) {
    routing {
        userRoutes(repo)
        coachRoutes(repo)
    }
}
```

**Note**: `configureSerialization()` must be installed before `configureAuth()` because the JWT challenge block uses `call.respond()` with JSON bodies.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `Application.kt` updated with all `configure*()` calls in documented order
- [ ] `AppConfig.load()` is the first call in `module()` — server exits on missing env vars
- [ ] `UserRepository` instantiated once and passed to plugins/routes that need it
- [ ] Existing GET `/` test still passes (no regression)
- [ ] Gate check passes: `.\gradlew.bat server:test`
- [ ] Test count: ≥13 tests pass (no regressions)

**Tests**: integration
**Gate**: full

**Commit**: `feat(server): wire backend auth into Application.kt (AUTH-01 through AUTH-12)`

---

## Parallel Execution Map

```
Phase 1 (Sequential):
  T1

Phase 2 (Parallel — all after T1):
  T1 ──┬→ T2 [P]
       ├→ T3 [P]
       ├→ T4 [P]
       ├→ T6 [P]
       └→ T7 [P]

Phase 3 (Mixed — T5 waits for T2+T3; T8 waits for T2 only):
  T2+T3 ──→ T5
  T2    ──→ T8   ← parallel with T5

Phase 4 (Sequential):
  T3+T4+T5 ──→ T9

Phase 5 (Sequential):
  T8+T9 ──→ T10

Phase 6 (Sequential):
  T10 ──→ T11

Phase 7 (Parallel):
  T10+T11 ──┬→ T12 [P]
             └→ T13 [P]

Phase 8 (Sequential):
  T2+T5+T6+T7+T8+T10+T12+T13 ──→ T14
```

**Parallelism constraints:**

- T12 and T13 are parallel-safe per TESTING.md (Ktor integration tests: Parallel-Safe: Yes)
- T2–T7 in Phase 2 have no code dependencies on each other
- T5 and T8 can run in parallel (T5 waits for T2+T3, T8 waits for T2 only — start T8 as soon as T2 finishes)

---

## Pre-Approval Validation

### Check 1: Task Granularity

| Task | Scope | Status |
|------|-------|--------|
| T1: Gradle deps | 2 config files (cohesive) | ✅ |
| T2: AppConfig | 1 data class + companion | ✅ |
| T3: UsersTable | 1 Exposed Table object | ✅ |
| T4: Data models | 3 cohesive model files | ✅ |
| T5: DatabasePlugin | 1 plugin function | ✅ |
| T6: SerializationPlugin | 1 plugin function | ✅ |
| T7: StatusPagesPlugin | 1 plugin + 2 exception classes (cohesive) | ✅ |
| T8: AuthPlugin | 1 plugin function | ✅ |
| T9: UserRepository | 1 class, 5 methods (cohesive) | ✅ |
| T10: AccountStatusPlugin + CallExtensions | 2 files (tightly coupled: plugin uses helper) | ✅ |
| T11: RoleGuard | 1 extension function | ✅ |
| T12: UserRoutes | 1 route file, 2 endpoints (cohesive) | ✅ |
| T13: CoachRoutes | 1 route file, 2 endpoints (cohesive) | ✅ |
| T14: Application.kt | 1 file modification (wiring) | ✅ |

### Check 2: Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
|------|-------------------|---------------|--------|
| T1 | None | Start node | ✅ |
| T2 | T1 | T1 → T2 | ✅ |
| T3 | T1 | T1 → T3 | ✅ |
| T4 | T1 | T1 → T4 | ✅ |
| T5 | T1, T2, T3 | T2+T3 → T5 (T1 implicit via T2/T3) | ✅ |
| T6 | T1 | T1 → T6 | ✅ |
| T7 | T1 | T1 → T7 | ✅ |
| T8 | T1, T2 | T2 → T8 (T1 implicit via T2) | ✅ |
| T9 | T3, T4, T5 | T3+T4+T5 → T9 | ✅ |
| T10 | T8, T9 | T8+T9 → T10 | ✅ |
| T11 | T9, T10 | T10 → T11 (T9 implicit via T10) | ✅ |
| T12 [P] | T4, T8, T9, T10 | T10+T11 → T12 | ✅ |
| T13 [P] | T4, T8, T9, T10, T11 | T10+T11 → T13 | ✅ |
| T14 | T2, T5, T6, T7, T8, T10, T12, T13 | All prior phases → T14 | ✅ |

### Check 3: Test Co-location Validation

Per TESTING.md — `server` routes require Integration tests. Gate: `.\gradlew.bat server:test`.

| Task | Code Layer | Matrix Requires | Task Says | Status |
|------|-----------|----------------|-----------|--------|
| T1 | Config only | none | none | ✅ |
| T2 | Config data class | none | none | ✅ |
| T3 | DB Table definition | none | none | ✅ |
| T4 | Data models | none | none | ✅ |
| T5 | Plugin (no routes) | none | none | ✅ |
| T6 | Plugin (no routes) | none | none | ✅ |
| T7 | Plugin (no routes) | none | none | ✅ |
| T8 | Auth plugin (behavior testable via TestHost) | integration | integration | ✅ |
| T9 | Repository (no routes) | none | none | ✅ |
| T10 | Plugin + helper (pipeline behavior) | integration | integration | ✅ |
| T11 | Route utility (no routes) | none | none | ✅ |
| T12 | Server routes | integration | integration | ✅ |
| T13 | Server routes | integration | integration | ✅ |
| T14 | Wiring (routes indirectly) | integration | integration | ✅ |

All checks pass. ✅

---

## Requirement Traceability

| Requirement | Covered By |
|---|---|
| AUTH-01 DB Connection | T1, T2, T5, T14 |
| AUTH-02 Users Table Migration | T1, T3, T5 |
| AUTH-03 JWT Verification | T1, T2, T8, T14 |
| AUTH-04 Lazy User Profile Sync | T7, T9, T10 |
| AUTH-05 GET /users/me | T4, T9, T12 |
| AUTH-06 PATCH /users/me | T4, T9, T12 |
| AUTH-07 Role-Based Route Guard (P2) | T11 |
| AUTH-08 Maintenance Mode (P2) | Deferred — no task |
| AUTH-09 Account Approval — Status Field | T3, T9 |
| AUTH-10 Pending Account Access Block | T10 |
| AUTH-11 Coach Views Pending Accounts | T9, T11, T13 |
| AUTH-12 Coach Approves Account | T9, T11, T13 |

**Coverage:** 12 requirements, 12 mapped ✅ (AUTH-08 explicitly deferred — P2, not in task scope)
