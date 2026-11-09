---
date: 2026-11-09
change: add-api-documentation
input: use-cases/UC-005-explore-api-documentation.md
branch: feat/uc-5-explore-api-documentation
outcome: archived
agent_runs: 6
implementer: junior-dev
claude_code: 2.1.317
orchestrator: {model: claude-opus-5-5, effort: medium}
agents:
  architect: {model: claude-opus-5-5, effort: medium, runs: 12}
  spec-reviewer: {model: claude-opus-5-5, effort: medium, runs: 6}
  junior-dev: {model: claude-opus-5-5, effort: medium, runs: 6}
  qa: {model: claude-opus-5-5, effort: medium, runs: 5}
  senior-dev: {model: claude-opus-5-5, effort: medium, runs: 5}
session: 47ef3287-b922-40a6-aa25-2e7585cba59d
session_report: reports/sessions/47ef3287-b922-40a6-aa25-2e7585cba59d.report.html
---

# Run retrospective — add-api-documentation

## Summary

This change built B-5, which completes the MVP.
- **OpenAPI 3.1 docs:** springdoc 3.2.0 serves `/v3/api-docs` and Swagger UI at its default paths.
- **Explicit responses on the controller:** 200 as `application/json`; 400 and 422 as `application/problem+json`.
- **One `OpenApiCustomizer` in `config.OpenApiConfig`** generates everything that comes from the domain: the Operation list, the BR-1 numbers, the domain exception details and the 200 example.
- **`ApiDocumentationTest`** sends each documented example's request and compares the live status and body with it.
- **`OperandOutOfRangeException`** now builds its message from the BR-1 constants. 155 tests pass.

Every gate passed first time:
- **Spec review:** approved with 5 advisories, all folded in after Gate 1.
- **Implementation:** no deviations from the plan. The failing-test output came from tests that ran: 17 tests got a 404 where they expected 200, and one where it expected a redirect. The implementer broke each "Required" constraint on purpose and confirmed its test failed (L-13).
- **QA:** broke the docs 21 ways. Only the copied-value limit accepted at Gate 1 survived.
- **Code review:** fixed the one overclaim. The description said "every other HTTP error" is `about:blank`; a 500 is not.

The thing most worth improving: **claims about "every" or "all" need checking against the case that breaks them.** The overclaim came from PRD §3.2, whose own "every error response is Problem Details" line is still untrue for an unhandled 500 (C2). That needs a decision: a catch-all handler, or narrower wording.

Ledger L-6 and L-11 got a second record. Four lines are now at 2 records (L-1, L-3, L-6, L-11); none is due yet.

The session log and report cover B-1 to B-5. The `agents` run counts above are cumulative; this change had 6 agent runs.

## Gate log

| # | Gate | Round | Verdict | Blocking | Advisory | Notes |
|---|------|-------|---------|----------|----------|-------|
| 1 | spec-reviewer | 1 | APPROVE | 0 | 5 | A1–A5 folded in by the architect after Gate 1 |
| 2 | Gate 1 (human) | — | proceed | — | — | Fix A1–A5; Q-1 description only; docs on in every profile until B-6 accepted; copied-value limit accepted |
| 3 | qa | 1 | READY TO ARCHIVE | 0 | 1 | 20/21 code breaks caught (the survivor is the accepted limit); running app checked |
| 4 | senior-dev | 1 | APPROVE (1 fixed in place, 1 outstanding, 1 waived) | 0 | 0 | Q1/C1 fixed; C2 outstanding; C3 waived |
| 5 | Gate 2 (human) | — | archive | — | — | |

## Failed reviews

None. Every gate passed first time.

## Standards violations

| # | Rule | Where | Found by | Status | Recommendation → target |
|---|------|-------|----------|--------|-------------------------|
| S1 | `docs/STANDARDS.md §4` (documentation correct) | `OpenApiConfig.java:38`, spec "The limits and other errors are stated", proposal, design Decision 5: "every other HTTP error" | qa (Q1), senior-dev (C1) | fixed-in-place | R3 |
| S2 | doc accuracy | `docs/PRD.md` §3.2: "Every error response … is Problem Details" (untrue for an unhandled 500; text from UC-002) | senior-dev (C2) | outstanding | R4 |
| S3 | `docs/STANDARDS.md §4` (generated, never copied), for config values | `OpenApiConfig.java:37,206`: request limits copied as text | senior-dev (C3) | waived (adapter or config text by design; the too-large example is checked live) | R5 |

## Other defects caught

| # | Kind | Where | Found by | Status | Recommendation → target |
|---|------|-------|----------|--------|-------------------------|
| D1 | plan-defect | `tasks.md` 1.1: PRD §3.3 sentence "only `POST /calculations`", untrue of the target state (B-9) | spec-reviewer (A1/P1) | fixed-by-rework | R6 |
| D2 | test-gap | `tasks.md` 2.1: the red step was only a compile failure | spec-reviewer (A2/P2) | fixed-by-rework | R1 |
| D3 | plan-defect | STANDARDS §8 "prod turns off docs" untrue until B-6, not flagged | spec-reviewer (A3/P3) | fixed-by-rework (accepted at Gate 1) | R2 |
| D4 | plan-defect | `tasks.md` 2.1: request example had no name or location | spec-reviewer (A4/P4) | fixed-by-rework | none: one-off |
| D5 | plan-defect | BR-1 numbers duplicated in `OperandOutOfRangeException` | spec-reviewer (A5/P5) | fixed-by-rework | none: one-off |

## Recommendations

- [ ] **R1**: failing-test-first output must come from assertions that ran; put trivial enabling edits (visibility, stubs) before the failing-test task → `openspec/config.yaml` (rules.tasks) (from D2) · ledger: L-6
- [ ] **R2**: when a change defers behaviour that a doc states in the present tense, list every doc that states it, and carry PRD-changing assumptions into all of them → `openspec/config.yaml` (rules.design) (from D3) · ledger: L-11
- [ ] **R3**: a claim about *all* or *every* error names the status class it covers, and is checked against a 5xx as well as a 4xx → `openspec/config.yaml` (rules.specs); use-case template (from S1) · ledger: L-16 (new)
- [ ] **R4**: decide PRD §3.2's "every error response is Problem Details": add a catch-all 500 Problem Details handler (a new backlog item), or narrow the wording to client errors (4xx) → `BACKLOG.md` / `docs/PRD.md` (from S2) · ledger: L-17 (new)
- [ ] **R5**: extend STANDARDS §4 "generated, never copied" to documented values that come from configuration, or record copied config values as an accepted limit → `docs/STANDARDS.md` (from S3) · ledger: L-18 (new)
- [ ] **R6**: doc edits stay true of the PRD's target state, not only of the current build → `openspec/config.yaml` (rules.tasks) (from D1) · ledger: L-19 (new)
