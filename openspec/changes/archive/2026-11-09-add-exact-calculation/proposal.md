# Proposal

## Why

UC-002 (B-2): a Client service needs the exact Result of adding, subtracting or multiplying two Operands, or a clear reason why its request was rejected. This is the product's core goal for the Operations that need no rounding (BR-3), and the first vertical slice through `domain`, `application`, `adapter.in.web` and `config`.

## What Changes

- **`POST /api/v1/calculations`** for `ADD`, `SUBTRACT` and `MULTIPLY` (PRD §3.2). Operands `a` and `b` may be JSON numbers or strings (R-1). The `200` response echoes the Operation and both Operands as given, in plain notation with their given scale (R-5), plus the exact (BR-3), normalised (BR-4) Result. Every decimal value is a JSON string (R-6).
- **Rejections as RFC 9457 Problem Details** (`application/problem+json`) from one `@RestControllerAdvice`: `400` `/problems/invalid-request` (malformed body, missing or `null` field, non-decimal Operand; R-2) and `400` `/problems/unknown-operation` (any name other than `ADD`, `SUBTRACT`, `MULTIPLY`, case-sensitive; the reason lists the supported Operations; R-3). Only the first failure is reported, invalid request before unknown operation (R-4).
- **Framework errors become Problem Details too** (R-7): `404`, `405`, `415` and the like keep Spring's standard `type` `about:blank` with the status title and a detail that exposes no internals. This is the "single advice" B-1 deferred.
- **Domain**: `Operation` enum (`ADD`, `SUBTRACT`, `MULTIPLY`) with exact, case-sensitive lookup; `Calculation` record with a normalised Result; `UnknownOperationException`.
- **New dependency**: `org.springframework.boot:spring-boot-starter-validation` (presence checks on the request; see design.md Decision 6). STANDARDS.md §1 already lists validation in the stack.

## Out of scope

- `DIVIDE` (B-3) and any rounding. Until B-3, `"DIVIDE"` is rejected as an unknown operation.
- Operand limits BR-1 and request-size limits (B-4). Until B-4, a huge Operand or exponent (for example `1E+999999999`) is not rejected; see design.md Risks.
- OpenAPI docs, springdoc and `ApiDocumentationTest` (B-5). Task order step 8 has no work in this change.
- Arity and BR-9 (unary Operations): every Operation here takes two Operands; a missing `b` is an invalid request under R-2.
- `GET /api/v1/operations` (B-9). Persistence, authentication, client-chosen precision (PRD §5).

## Decisions confirmed at Gate 1

- **Arity deferred to B-8.** `Operation.arity()` is added with B-8, the first unary Operation, together with BR-9. Reason: STANDARDS.md §5 (no speculative code), even though DOMAIN-MODEL.md §1 and STANDARDS.md §3 mention arity. Nothing in this change would read it.
- **Accepted until B-4 and B-5:** huge Operands can cause a `500` or run out of memory (no BR-1 limits until B-4), and the endpoint has no OpenAPI docs (B-5).

## Docs to update

- **PRD.md**: yes. §3.2: one bullet stating that framework errors outside the error table (for example `404`, `405`, `415`) use `type` `about:blank` with the HTTP status as `title` (RFC 9457 §4.2.1); add a §8 change-log row. Everything else UC-002 needs is already in PRD §3.2.
- **DOMAIN-MODEL.md**: none. No new term or business rule: UC-002 uses Operand, Operation, Calculation, Result, BR-3 and BR-4 as already defined.
- **STANDARDS.md**: none. The validation starter is already in the §1 stack.

## Capabilities

### New Capabilities

- `calculation`: how a Client service performs a Calculation over HTTP, what the Result looks like, and how an invalid or unsupported request is rejected.

### Modified Capabilities

None.

## Impact

- New production classes: `domain.Operation`, `domain.Calculation`, `domain.UnknownOperationException`, `application.PerformCalculation`, `config.UseCaseConfig`, `adapter.in.web.CalculationController`, `CalculationRequest`, `CalculationResponse`, `ApiExceptionHandler`.
- New tests: `OperationTest`, `CalculationTest`, `PerformCalculationTest`, `CalculationControllerTest`; one smoke test added to the existing `MathApiApplicationTest`.
- `build.gradle.kts`: one new dependency (validation starter).
- Public surface: `POST /api/v1/calculations`; Problem Details for every error response from the MVC layer.
