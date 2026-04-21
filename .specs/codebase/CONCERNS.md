# Technical Concerns & Tech Debt

**Analyzed:** 2026-04-21

---

## Critical

### No Networking Client — Severity: Critical

- **Evidence:** No Ktor Client, Retrofit, OkHttp, or axios in any `build.gradle.kts` or `package.json`. Mobile and web apps render hardcoded UI with no server calls.
- **Risk:** Cannot build any data-driven feature without retrofitting a networking layer. Server is isolated from all consumers.
- **Fix:** Add Ktor Client to `shared/build.gradle.kts` (KMP-compatible). Create an API service layer in `shared/src/commonMain/`. Connect mobile/web to backend endpoints.

---

### No Database or Persistence — Severity: Critical

- **Evidence:** No Room, SQLDelight, SQLite, or cloud DB dependency in any module. No local caching layer. Server has no data model or storage.
- **Risk:** Cannot persist user data, race results, or registrations across sessions. Server loses all state on restart.
- **Fix:** Add SQLDelight for shared KMP persistence. Add a server-side database (PostgreSQL via Exposed or similar). Define shared data models in `shared/src/commonMain/`.

---

### No Authentication or Authorization — Severity: Critical

- **Evidence:** No auth library in any module. Ktor server accepts all requests with no middleware. No token management.
- **Risk:** Cannot implement multi-user features. No access control between runner and coach roles. No security boundary.
- **Fix:** Add Ktor Auth plugin (JWT). Implement auth middleware on protected routes. Add token storage on mobile (EncryptedSharedPreferences).

---

### No Dependency Injection — Severity: High

- **Evidence:** Direct class instantiation throughout: `Greeting().greet()` in `App.kt`, `Application.kt`, `Greeting.tsx`. No Koin, Hilt, or factory patterns.
- **Risk:** Untestable — cannot mock dependencies. Tight coupling between UI and logic. Hard to swap implementations.
- **Fix:** Add Koin (KMP-friendly). Define modules for shared dependencies. Use constructor injection in ViewModels and composables.

---

### No Error Handling — Severity: High

- **Evidence:** No try-catch in `App.kt`, `Application.kt`, or `Greeting.tsx`. No error states in Compose UI. Logback configured but zero log statements in code.
- **Risk:** Crashes on unexpected input. Users see blank screens on failures. No observability.
- **Fix:** Add sealed Result types for API calls. Add `Error | Loading | Success` state to Compose. Add logging at all I/O boundaries.

---

## High

### No Real Test Coverage — Severity: High

- **Evidence:** `SharedCommonTest.kt` and `ComposeAppCommonTest.kt` only assert `assertEquals(3, 1+2)`. `ApplicationTest.kt` tests only the happy path for GET `/`. WebApp has zero tests. No Mockk, Jest, or Compose Test library.
- **Risk:** Cannot refactor safely. No regression detection as features are added.
- **Fix:** Add Mockk for Kotlin mocking. Add Compose Test for UI snapshot tests. Add Jest + React Testing Library for webApp. Establish coverage gates in CI.

---

### No API Contract — Severity: High

- **Evidence:** Server returns plain text (`"Ktor: Hello, Android!"`) — no JSON, no OpenAPI spec. No TypeScript types matching backend responses. KMP JS interop is untyped in React.
- **Risk:** Frontend and backend can diverge silently. No IDE autocomplete on API responses.
- **Fix:** Define JSON API responses with `kotlinx.serialization`. Generate TypeScript types via `generateTypeScriptDefinitions()` in `shared/build.gradle.kts`. Add contract tests.

---

## Medium

### No State Management for Multi-Screen Apps — Severity: Medium

- **Evidence:** `App.kt` uses bare `remember { mutableStateOf(false) }`. `Greeting.tsx` uses `useState()`. No ViewModel, StateFlow, Redux, or Zustand.
- **Risk:** State lost on configuration change (Android). Hard to share state across screens. Unscalable for multi-feature apps.
- **Fix:** Add Compose MVVM (ViewModel + StateFlow) for mobile. Add Zustand or Redux Toolkit for web.

---

### No Navigation — Severity: Medium

- **Evidence:** Single `Column` in `App.kt` (one screen). Single `Greeting` component in React. No Compose Navigation or React Router.
- **Risk:** Cannot build multi-screen flows. No deep linking or back stack.
- **Fix:** Add Jetpack Navigation Compose for mobile. Add React Router for web. Define route structure early.

---

### Hardcoded Configuration — Severity: Medium

- **Evidence:** `SERVER_PORT = 8080` is a Kotlin constant. Vite dev port hard-coded in `vite.config.ts`. No environment variable support in any module.
- **Risk:** Dev/staging/prod require code changes. Not 12-factor compliant.
- **Fix:** Add env variable support. Use `BuildConfig` for Android, `.env` files for Vite, Ktor `environment.config` for server.

---

## Low

### No Documentation — Severity: Low

- **Evidence:** No module-level READMEs. No architecture diagrams. No inline comments beyond syntax. String literals hardcoded in UI (`"Click me!"`, `"Compose: "`).
- **Risk:** Onboarding friction. Decisions not recorded.
- **Fix:** Add per-module READMEs. Extract UI strings to resources. Record architectural decisions in `.specs/`.

---

## Starter Template Expectations

These are **expected gaps for a starter template** — not blockers for experimentation:

- ✓ No feature implementation (by design)
- ✓ Minimal tests (skeleton placeholders provided)
- ✓ Single-screen UI (demonstration purpose)
- ✓ No advanced state management

**Must be resolved before shipping any real feature:**

- [ ] Networking layer (Ktor Client)
- [ ] Persistence layer (SQLDelight + server DB)
- [ ] Authentication
- [ ] Error handling
- [ ] Dependency injection (Koin)
- [ ] Navigation
- [ ] API contract (JSON + types)
- [ ] Real test coverage
