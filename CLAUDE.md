# math-api (sdlc-pipeline rebuild)

Stateless Spring Boot 4 / Java 25 REST API for exact decimal arithmetic, rebuilt from the original
`../math-api` docs using the sdlc-pipeline plugin, so the two builds can be compared.

Read before working:
- `docs/PRD.md` — scope, API contract, non-goals, roadmap
- `docs/DOMAIN-MODEL.md` — glossary and business rules (BR-n)
- `docs/STANDARDS.md` — architecture, conventions, testing
- `BACKLOG.md` — ordered use cases; the pipeline ticks entries as they are built
- `.claude/sdlc-profile.md` — the pipeline profile (commands, rules, review checklist)

## Keep the docs current

Any change that alters scope, the API contract, Operations, domain terms or business rules, or
engineering standards MUST update the matching doc(s) in `docs/` in the same change, including that
doc's change log. Never renumber or reuse a BR id.

## Change workflow

One `BACKLOG.md` entry per change, in order:

1. `/sdlc-pipeline:write-use-case next` — drafts `use-cases/UC-<n>-<slug>.md`.
2. `/sdlc-pipeline:build-use-case use-cases/UC-<n>-<slug>.md` — runs the pipeline with its two gates,
   archives the OpenSpec change, ticks the backlog and writes the retrospective and session report.
3. Commit on the `feat/uc-<n>-<slug>` branch (Conventional Commits) and open a PR against `main` with
   `.github/pull_request_template.md`.
