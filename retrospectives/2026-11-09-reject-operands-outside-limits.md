---
date: 2026-11-09
change: reject-operands-outside-limits
input: use-cases/UC-004-reject-operands-outside-limits.md
branch: feat/uc-4-reject-operands-outside-limits
outcome: archived
agent_runs: 11
implementer: junior-dev
claude_code: 2.1.317
orchestrator: {model: claude-opus-5-5, effort: medium}
agents:
  architect: {model: claude-opus-5-5, effort: medium, runs: 10}
  spec-reviewer: {model: claude-opus-5-5, effort: medium, runs: 5}
  junior-dev: {model: claude-opus-5-5, effort: medium, runs: 5}
  qa: {model: claude-opus-5-5, effort: medium, runs: 4}
  senior-dev: {model: claude-opus-5-5, effort: medium, runs: 4}
session: 47ef3287-b922-40a6-aa25-2e7585cba59d
session_report: reports/sessions/47ef3287-b922-40a6-aa25-2e7585cba59d.report.html
---

# Run retrospective — reject-operands-outside-limits

## Summary

This change built B-4.
- **BR-1 in the domain:** at most 100 significant digits and a scale within ±1000, checked in the `Calculation` constructor, `a` before `b`. A breach returns `400 /problems/operand-out-of-range`, naming the Operand but never its value.
- **Request size limits from Boot properties alone:** a 16,384-byte body and 1,000-character values, set through Jackson's stream read constraints in `application.yaml`, with no new code or dependency.
- **Closes the huge-Operand 500** accepted at the B-2 and B-3 Gate 1s. 137 tests pass.

This was the hardest run so far: 11 agent runs, past the pipeline's ceiling of about 10 before Gate 2. The human approved continuing.
- **Spec review:** round 1 returned REQUEST CHANGES. The reviewer ran Jackson itself and disproved the architect's risk example.
- **Apply:** the junior dev hit a plan defect and stopped correctly. A spec test value meant to be out of range was exactly at a BR-1 limit.
- **QA:** found two tests that would pass against broken code, and closed both: a `Math.abs` scale check, and echoing the value in its scientific form.
- **Gate 2:** the human asked for one PRD wording fix (C3) before the archive. The orchestrator made it directly, as a one-line doc edit the human had decided, rather than spawning another agent.

The thing most worth improving: **boundary values in plans must be computed, not assumed.** Three of this run's defects were wrong claims about what a value or library does (P1, the `98765E+1000` value, and C2/Q4). Each was caught only when someone ran it.

Ledger L-1 and L-3 each got a second record. Both still work only through the orchestrator's prompts.

The session log and report cover B-1 to B-4. The `agents` run counts above are cumulative; this change had 11 agent runs.

## Gate log

| # | Gate | Round | Verdict | Blocking | Advisory | Notes |
|---|------|-------|---------|----------|----------|-------|
| 1 | spec-reviewer | 1 | REQUEST CHANGES | 3 | 1 | P1 wrong probe example, P2 characters vs bytes, P3 `Math.abs`; P4 advisory |
| 2 | spec-reviewer | 2 | APPROVE | 0 | 2 | P1–P4 fixed-by-rework; P5, P6 advisory |
| 3 | Gate 1 (human) | — | proceed | — | — | Fix P5/P6; accept (a) the decimal size gap, (b) the limit counted in UTF-8 bytes, (c) an unrepresentable exponent as invalid-request |
| 4 | implementer blocker | — | plan defect | 1 | — | `98765E+1000` is within BR-1; the architect corrected it to `98765E+1001` (no re-review, given the run budget) |
| 5 | budget ceiling (human) | — | continue | — | — | 9 runs before QA; the human approved going past ~10 |
| 6 | qa | 1 | READY TO ARCHIVE | 0 | 4 | Q1, Q2 test gaps fixed in place; Q3, Q4 doc items |
| 7 | senior-dev | 1 | APPROVE (2 fixed in place) | 0 | 0 | C1 = Q3, C2 = Q4 fixed; C3 outstanding; C4 waived |
| 8 | Gate 2 (human) | — | fix C3, then archive | — | — | PRD §3.2 reworded by the orchestrator; build re-verified |

## Failed reviews

### F1 — spec-reviewer, round 1: REQUEST CHANGES

- **What failed:**
  - P1: the risk example and its defaults row were factually wrong (`1.` + 999 zeros is rejected, not accepted).
  - P2: the spec and the size message said "characters" while the PRD edit said "bytes".
  - P3: the scale check allowed `Math.abs`, which overflows at `Integer.MIN_VALUE`.
- **Root cause:**
  - P1: model slip; the probe result was misreported.
  - P2: plan omission; an assumption changed the PRD without reaching the spec.
  - P3: model slip; domain correctness was justified by how the adapter parses.
- **Preventable?** partly
- **Recommendation:** record the exact input, configuration and outcome for every probed default (R1); carry PRD-changing assumptions into the spec and the messages (R3).

### F2 — implementer, apply: blocked on a plan defect

- **What failed:** the R-2 scenario and tasks 2.1/3.1 used `98765E+1000` as an out-of-range value. It has scale -1000, which BR-1 accepts, so a correct check failed the test.
- **Root cause:** model slip. Neither the architect nor two spec-review rounds computed the boundary value.
- **Preventable?** yes
- **Recommendation:** the spec reviewer computes the precision and scale of every BR-n boundary example (R7).

### F3 — Gate 2 (human): revise

- **What failed:** PRD §3.2 said `1e2147483648` "cannot be represented as a decimal at all", which is false for the JDK (C3).
- **Root cause:** model slip, carried from the design (C2/Q4) into task 1.1's dictated PRD text.
- **Preventable?** partly
- **Recommendation:** a claim about which layer refuses an input names that layer and is probed at that layer alone (R1).

## Standards violations

| # | Rule | Where | Found by | Status | Recommendation → target |
|---|------|-------|----------|--------|-------------------------|
| S1 | `.claude/sdlc-profile.md` Implementation rules (docs and spec agree) | spec "Oversized request is rejected"; design Decision 3 | spec-reviewer (P2) | fixed-by-rework | R3 |
| S2 | ledger L-1 (departures recorded) | design.md, tasks.md: post-Gate-1 correction of `98765E+1000` unrecorded | qa (Q3), senior-dev (C1) | fixed-in-place | R2 |
| S3 | `docs/STANDARDS.md §4` (generated, never copied), in spirit | `OperandOutOfRangeException.java:8`, `ApiExceptionHandler.java:53` | senior-dev (C4) | waived (literal text specified by design; tests pin it; revisit in B-5) | none |

## Other defects caught

| # | Kind | Where | Found by | Status | Recommendation → target |
|---|------|-------|----------|--------|-------------------------|
| D1 | plan-defect | design.md Risks / defaults row: wrong probe example | spec-reviewer (P1) | fixed-by-rework | R1 |
| D2 | plan-defect | design.md Decision 1: `Math.abs` allowed | spec-reviewer (P3) | fixed-by-rework | none: one-off |
| D3 | plan-defect | design.md A-2: unrepresentable exponent called "not covered" instead of a departure from 4a | spec-reviewer (P4) | fixed-by-rework | R4 |
| D4 | plan-defect | design.md defaults row credits the wrong Jackson limit; scenarios said "characters" | spec-reviewer (P5, P6) | fixed-by-rework (Gate 1 fold-in) | none: one-off |
| D5 | plan-defect | spec R-2 scenario and tasks 2.1/3.1: `98765E+1000` within BR-1 | junior-dev (blocker) | fixed-by-rework | R7 |
| D6 | test-gap | `CalculationTest`: a `Math.abs` scale check passed every test | qa (Q1) | fixed-in-place | R5 |
| D7 | test-gap | R-2 tests: an echo via `toString()` (`9.8765E+1005`) passed every test | qa (Q2) | fixed-in-place | R6 |
| D8 | code-defect (doc) | design.md Decision 5: "`1e2147483648` cannot become a BigDecimal" | qa (Q4), senior-dev (C2) | fixed-in-place | R1 |
| D9 | code-defect (doc) | PRD §3.2: "cannot be represented as a decimal at all" | senior-dev (C3) | fixed at Gate 2 by the orchestrator | R1 |

## Recommendations

- [ ] **R1**: every default or library claim marked "Probed" records the exact input, configuration and outcome. A claim about which layer refuses an input names that layer and is probed at that layer alone → `openspec/config.yaml` (rules.design) (from F1, F3, D1, D8, D9) · ledger: L-3
- [ ] **R2**: any edit to the spec delta or tasks after Gate 1, by any agent including the architect on a routed blocker, is a departure, recorded in design.md and tasks.md in the same edit → plugin `agents/architect.md`, `agents/junior-dev.md` (from S2) · ledger: L-1
- [ ] **R3**: an assumption that changes a PRD statement also updates the spec requirement and any user-facing message that restates it → `openspec/config.yaml` (rules.design) (from F1, S1) · ledger: L-11 (new)
- [ ] **R4**: the use-case template's edge-case prompt asks about "values the platform cannot represent at all" → plugin `write-use-case` template (from D3) · ledger: L-12 (new)
- [ ] **R5**: every "Required:" or "Do not" constraint in design.md has a named test in tasks.md that fails if it is broken → `openspec/config.yaml` (rules.tasks) (from D6) · ledger: L-13 (new)
- [ ] **R6**: a "never echoes the value" scenario asserts against the value's `toString()` and `toPlainString()` forms too, or uses a digit run that survives every rendering → `openspec/config.yaml` (rules.specs) (from D7) · ledger: L-14 (new)
- [ ] **R7**: the spec reviewer computes (in jshell or the build) the precision and scale of every BR-n boundary example and checks its in/out label → plugin `agents/spec-reviewer.md`; profile Plan review checklist (from F2, D5) · ledger: L-15 (new)
