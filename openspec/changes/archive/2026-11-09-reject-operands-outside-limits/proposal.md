# Proposal

## Why

`POST /api/v1/calculations` accepts any Operand and any request size today. A value such as `1e999999999` is short to send but huge to render, so it can use up the service's time or memory and gives a `500` that is not Problem Details. B-2 and B-3 accepted that risk until this change (B-4, UC-004). BR-1 and the PRD §3.2 request limits close it.

## What Changes

- Every Operand is checked against BR-1 as written (at most 100 significant digits, scale within ±1000), `a` before `b`, before any arithmetic. A failure is `400` Problem Details `type` `/problems/operand-out-of-range`. The `detail` names the Operand (`a` or `b`) and never its value (UC-004 R-2).
- A request body over 16,384 bytes (UTF-8), or any JSON string or number over 1,000 characters, is rejected as `400` `/problems/invalid-request` before any value is read as a number (UC-004 R-1). The `detail` states the size limits.
- The order of failures becomes: invalid request (including size) → unknown operation → operand out of range (`a` before `b`) → division by zero (UC-004 R-3, PRD §3.2).
- An Operand whose exponent cannot be represented at all (for example `1e2147483648`) is an invalid request, not a `500`. This departs from UC-004 4a and needs Gate 1 acceptance (design.md Decision 5, A-2).
- No new dependency. One new domain exception and its handler, a BR-1 check in the `Calculation` constructor, three Spring Boot Jackson properties in `application.yaml`.

**Kept from the use case, not in PRD/DOMAIN-MODEL:** "`a` is checked before `b`" (UC-004 4b, R-3). Kept: first-failure reporting needs a fixed order, `a` before `b` is the order of the Calculation, and the DOMAIN-MODEL already says the constructor checks "BR-1 on each Operand present". This change records it in both docs.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `calculation`: adds "Operand outside the limits is rejected" (BR-1), "Oversized request is rejected" (R-1) and "Operand that cannot be represented is rejected"; modifies "Only the first failure is reported" to add the size limit, operand out of range and `a` before `b`. The Purpose line stays true (ledger L-10: no follow-up needed).

## Out of scope

- `POWER` and `MODULO`, with BR-5, BR-6 and the Result bound (B-6, B-7). Limits on the Result of `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`: bounding the Operands bounds them.
- OpenAPI docs for `/problems/operand-out-of-range` (B-5).
- Configurable limits (PRD: fixed). `Operation.arity()` and BR-9 (B-8).
- Rate limiting, authentication (PRD §5, B-12). A catch-all `500` handler.

## Docs to update

- **PRD.md**: §3.2 order line gains "`a` before `b`"; the request-limit bullet states that the body limit counts the bytes of the UTF-8 body (design.md Decision 2, A-1) and that an unrepresentable number is `invalid-request` (A-2). Change log entry.
- **DOMAIN-MODEL.md**: §2 "checks BR-1 on `a`, then on `b`; only the first failure is reported". Change log entry. BR-1 text unchanged.
- **STANDARDS.md**: none.

## Impact

- Code: `domain.Calculation` (BR-1 check), new `domain.OperandOutOfRangeException`, `adapter.in.web.ApiExceptionHandler` (new handler; size-specific `detail`), `src/main/resources/application.yaml` (Jackson read constraints). Javadoc on `domain.UnknownOperationException`.
- API: new `400` `/problems/operand-out-of-range` responses; some requests that gave `200` or `500` now give `400`. Non-breaking under PRD §3.4 (the PRD already lists this error).
- Closes the accepted "huge Operand gives a non-Problem-Details `500`" risk from B-2/B-3 (design.md Risks).
