# AGENTS.md

This file provides guidance to AI agents (Claude Code, Codex, Antigravity, etc.) when working with code in this repository.

## Project Overview

PompeiaRunners is a **Kotlin Multiplatform (KMP)** project targeting Android, iOS, Web, and a Ktor backend server. All modules share common business logic via the `shared` module.

## Build Commands

```bash
# Android app (debug APK)
./gradlew :composeApp:assembleDebug

# Run Ktor backend server
./gradlew :server:run

# Run all tests
./gradlew test

# Build shared library for web
./gradlew :shared:jsBrowserDevelopmentLibraryDistribution

# Web app (from webApp/ directory)
npm install && npm run start
```

> On Windows use `.\gradlew.bat` instead of `./gradlew`.

iOS: open `iosApp/` in Xcode and run from there.

## Module Structure

| Module | Role | Targets |
|--------|------|---------|
| `composeApp` | Compose Multiplatform UI app | Android, iOS |
| `shared` | Shared business logic library | Android, iOS, JVM, JS |
| `server` | Ktor REST API backend | JVM |
| `webApp` | Next.js + React + TypeScript frontend | Browser |
| `iosApp` | iOS native entry point | iOS (Xcode) |

## Architecture

**Pattern:** This is a starter template — no MVVM/MVI/Clean Architecture layers are in place yet. UI state uses plain Compose `mutableStateOf`.

**Platform abstraction:** Uses Kotlin's `expect`/`actual` pattern. The `shared` module defines `expect` interfaces (e.g., `Platform`, `getPlatform()`) and each target (`androidMain`, `iosMain`, `jvmMain`, `jsMain`) provides the `actual` implementation.

**Shared constants:** `shared/src/commonMain/kotlin/.../Constants.kt` holds values like `SERVER_PORT = 8080` used by both server and clients.

**UI entry points:**
- Android: `composeApp/src/androidMain/.../MainActivity.kt` → calls `App()`
- iOS: `composeApp/src/iosMain/.../MainViewController.kt`
- Web: `webApp/src/index.tsx` → `Greeting.tsx` consumes the compiled KMP `Greeting` class

**Server:** Single Ktor module in `server/src/main/.../Application.kt`; currently exposes one `GET /` route.

## Key Config Files

- `gradle/libs.versions.toml` — version catalog for all dependencies and plugins
- `gradle.properties` — Gradle/Kotlin JVM memory settings, AndroidX flags, configuration cache
- `composeApp/build.gradle.kts` — Android config (minSdk 28, compileSdk 36, namespace `com.example.pompeiarunners`)
- `shared/build.gradle.kts` — declares all KMP targets including JS TypeScript definitions output
- `server/build.gradle.kts` — Ktor server, entry point `com.example.pompeiarunners.ApplicationKt`

## Tech Stack

- **Language:** Kotlin 2.3.20
- **UI:** Compose Multiplatform 1.10.3, Material3
- **Backend:** Ktor Server 3.4.1 (Netty engine)
- **Web frontend:** Next.js + React 18.2.0 + TypeScript
- **Build:** Gradle with Kotlin DSL, configuration cache + build cache enabled
- **DI / Networking / Database:** None configured (starter template)
