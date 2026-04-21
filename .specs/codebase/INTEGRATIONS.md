# External Integrations

**Analyzed:** 2026-04-21

## Overview

Starter template — minimal integrations. No external services connected.

## Backend API (Ktor Server)

**Location:** `server/src/main/kotlin/com/example/pompeiarunners/Application.kt`

| Method | Route | Response | Purpose |
|--------|-------|----------|---------|
| GET | `/` | `"Ktor: Hello, <platform>!"` | Demo endpoint only |

- Engine: Netty, port `8080` (from `shared/.../Constants.kt`)
- Response format: plain text (no JSON, no content negotiation)
- No external APIs called from server

## Frontend → Backend: NOT IMPLEMENTED

Neither mobile nor web apps make HTTP calls to the server.

| Client | HTTP Client | Status |
|--------|------------|--------|
| Android / iOS | — | Not configured |
| React web | — | Not configured |

## External Services: NONE

No authentication, analytics, crash reporting, CDN, push notifications, or cloud database connected.

## Configuration

**Server port:** Hard-coded Kotlin constant
```kotlin
// shared/src/commonMain/.../Constants.kt
const val SERVER_PORT = 8080
```

**Web dev server port:** Hard-coded in `vite.config.ts`

**No `.env` files, no secrets management, no environment-based config.**

## Logging

- `server/src/main/resources/logback.xml` — Logback configured, root level TRACE
- Logback is configured but **no log statements exist in code**
- Mobile and web: no logging

## Build Integrations

- **Gradle:** Google Maven + Maven Central repositories
- **npm:** Vite dev server + bundler for webApp
- **CI/CD:** None configured
