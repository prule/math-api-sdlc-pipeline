# Proposal

## Why

Client services can add, subtract and multiply, but not divide (UC-003, backlog B-3). Division is the first Operation that can round (BR-3) and the first that can be rejected by a domain rule (BR-2, `422`). So this change also checks that STANDARDS.md §3 holds: a new Operation needs only a constant, its rules and tests, plus a domain exception and its Problem Details mapping for a new kind of failure.

## What Changes

- `POST /api/v1/calculations` accepts the Operation `DIVIDE`. The Result is `a ÷ b`, correctly rounded to 34 significant digits, half to even (BR-3), then normalised (BR-4). An exact quotient that fits is not padded.
- A `DIVIDE` whose `b` is zero, in any form (`0`, `0.00`, `-0`, as a JSON number or string), is rejected with `422` and Problem Details `type` `/problems/division-by-zero` (BR-2). Its `detail` does not echo the Operands (UC-003 R-2).
- Only the first failure is reported: invalid request, then unknown operation, then division by zero (UC-003 R-1, PRD §3.2 order).
- The `unknown-operation` `detail` now lists `DIVIDE`. It is generated from the Operations, so no code changes for this.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calculation`: adds the requirements "DIVIDE is correctly rounded" and "Division by zero is rejected". Modifies "Unknown operation is rejected" (the list of supported names now includes `DIVIDE`, and the "not yet supported" example moves from `DIVIDE` to `POWER`) and "Only the first failure is reported" (division by zero is now third in the order).

## Out of scope

- `POWER` and `MODULO`, including their BR-2 cases (B-7).
- Operand limits (BR-1) and request-size limits (B-4). Until B-4, a huge Operand can still cause a `500` (see design.md Risks).
- OpenAPI docs and `ApiDocumentationTest` (B-5).
- `Operation.arity()` and BR-9 (deferred to B-8 at the B-2 Gate 1).
- Client-chosen precision or rounding (PRD §5, B-10).

## Docs to update

- **PRD.md:** none. §3.1 already describes `DIVIDE`, §3.2 already has the `division-by-zero` row (`422`) and the order of failures.
- **DOMAIN-MODEL.md:** none. BR-2, BR-3, BR-4, the `DIVIDE` edge cases (§3) and `DivisionByZeroException` (§2) are already written. This change adds no new term or rule.
- **STANDARDS.md:** none.

## Impact

- Code: `domain.Operation` (new constant `DIVIDE`), new `domain.DivisionByZeroException`, one new handler in `adapter.in.web.ApiExceptionHandler`. There is also a Javadoc correction on `domain.Calculation.result()`, which says the Result is "Exact (BR-3)". That is no longer true for every Operation.
- Tests: `OperationTest`, `CalculationTest`, `CalculationControllerTest`. `PerformCalculation` and `UseCaseConfig` do not change (design.md Decision 4). The existing tests that use `DIVIDE` as an unsupported name change to `POWER`.
- API: non-breaking addition in `/api/v1` (PRD §3.4). There are no new dependencies.
