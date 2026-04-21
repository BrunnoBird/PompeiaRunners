# Project State

_Last updated: 2026-04-21_

## Status

Backend auth spec + design complete (AUTH-01 → AUTH-08). Ready for tasks breakdown.

## Decisions

| Date | Decision | Reason |
|------|----------|--------|
| 2026-04-21 | Android + Ktor backend + React admin panel are v1 scope | iOS deprioritized; solo project, ship usable platform first |
| 2026-04-21 | Target audience: Brazilian runners (PT-BR locale) | Product is for a Brazilian running consultancy |
| 2026-04-21 | Architecture not yet chosen (MVVM/MVI/Clean) | Starter template — must be decided before first feature |
| 2026-04-21 | Auth strategy: Supabase Auth (Option A) | Supabase owns token lifecycle (email/password + Google + Apple); Ktor only verifies JWTs — saves weeks of OAuth implementation for solo MVP |
| 2026-04-21 | User roles: `runner` (default), `coach`, `admin` | Admin is developer-only role for maintenance windows; not user-selectable |
| 2026-04-21 | User sync: lazy sync on first authenticated request | Simpler than webhooks; no Supabase Edge Function needed |
| 2026-04-21 | ORM: Exposed DSL (not DAO) | Less ceremony; SQL stays visible for simple tables |
| 2026-04-21 | JWT issuer validation: skipped in MVP | Supabase issuer URL varies by env; HS256 secret is sufficient |
| 2026-04-21 | Role source: public.users.role (DB, not JWT claims) | Keeps role management server-side; JWT doesn't carry role |

## Blockers

_None currently._

## Todos

- [x] Map existing codebase → `.specs/codebase/` (7 docs)
- [x] Create ROADMAP.md with feature milestones
- [x] Specify backend-auth feature → `.specs/features/backend-auth/spec.md`
- [x] Design backend-auth → architecture, Exposed ORM setup, Ktor plugin config
- [ ] Break backend-auth into atomic tasks → `tasks.md`
- [ ] Choose and document architecture pattern (MVVM recommended for Compose Multiplatform)
- [ ] Choose and configure DI (Koin is idiomatic for KMP)
- [ ] Choose and configure networking client (Ktor Client for KMP)
- [ ] Define API contract between Ktor backend and clients

## Deferred Ideas

- iOS app (after Android v1 ships)
- Push notifications for race reminders
- Payment/fee integration
- Social features (groups, following athletes)
- Offline mode

## Preferences

_None recorded yet._
