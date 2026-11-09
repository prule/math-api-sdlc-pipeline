# Recommendation ledger

One line for each retrospective recommendation that is still open or that recurs. The run records
link here by id, so a lesson that comes back adds a count, not another copy of its text. The
`build-use-case` orchestrator reads this file at the start of a run and updates it at the end.

## Rules

- **At step 0 of a run:** the orchestrator reads the Open lines. It tells the architect about each
  line that applies to the use case, and tells later agents about the lines that apply to their
  phase. Gate 1 lists the *due* lines that apply.
- **After a run:** the orchestrator matches each recommendation in the new record to an Open line.
  For each match, it adds 1 to *Records* (once per record) and writes the ledger id in the record
  (`· ledger: L-8`). It adds a new line for a recommendation that is still open after its run.
- **Limit:** a line with 3 or more records is *due*. At Gate 1 the human adopts it in that run, or
  writes a reason to keep it open or to close it.
- **Adopting:** edit the target files in a PR, move the line to Closed with *Status*
  `adopted, #<PR>`, and tick the matching items in the records. A line adopted during a run is
  first closed with `adopted, <branch>`; replace the branch with the PR number once the PR is open. A target in the plugin
  (an agent or skill) is adopted by an issue or PR in
  [`prule/sdlc-pipeline`](https://github.com/prule/sdlc-pipeline); put its link in *Status*.
- **Ids:** `L-<n>`, the next number after the highest in either table. An id is never reused.
- **Status:** `open`, `due` (3+ records), or a short note such as `kept: <reason>`. A closed line
  says `adopted, #<PR>` or `closed: <reason>`.

## Open

| Id | Recommendation | Target | Records | Status |
|---|---|---|---|---|
| L-1 | Implementer updates design.md and tasks.md in the same apply step when it departs from them (new dependency, pin, or unplanned config) | `.claude/sdlc-profile.md`; plugin `agents/junior-dev.md` | 2 | open |
| L-2 | List the exact Boot 4 starter and test artifacts (and the dependency-management pin) in the STANDARDS §1 Stack table | `docs/STANDARDS.md` | 1 | open |
| L-3 | Design lists the framework defaults it relies on, each confirmed against the pinned version | `openspec/config.yaml` | 2 | open |
| L-4 | Spec delta has one scenario per status a PRD error bullet lists | `.claude/sdlc-profile.md` (Plan review) | 1 | open |
| L-5 | ArchUnit layer rules also check test classes; slice tests import application classes, never `config` | `docs/STANDARDS.md §2`; `.claude/sdlc-profile.md` | 1 | open |
| L-6 | Failing-test-first tasks ask for the pasted red test output before the implementing task | `openspec/config.yaml` (rules.tasks) | 2 | open |
| L-7 | An accepted risk that breaks a spec requirement names it, for Gate 1 to accept | `openspec/config.yaml` (rules.design) | 1 | open |
| L-8 | Settle arity timing: `Operation.arity()` arrives with the first unary Operation; align STANDARDS §3 and the DOMAIN-MODEL glossary | `docs/STANDARDS.md`; `docs/DOMAIN-MODEL.md` | 1 | open |
| L-9 | Tasks write BigDecimal test values as `new BigDecimal("<text>")` | `.claude/sdlc-profile.md` | 1 | open |
| L-10 | If a change makes a capability's Purpose line untrue, add a task to correct it after archive | `openspec/config.yaml` (rules.tasks) | 1 | open |
| L-11 | An assumption that changes a PRD statement also updates the spec requirement and user-facing messages that restate it | `openspec/config.yaml` (rules.design) | 2 | open |
| L-12 | Use-case edge-case prompt asks about values the platform cannot represent at all | plugin `write-use-case` template | 1 | open |
| L-13 | Every design "Required:"/"Do not" constraint has a named test in tasks.md that fails if broken | `openspec/config.yaml` (rules.tasks) | 1 | open |
| L-14 | "Never echoes the value" scenarios assert against `toString()`/`toPlainString()` forms too | `openspec/config.yaml` (rules.specs) | 1 | open |
| L-15 | Spec reviewer computes every BR-n boundary example's precision/scale and checks its in/out label | plugin `agents/spec-reviewer.md`; `.claude/sdlc-profile.md` | 1 | open |
| L-16 | A claim about all/every error names its status class and is checked against a 5xx as well as a 4xx | `openspec/config.yaml` (rules.specs); use-case template | 1 | open |
| L-17 | Decide PRD §3.2 "every error response is Problem Details": catch-all 500 handler or narrower wording | `BACKLOG.md`; `docs/PRD.md` | 1 | open |
| L-18 | STANDARDS §4 "generated, never copied" also covers documented values from configuration | `docs/STANDARDS.md` | 1 | open |
| L-19 | Doc edits stay true of the PRD's target state, not only the current build | `openspec/config.yaml` (rules.tasks) | 1 | open |

## Closed

| Id | Recommendation | Records | Status |
|---|---|---|---|
