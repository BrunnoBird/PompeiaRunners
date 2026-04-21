# Roadmap

**Current Milestone:** M0 — Foundation
**Status:** In Progress

---

## M0 — Foundation

**Goal:** Project infrastructure ready — architecture, DI, networking, and database configured across all modules. No user-facing features yet, but the skeleton is solid enough to build on.
**Target:** Before any feature work begins.

### Features

**Architecture Setup** — IN PROGRESS
- Choose and document architecture pattern (MVVM for composeApp, clean layering in shared)
- Configure Koin for dependency injection (KMP-compatible)
- Configure Ktor Client for networking (shared module)
- Configure SQLDelight for local database (shared module)
- Define base API contract between Ktor server and clients

**Ktor Backend Bootstrap** — PLANNED
- Define REST API routes for events and rankings
- Add basic JSON serialization (kotlinx.serialization)
- Configure CORS for web admin panel
- In-memory or file-based data store (defer real DB for M1)

**Project Conventions** — PLANNED
- Define and document code conventions (naming, layers, package structure)
- Configure linting / code style tools

---

## M1 — Race Calendar (Read-Only)

**Goal:** Runners can open the Android app and see a list of upcoming race events. Data served by the Ktor backend.
**Target:** First shippable user-facing milestone.

### Features

**Event Listing — Android** — PLANNED
- Screen: list of upcoming races (name, date, location)
- Pull-to-refresh
- Empty state and error state

**Event Detail — Android** — PLANNED
- Screen: race details (description, date, location, distance)
- Registration CTA (visible but not functional until M2)

**Events API — Backend** — PLANNED
- GET /events — list all upcoming events
- GET /events/{id} — single event detail
- Static/seed data for development

---

## M2 — Race Registration

**Goal:** Runners can register and unregister for events through the Android app.

### Features

**User Identity (minimal)** — PLANNED
- Runner profile: name, email (no auth system yet — device-local or simple token)
- Persist registration state locally (SQLDelight)

**Registration Flow — Android** — PLANNED
- Register for an event (POST /registrations)
- Unregister from an event (DELETE /registrations/{id})
- Show registration status on event detail and list screens

**Registrations API — Backend** — PLANNED
- POST /registrations — register a runner for an event
- DELETE /registrations/{id} — unregister
- GET /events/{id}/registrations — list registrations for an event

---

## M3 — Personal Ranking

**Goal:** Runners can view a leaderboard — either per event or overall.

### Features

**Ranking Screen — Android** — PLANNED
- Overall ranking screen (sorted by total races completed or points)
- Per-event results/ranking

**Rankings API — Backend** — PLANNED
- GET /ranking — overall leaderboard
- GET /events/{id}/ranking — per-event results
- Simple scoring logic (e.g., finishers ranked by registration date or manual result entry)

---

## M4 — Coach Admin Panel (React)

**Goal:** Coaches can log into the React web app and manage events and registrations.

### Features

**Admin Authentication** — PLANNED
- Simple coach login (hardcoded credentials or API key for v1)
- Protected routes in React

**Event Management — React** — PLANNED
- Create a new event (name, date, location, distance, description)
- Edit an existing event
- Delete an event
- List all events with status

**Registration Management — React** — PLANNED
- View registrations per event
- Add/remove runners manually
- Export registrations list (CSV)

**Admin API — Backend** — PLANNED
- POST /events — create event (coach only)
- PUT /events/{id} — update event
- DELETE /events/{id} — delete event
- Auth middleware protecting admin routes

---

## M5 — Polish & Release

**Goal:** App is stable, Portuguese-localized, and ready for the Brazilian running community.

### Features

**Localization** — PLANNED
- Full PT-BR strings across Android app
- Date and number formatting per Brazilian locale

**Error Handling & UX Polish** — PLANNED
- Network error states with retry
- Loading skeletons
- Form validation in admin panel

**Backend Hardening** — PLANNED
- Replace in-memory store with a real database (PostgreSQL or SQLite)
- Input validation and error responses
- Basic logging

---

## Future Considerations

- iOS app (KMP target already in place)
- Push notifications for race reminders
- Runner authentication (full auth flow, not just identity)
- Payment/fee integration for paid races
- Social features: following athletes, sharing results
- Offline mode with sync
