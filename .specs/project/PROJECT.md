# PompeiaRunners

**Vision:** A race management platform for Brazilian runners, connecting athletes to competition calendars and personal rankings, with a coach-facing admin panel to manage events.
**For:** Brazilian runners (mobile users) and running coaches (web admins).
**Solves:** Fragmented race management — runners lack a centralized place to discover events, register, and track their competitive ranking; coaches need a simple tool to create and manage those events.

## Goals

- Runners can browse the race calendar, register for events, and view their personal ranking.
- Coaches can create, edit, and manage races via the React admin panel.
- All platforms (Android, Backend, React Web) share consistent data through the Ktor API.

## Tech Stack

**Core:**

- Language: Kotlin 2.3.20 (shared/backend), TypeScript (web)
- UI (mobile): Compose Multiplatform 1.10.3 + Material3
- Backend: Ktor Server 3.4.1 (Netty) — REST API
- Web (admin): React 18.2.0 + TypeScript + Vite

**Key dependencies:**
- Kotlin Multiplatform (`shared` module — common business logic)
- Gradle version catalog (`gradle/libs.versions.toml`)
- No DI / Networking / Database configured yet (starter template)

## Scope

**v1 includes:**

- Race calendar: list and detail view for upcoming events
- Race registration: runners register/unregister for events
- Personal ranking: leaderboard per event or overall
- Admin panel (React): create, edit, delete events; manage registrations
- Ktor backend: REST API serving all data to mobile and web clients

**Explicitly out of scope:**

- iOS app (KMP target exists but not prioritized for v1)
- Push notifications
- Payment processing / race fees
- Social features (chat, groups, following)
- Offline mode

## Constraints

- Timeline: Solo project — no fixed deadline, but ship Android + backend + admin panel as a cohesive v1
- Technical: Starter template — architecture layers (MVVM/MVI, DI, networking, database) are not yet configured and must be added
- Resources: Solo developer; prioritize Android and backend over web parity
- Locale: Brazilian market (Portuguese language, Brazilian date/currency formats)
