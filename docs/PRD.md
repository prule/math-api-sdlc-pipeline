# Math API — Product Requirements

> Living document. Update it in the same change as any change to scope, API contract or operations (see `CLAUDE.md`).
> Related: [Domain Model](DOMAIN-MODEL.md) · [Standards](STANDARDS.md)

## 1. Problem and goal

Clients need exact, predictable arithmetic over HTTP without each one re-implementing decimal handling.
Math API is a stateless REST service that applies one arithmetic operation to one or two operands and returns an exact decimal result.

## 2. Users

Internal services and developers (portfolio / learning-grade). No public-internet audience in v1.

## 3. Scope (v1)

### 3.1 Operations

| Operation  | Meaning | Notes |
|------------|---------|-------|
| `ADD`      | a + b   | |
| `SUBTRACT` | a − b   | |
| `MULTIPLY` | a × b   | |
| `DIVIDE`   | a ÷ b   | Rounded to 34 significant digits (BR-3); b = 0 rejected (BR-2) |
| `POWER`    | a ^ b   | Integer b, \|b\| ≤ 999 (BR-5); exact power rounded to 34 digits (BR-3); result scale within ±1000 (BR-6); 0 ^ negative rejected (BR-2) |
| `MODULO`   | a mod b | Truncated remainder, sign of a: −7 mod 3 = −1 (BR-7); exact; b = 0 rejected (BR-2) |
| `SQRT`     | √a      | Unary. Rounded to 34 significant digits (BR-3); negative a rejected (BR-8) |
| `NEGATE`   | −a      | Unary. Exact |

`SQRT` and `NEGATE` are unary (operand `a` only); all others are binary (BR-9). Semantics and rules live in [DOMAIN-MODEL.md](DOMAIN-MODEL.md).

### 3.2 API contract

`POST /api/v1/calculations`

Request — operands may be JSON numbers or strings:

```json
{ "operation": "DIVIDE", "a": 1, "b": "3" }
```

`200 OK` — all numeric values returned as strings to avoid client-side precision loss:

```json
{ "operation": "DIVIDE", "a": "1", "b": "3", "result": "0.3333333333333333333333333333333333" }
```

A unary Operation takes no `b`, and its response has no `b` key:

```json
{ "operation": "SQRT", "a": "2" }
```

```json
{ "operation": "SQRT", "a": "2", "result": "1.414213562373095048801688724209698" }
```

- `operation` must be exactly a supported name (case-sensitive: `ADD`, not `add`).
- `b` is required for a binary Operation and must be omitted (or `null`) for a unary one; otherwise the request is rejected as `invalid-request` (BR-9).
- `a` and `b` are echoed as received, in plain notation and with their given scale (`1e2` → `"100"`, `"1.50"` → `"1.50"`). Only `result` is normalised (BR-4).
- Every error response (including framework errors such as 415) is Problem Details with `type`, `title`, `status` and a human-readable `detail` that exposes no internals.
- Request limits: a body over 16,384 characters, or any JSON string or number over 1,000 characters, is rejected as `invalid-request` before parsing into numbers. Operand limits (BR-1) are reported as `operand-out-of-range`, naming the operand but never echoing its value.
- Only the first failure is reported, checked in this order: invalid request → unknown operation → operand count (BR-9, reported as invalid request) → operand out of range (BR-1) → invalid exponent (BR-5) → division by zero (BR-2) → negative square root (BR-8) → result out of range (BR-6).

`GET /api/v1/operations` lists the supported Operations, derived from the domain so it always matches what `POST /api/v1/calculations` accepts:

```json
[ { "name": "ADD", "arity": 2 }, { "name": "SUBTRACT", "arity": 2 }, …, { "name": "SQRT", "arity": 1 }, { "name": "NEGATE", "arity": 1 } ]
```

- One entry per Operation, in the same order as the `unknown-operation` detail. `name` is the exact value to send as `operation`; `arity` is a JSON integer, 1 or 2 (BR-9).
- It takes no input and returns only `200`. Methods other than `GET` and `HEAD` get `405` Problem Details.

Errors use RFC 9457 Problem Details (`application/problem+json`):

| Status | When | `type` |
|--------|------|--------|
| 400 | Malformed JSON, missing field, `b` not matching the Operation's arity (BR-9), non-numeric operand | `/problems/invalid-request` |
| 400 | Unknown `operation` | `/problems/unknown-operation` |
| 400 | Operand outside limits (BR-1) | `/problems/operand-out-of-range` |
| 422 | Division by zero (BR-2) | `/problems/division-by-zero` |
| 422 | `POWER` exponent not an integer or \|b\| > 999 (BR-5) | `/problems/invalid-exponent` |
| 422 | `SQRT` of a negative operand (BR-8) | `/problems/negative-square-root` |
| 422 | `POWER` result scale outside ±1000 (BR-6) | `/problems/result-out-of-range` |

### 3.3 Supporting endpoints

- `GET /actuator/health` — liveness/readiness.
- OpenAPI 3.1 docs via springdoc: `/v3/api-docs` (JSON), `/swagger-ui.html` (interactive). The allowed `operation` values are generated from the domain, so a new Operation appears without doc edits. Unauthenticated, like the API in v1. **On by default; off under the `prod` profile** (`SPRING_PROFILES_ACTIVE=prod`), where `/v3/api-docs`, `/swagger-ui.html`, `/swagger-ui/**` and the swagger-ui webjar return 404. Each part can still be switched on its own via `springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled`.

### 3.4 Versioning

Path-based (`/api/v1`). Adding an operation is non-breaking and stays in v1. Any breaking contract change ships as `/api/v2`.

## 4. Non-functional requirements

- **Exactness:** `BigDecimal` throughout; `0.1 + 0.2` returns `"0.3"`.
- **Safety:** operand limits (BR-1) prevent CPU-exhaustion via huge exponents.
- **Stateless:** no database, no session; any instance can serve any request.
- **Stack:** Java 25, Spring Boot 4 — see [STANDARDS.md](STANDARDS.md).

## 5. Non-goals (v1)

- Authentication, authorization, rate limiting.
- Persistence / calculation history.
- Expression parsing (`"2 + 3 * 4"`) or n-ary operations (`sum[1,2,3]`).
- Client-chosen precision or rounding mode.
- HATEOAS links.

## 6. Roadmap / known future changes

| Item | Impact when it lands |
|------|---------------------|
| Client-chosen precision | New optional request field; non-breaking |
| Calculation history | Adds a repository port + persistence adapter |
| Auth / rate limiting | New adapter-level concern |

## 7. Success criteria (v1)

- All four operations return correct exact results, including the edge cases listed in the domain model.
- Every error path in §3.2 returns the stated status and Problem Details `type`.
- `./gradlew build` passes in CI (tests, ArchUnit, Spotless).

## 8. Change log

| Date | Change |
|------|--------|
| 2026-11-09 | Copied from math-api (its final v1 docs) as the target for the sdlc-pipeline rebuild |
