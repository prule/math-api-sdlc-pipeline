# Design

## Context

B-2 built `POST /api/v1/calculations` for `ADD`, `SUBTRACT` and `MULTIPLY`. `domain.Operation` is an enum. Each constant takes a `BinaryOperator<BigDecimal>`. `Calculation.result()` returns `operation.apply(a, b).stripTrailingZeros()`. `CalculationResponse.from` calls `result()` and renders it with `toPlainString()`. `ApiExceptionHandler` is the single `@RestControllerAdvice`. It has one `@ExceptionHandler` per domain exception. `UnknownOperationException` builds its message from `Operation.values()`. For the motivation, see proposal.md - Why. For the requirements, see `specs/calculation/spec.md`.

**Rule for the implementer (ledger L-1):** sometimes you must depart from this design or from tasks.md. Examples: a new dependency, a config property, a class that is not named here, or a default in "Framework and JDK defaults relied on" that turns out different. In that case, update **both** design.md and tasks.md in the same apply step, before QA, and say so in your report.

## Goals / Non-Goals

**Goals:**
- Add `DIVIDE` the way STANDARDS.md §3 says: one `Operation` constant, one domain exception for the new kind of failure (BR-2) and its Problem Details handler, and tests. Nothing else.

**Non-Goals:**
- A `Calculation` constructor check, an application-layer check, a domain exception base class, a catch-all `500` handler.
- `Operation.arity()` / BR-9 (B-8), BR-1 limits (B-4), OpenAPI (B-5).

## Use case mapping

| UC-003 item | Covered by |
|---|---|
| Main flow 1 (`DIVIDE`, `a` dividend, `b` divisor, as in UC-002) | Req "DIVIDE is correctly rounded" (number and text forms in its scenarios). The existing UC-002 reqs are unchanged |
| Main flow 2 / 2a (checked as in UC-002) | Existing reqs "Invalid request is rejected", "Unknown operation is rejected" (modified: `DIVIDE` is listed) |
| Main flow 3 / 3a / BR-2 (`b` zero in any form, number or text) | Req "Division by zero is rejected": 6 zero-form scenarios plus `0 ÷ 0` |
| Main flow 4 / BR-3 (34 digits, half even, exact not padded) | Req "DIVIDE is correctly rounded": `1 ÷ 3`, `2 ÷ 3`, two ties, `1 ÷ 4` |
| Main flow 5 / BR-4 (normalise, echo) | Req "DIVIDE is correctly rounded": `1.50 ÷ 0.5 = 3`, `100 ÷ 0.01 = 10000`, exact-body scenarios |
| 3b (`a` zero, `b` not → `0`) | Scenario "A zero dividend gives zero" |
| 3c / R-1 (first failure only) | Req "Only the first failure is reported" (modified): 3 new scenarios |
| R-2 (reason does not echo Operands) | Scenario "The reason echoes no Operand and no internals" |
| UC-002 R-1 to R-7 unchanged; unknown-operation reason lists `DIVIDE` | Modified req "Unknown operation is rejected" |
| DOMAIN-MODEL §3 `DIVIDE` edge cases (BR-2 each zero form, `1 ÷ 3`, `1 ÷ 4`, negative divisor) | Scenarios above, plus the domain tests in tasks 2.2 |
| L-4: the PRD row `422 division-by-zero` | One `422` scenario family. The row lists one status |
| Postconditions "Nothing is stored" | No persistence exists or is added. There is no observable state to test |
| Non-goals (POWER, MODULO, BR-1, B-4, B-5, B-10) | Out of scope (proposal.md) |

## Decisions

1. **`DIVIDE` constant.** In `domain.Operation`, after `MULTIPLY` (the order is the order in the `unknown-operation` detail and in PRD §3.1):
   ```java
   DIVIDE((a, b) -> {
       if (b.signum() == 0) { // BR-2: every zero form, including 0.00 and -0
           throw new DivisionByZeroException();
       }
       return a.divide(b, MathContext.DECIMAL128); // BR-3
   }),
   ```
   `signum()` ignores scale, so `0`, `0.00` and `-0` (which `BigDecimal` parses to plain zero) are all caught. The check must run **before** `divide`. `BigDecimal.divide` throws its own `ArithmeticException` ("Division by zero" or "Division undefined" for `0/0`). That exception would give a `500`, not a `422`.

2. **`domain.DivisionByZeroException extends RuntimeException`.** It has a no-arg constructor with the fixed message `Division by zero is not allowed: the divisor b is zero.` It carries no Operand (UC-003 R-2) and is not HTTP-aware (STANDARDS.md §3). It does not extend `ArithmeticException`, so it cannot be confused with JDK failures. It follows the same pattern as `UnknownOperationException`.

3. **Handler.** In `ApiExceptionHandler`, add `@ExceptionHandler(DivisionByZeroException.class)`. It returns `ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage())` with `type` `URI.create("/problems/division-by-zero")` (a new constant beside the others) and `title` `Division by zero`. This mirrors `handleUnknownOperation`. Use `HttpStatus.UNPROCESSABLE_CONTENT` (status 422, the RFC 9110 name). `UNPROCESSABLE_ENTITY` is deprecated in Spring Framework 7.0.10 (checked with javap against the cached `spring-web-7.0.10.jar`).

4. **Where BR-2 runs: in the Operation, so it fires at `result()`.** `Calculation.result()` is first called by `CalculationResponse.from` in the controller. The exception then leaves the controller method and reaches the advice. The order of failures holds without code. Jackson and `@Valid` (invalid request) run before the controller method. `Operation.named` (unknown operation) runs in `PerformCalculation.perform`. `result()` (division by zero) runs after both. B-4 puts BR-1 in the `Calculation` constructor, which still comes before `result()`, so it matches the PRD §3.2 order. `PerformCalculation` and `UseCaseConfig` do not change, so there is no new application test (task order steps 4 and 5 have no work).
   *Alternatives rejected:* (a) A zero check in the `Calculation` constructor. It puts an Operation-specific rule into `Calculation`, and STANDARDS.md §3 calls that drift. (b) A check in `PerformCalculation`. It puts a business rule outside the domain. (c) Calling `result()` eagerly in `PerformCalculation`. It changes a layer for no observable gain.
   *Trade-off:* a `Calculation(DIVIDE, 1, 0)` object can exist, and only `result()` rejects it. That is acceptable: the object is never stored or shown without its Result.

5. **Javadoc on `Calculation.result()`.** It now reads "Exact (BR-3) and normalised (BR-4)…". Change it to "Rounded only where BR-3 says, and normalised (BR-4); a zero Result strips to `BigDecimal.ZERO`." This is a comment fix, not a change in behaviour.

6. **Existing tests that named `DIVIDE` as unsupported** now use `POWER`: `OperationTest.rejectsAnUnsupportedName` and `CalculationControllerTest.rejectsAnOperationNotYetSupported` (spec scenario "Operation not yet supported"). `rejectsASupportedNameInTheWrongLetterCase` asserts `ADD, SUBTRACT, MULTIPLY, DIVIDE`. These are test edits, which STANDARDS.md §3 allows.

7. **No new dependency (L-2: none to name), no smoke-test change.** The single `@SpringBootTest` stays as it is. The web-slice test proves the advice mapping. Adding a DIVIDE request to the smoke test would duplicate that coverage.

### Design drift check (STANDARDS.md §3)

**No drift.** The production edits are: the `DIVIDE` constant (allowed), `DivisionByZeroException` and its handler (allowed for a new kind of failure), and a Javadoc-only line in `Calculation` that was already wrong for a rounding Operation. No other class, config or doc changes. One near-miss is worth recording. The `unknown-operation` detail and the web test helper `expectProblem` (which hard-codes `400`) are the only places that know the set of Operations or statuses. The detail is generated. The helper is test code: task 3.1 adds a `422` sibling instead of loosening it.

## Framework and JDK defaults relied on (ledger L-3)

Confirm each one against the pinned versions: JDK 25, Boot 4.1.2 / Spring Framework 7.0.x / Jackson 3 (`tools.jackson`). If one differs, the spec scenario governs. Fix the code and record the change here and in tasks.md (L-1).

| Default | Relied on for | Pinned by | Pre-checked |
|---|---|---|---|
| `BigDecimal.divide(BigDecimal, MathContext)` returns the exact quotient when it fits in `mc.getPrecision()` digits, else rounds to that many significant digits with `mc.getRoundingMode()` | BR-3: exact `1 ÷ 4 = 0.25`, rounded `1 ÷ 3` | "An exact quotient is not padded", "One divided by three…" | Yes, jshell on JDK 25.0.4: every DIVIDE value in the spec matched |
| `MathContext.DECIMAL128` is 34 digits, `RoundingMode.HALF_EVEN` | BR-3 | Tie scenarios | Yes (same run) |
| `BigDecimal.divide` throws `ArithmeticException` for a zero divisor | Why Decision 1 checks first | `0 ÷ 0` scenario | JDK Javadoc |
| `new BigDecimal("-0").signum() == 0` and `new BigDecimal("0.00").signum() == 0` | BR-2 every zero form | Zero-form scenarios | Yes (jshell) |
| Jackson reads the JSON numbers `0.00` and `-0` into a `BigDecimal` zero (not a parse error, not `double`) | BR-2 number forms | "…as a number" scenarios | Yes, confirmed in apply: `rejectsAScaledZeroDivisorAsANumber` and `rejectsANegativeZeroDivisorAsANumber` give `422 division-by-zero`, not `400 invalid-request` |
| `stripTrailingZeros()` of zero returns `BigDecimal.ZERO`; `toPlainString()` never uses an exponent | BR-4 | "A zero dividend gives zero", "A large quotient…" | Already confirmed in B-2 |
| An `@ExceptionHandler` method on the `ResponseEntityExceptionHandler` subclass that returns `ProblemDetail` is written as `application/problem+json` with the status from the `ProblemDetail` | 422 mapping | "Zero divisor as text" | Yes, confirmed in apply: `rejectsAZeroDivisorAsText` asserts status `422`, `application/problem+json` and `$.status` `422` |
| `HttpStatus.UNPROCESSABLE_CONTENT` is 422; `UNPROCESSABLE_ENTITY` is deprecated | Decision 3 | "Zero divisor as text" (status 422) | Yes: javap on spring-web 7.0.10; confirmed in apply (compiles without deprecation warning, tests get `422`) |

## Risks / Trade-offs

- **[Accepted risk, open until B-4 (ledger L-7)]** There are still no Operand limits. Example: `DIVIDE` of `1E-999999999` by `1E+999999999` overflows the `BigDecimal` scale (`ArithmeticException`). Rendering a huge Result with `toPlainString()` can also exhaust memory. Either way the response is a `500` from Boot's `/error`, not Problem Details. This breaks the requirement **"Every rejection is Problem Details without internals"** (calculation spec, UC-002 R-7, PRD §3.2). B-2 already accepted the same risk for `ADD`/`MULTIPLY`. DIVIDE widens it. **Gate 1 accepted it for this change (2026-11-09), until B-4.** B-4 (BR-1) closes it. Do not add ad-hoc limits or a catch-all handler here.
- [BR-2 fires lazily, at `result()`] → This is covered by Decision 4. A future caller that builds a `Calculation` without reading `result()` would not see the rejection. No such caller exists, and the order scenarios pin the current behaviour.
- [JSON number `-0` handling in Jackson 3] → The defaults table and two spec scenarios catch any difference.
- [STANDARDS.md §4: every endpoint and error `type` in the OpenAPI docs] → Deferred to B-5. B-5 must document `/problems/division-by-zero`.

## Migration Plan

None. This is an additive Operation in `/api/v1` (PRD §3.4). To roll back, revert the change.
