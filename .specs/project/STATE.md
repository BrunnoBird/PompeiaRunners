# Project State

_Last updated: 2026-04-21_

## Status

Project initialized. Starter template in place — no architecture layers configured yet.

## Decisions

| Date | Decision | Reason |
|------|----------|--------|
| 2026-04-21 | Android + Ktor backend + React admin panel are v1 scope | iOS deprioritized; solo project, ship usable platform first |
| 2026-04-21 | Target audience: Brazilian runners (PT-BR locale) | Product is for a Brazilian running consultancy |
| 2026-04-21 | Architecture not yet chosen (MVVM/MVI/Clean) | Starter template — must be decided before first feature |

## Blockers

_None currently._

## Todos

- [x] Map existing codebase → `.specs/codebase/` (7 docs)
- [x] Create ROADMAP.md with feature milestones
- [ ] Choose and document architecture pattern (MVVM recommended for Compose Multiplatform)
- [ ] Choose and configure DI (Koin is idiomatic for KMP)
- [ ] Choose and configure networking client (Ktor Client for KMP)
- [ ] Choose and configure local database (SQLDelight for KMP)
- [ ] Define API contract between Ktor backend and clients
- [ ] Specify M0 — Foundation feature (architecture + infrastructure setup)

## Deferred Ideas

- iOS app (after Android v1 ships)
- Push notifications for race reminders
- Payment/fee integration
- Social features (groups, following athletes)
- Offline mode

## Preferences

_None recorded yet._
