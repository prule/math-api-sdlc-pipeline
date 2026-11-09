---
date: 2026-11-09
change: add-exact-calculation
input: use-cases/UC-002-perform-exact-calculation.md
branch: feat/uc-2-perform-exact-calculation
outcome: archived
agent_runs: 6
implementer: junior-dev
claude_code: 2.1.317
orchestrator: {model: claude-opus-5-5, effort: medium}
agents:
  architect: {model: claude-opus-5-5, effort: medium, runs: 4}
  spec-reviewer: {model: claude-opus-5-5, effort: medium, runs: 2}
  junior-dev: {model: claude-opus-5-5, effort: medium, runs: 2}
  qa: {model: claude-opus-5-5, effort: medium, runs: 2}
  senior-dev: {model: claude-opus-5-5, effort: medium, runs: 2}
session: 47ef3287-b922-40a6-aa25-2e7585cba59d
session_report: reports/sessions/47ef3287-b922-40a6-aa25-2e7585cba59d.report.html
---

# Run retrospective — add-exact-calculation

## Summary

This change built B-2, the first vertical slice: `POST /api/v1/calculations` for ADD, SUBTRACT and MULTIPLY. It covers the domain `Operation` / `Calculation`, the `PerformCalculation` use case, the web adapter and a single Problem Details handler. It also removed B-1's ArchUnit empty-layer allowances. 64 tests pass.

Every gate passed first time. The spec reviewer approved with 5 advisories, and four of them were folded in after Gate 1. Gate 1 also deferred `Operation.arity()` to B-8 and accepted the huge-Operand and OpenAPI gaps until B-4 and B-5. QA caught all 9 code mutants. The senior dev added the one missing test, for the 404.

Last run's ledger lessons held. L-1: the implementer recorded both of its deviations in design.md and tasks.md. L-3: the framework-defaults table caught that Spring 7 leaves Problem Details `type` empty. Both lessons worked only because the orchestrator put them in the agents' prompts; their targets in the profile and the plugin are still unchanged.

The thing most worth improving: a plan that names a framework file (`UseCaseConfig`) in a test the ArchUnit rules also check.

The session log and its report cover both B-1 and B-2. The `agents` run counts above come from that report and add both runs together; this change had 6 agent runs.

## Gate log

| # | Gate | Round | Verdict | Blocking | Advisory | Notes |
|---|------|-------|---------|----------|----------|-------|
| 1 | spec-reviewer | 1 | APPROVE | 0 | 5 | P1, P3, P4, P5 folded in by the architect after Gate 1; P2 decided by the human |
| 2 | Gate 1 (human) | — | proceed | — | — | Fix P1/P3/P4/P5; defer arity to B-8; accept the huge-Operand 500 and no OpenAPI until B-4/B-5 |
| 3 | qa | 1 | READY TO ARCHIVE | 0 | 3 | 9/9 domain and application mutants caught; both deviations confirmed |
| 4 | senior-dev | 1 | APPROVE (1 fixed in place, 1 waived) | 0 | 0 | Added `rejectsAnUnknownPath` (404) |
| 5 | Gate 2 (human) | — | archive | — | — | |

## Failed reviews

None. Every gate passed first time.

## Standards violations

| # | Rule | Where | Found by | Status | Recommendation → target |
|---|------|-------|----------|--------|-------------------------|
| S1 | `docs/STANDARDS.md §3`, `docs/DOMAIN-MODEL.md §1` (arity on every Operation) | `design.md` Non-Goals | spec-reviewer (P2) | waived (Gate 1: defer to B-8, per STANDARDS §5) | R5 |
| S2 | `docs/STANDARDS.md §2` (ArchUnit checks test classes too) | `design.md` Decision 5: slice test `@Import(UseCaseConfig.class)` | qa (Q3) | fixed-in-place (by the implementer, recorded under L-1) | R2 |

## Other defects caught

| # | Kind | Where | Found by | Status | Recommendation → target |
|---|------|-------|----------|--------|-------------------------|
| D1 | plan-defect | `design.md` Risks: the huge-Operand 500 breaks R-7 without saying so | spec-reviewer (P1) | fixed-by-rework | R4 |
| D2 | test-gap | `tasks.md` 3.1: shorthand BigDecimal test values | spec-reviewer (P3) | fixed-by-rework | R6 |
| D3 | plan-defect | `ArchitectureTest`: B-1's empty-layer allowances outlived their reason | spec-reviewer (P4) | fixed-by-rework | none: one-off |
| D4 | plan-defect | `tasks.md` 5.1: smoke-test Javadoc still said UC-001 only | spec-reviewer (P5) | fixed-by-rework | none: one-off |
| D5 | test-gap | domain and application tests not seen failing before their code | qa (Q1) | fixed-in-place (proven by mutation) | R3 |
| D6 | test-gap | 404 named in PRD §3.2 but untested | qa (Q2), senior-dev (C1) | fixed-in-place | R1 |
| D7 | code-defect | `ApiExceptionHandler.handleMethodArgumentNotValid`: which missing field is named isn't fixed | senior-dev (C2) | waived (the spec asks for one failure, not a specific field) | none: one-off |

## Recommendations

- [ ] **R1**: when a change adds or edits a PRD error bullet that lists statuses, the spec delta has one scenario per listed status. Add this to the profile's Plan review checklist (§4/§6 line) → `.claude/sdlc-profile.md` (from D6) · ledger: L-4 (new)
- [ ] **R2**: state that ArchUnit layer rules also check test classes, so slice tests import application classes, never `config` → `docs/STANDARDS.md §2` and the profile's Plan review §2 line (from S2) · ledger: L-5 (new)
- [ ] **R3**: each "failing test first" task asks for the pasted red `./gradlew test --tests …` output before the implementing task → `openspec/config.yaml` (rules.tasks) (from D5) · ledger: L-6 (new)
- [ ] **R4**: an accepted risk that breaks a spec requirement names that requirement and says Gate 1 must accept it → `openspec/config.yaml` (rules.design) (from D1) · ledger: L-7 (new)
- [ ] **R5**: settle STANDARDS §3 against §5: `Operation.arity()` arrives with the first unary Operation (B-8). Update the DOMAIN-MODEL glossary and STANDARDS §3 to match → `docs/STANDARDS.md`, `docs/DOMAIN-MODEL.md` (from S1) · ledger: L-8 (new)
- [ ] **R6**: tasks write BigDecimal test values as `new BigDecimal("<text>")`, never from a double → `.claude/sdlc-profile.md` (Implementation rules) (from D2) · ledger: L-9 (new)
