# Design

## Context

B-1 left a Boot 4.1.2 skeleton: `MathApiApplication`, Actuator health, Spotless, CI and `ArchitectureTest` (rules written with `allowEmptyShould(true)` because the layer packages were empty). `MathApiApplicationTest` is the project's one `@SpringBootTest` (STANDARDS.md §6). No class exists yet in `domain`, `application`, `adapter.in.web` or `config`. Motivation: see proposal.md - Why. Requirements: `specs/calculation/spec.md`.

**Rule for the implementer (ledger L-1):** if you depart from this design or tasks.md (a new dependency, a version pin, a config property, a class not named here, a framework default that turns out different), update design.md and tasks.md in the same apply step, before QA, and say so in your report.

## Goals / Non-Goals

**Goals:**
- The `calculation` spec, met with the four layers STANDARDS.md §2 prescribes and the smallest class set that fills them.
- A `@RestControllerAdvice` that every later use case extends with one handler per new domain exception.

**Non-Goals:**
- `Operation.arity()` and BR-9: no unary Operation exists yet (UC-002 non-goal); arity lands with B-8, the first unary Operation, together with the rule that uses it. A field nothing reads would be speculative (STANDARDS.md §5). Confirmed at Gate 1 (proposal.md).
- A domain exception base class, a catch-all `500` handler, custom Jackson configuration, a `GET` for operations.
- Bounding Operand size (B-4) and OpenAPI annotations (B-5).

## Use case mapping

| UC-002 item | Covered by |
|---|---|
| Main flow 1 (Operation + two Operands, number or text) | Req "Operands are accepted as numbers or text" |
| Main flow 2 (well formed, supported) | Reqs "Invalid request is rejected", "Unknown operation is rejected" |
| Main flow 3 (exact, BR-3) | Reqs "ADD / SUBTRACT / MULTIPLY is exact" |
| Main flow 4 (normalise, BR-4) | Req "Result is normalised" |
| Main flow 5 (echo, R-5, R-6) | Req "The Calculation is echoed as given" |
| 2a / R-2 | Req "Invalid request is rejected" |
| 2b / R-3 | Req "Unknown operation is rejected" |
| 2c / R-4 | Req "Only the first failure is reported" |
| R-7 | Req "Every rejection is Problem Details without internals" |
| DOMAIN-MODEL §3 edge cases (ADD, SUBTRACT, MULTIPLY) | Scenarios under the three exact-Operation reqs |
| Postcondition "Nothing is stored" | No persistence exists or is added; not separately tested (no observable state to assert on) |
| Non-goals (DIVIDE, BR-1, B-4, B-5, B-7/8/9) | Out of scope (proposal.md) |

## Decisions

1. **Classes and layers.**
   - `domain.Operation` — enum `ADD`, `SUBTRACT`, `MULTIPLY`, each built with a `BinaryOperator<BigDecimal>` (`BigDecimal::add`, `::subtract`, `::multiply`); `apply(a, b)`; `static Operation named(String name)` loops `values()` comparing `name()` with `equals` (case-sensitive, R-3) and throws `UnknownOperationException` otherwise. Not `valueOf`: it throws `IllegalArgumentException` with an internal message and `NullPointerException` for null.
   - `domain.UnknownOperationException extends RuntimeException` — no-arg; its message is built from `Operation.values()`: `The operation is not supported. Supported operations: ADD, SUBTRACT, MULTIPLY.` (generated, so B-3 needs no edit). It does not carry the given name (R-7: no request size limits until B-4, so echoing is unbounded).
   - `domain.Calculation` — `record Calculation(Operation operation, BigDecimal a, BigDecimal b)`; compact constructor `Objects.requireNonNull` on all three; `BigDecimal result()` returns `operation.apply(a, b).stripTrailingZeros()` (BR-4). Since Java 8, `stripTrailingZeros()` of any zero returns `BigDecimal.ZERO`, so zero is `0`.
   - `application.PerformCalculation` — plain class, `Calculation perform(String operationName, BigDecimal a, BigDecimal b)` returning `new Calculation(Operation.named(operationName), a, b)`. Thin today; it is where BR-1 and later rules are sequenced (PRD §3.2 order).
   - `config.UseCaseConfig` — `@Configuration` with `@Bean PerformCalculation performCalculation()`.
   - `adapter.in.web.CalculationRequest` — `record(@NotNull String operation, @NotNull BigDecimal a, @NotNull BigDecimal b)`. `operation` is a `String`, not the enum: an enum would make Jackson reject unknown names as unreadable (invalid-request), breaking R-3/R-4.
   - `adapter.in.web.CalculationResponse` — `record(String operation, String a, String b, String result)` with `static from(Calculation)` using `toPlainString()` on `a`, `b` and `result()` (R-5, R-6, BR-4 plain notation). Strings in the record mean no Jackson number-writing setting is involved.
   - `adapter.in.web.CalculationController` — `@RestController`, `@PostMapping("/api/v1/calculations")`, `@Valid @RequestBody CalculationRequest`, delegates to `PerformCalculation`, returns `CalculationResponse`.
   - `adapter.in.web.ApiExceptionHandler` — the single `@RestControllerAdvice` (Decision 4).

2. **Echo through `BigDecimal`, not the raw text.** R-5 wants plain notation with the given scale; `BigDecimal` keeps the scale (`"1.50"` has scale 2; `1e2` is unscaled 1, scale −2), so `toPlainString()` gives `"1.50"` and `"100"`. Keeping raw text would echo `1e2` verbatim, which R-5 forbids.

3. **Order of failures comes free from Spring MVC.** Body parsing (malformed JSON, non-decimal Operand) and then `@Valid` (missing/null field) both run before the controller method; the Operation lookup runs inside the use case. So R-4's order (invalid request, then unknown operation) holds without code. The order scenarios pin it.

4. **Problem Details mapping.** `ApiExceptionHandler extends ResponseEntityExceptionHandler`, which already turns every Spring MVC exception (`405`, `415`, `404 NoResourceFoundException`, …) into a `ProblemDetail` with `type` `about:blank` and the status title (R-7, PRD bullet added in task 1.1). Overrides:
   - `handleHttpMessageNotReadable` → `400`, `type` `/problems/invalid-request`, title `Invalid request`, detail `The request body is not valid JSON, or an Operand is not a decimal number.` The parser's message is never copied (it names Jackson classes).
   - `handleMethodArgumentNotValid` → `400`, `/problems/invalid-request`, detail `Field '<name>' is required.` from the first `FieldError`'s field name (the API field names `operation`, `a`, `b`; no internals).
   - `@ExceptionHandler(UnknownOperationException.class)` → `400`, `/problems/unknown-operation`, title `Unknown operation`, detail = exception message.
   - `createResponseEntity` override (*added during apply, L-1*): if the body is a `ProblemDetail` with a `null` `type`, set it to `about:blank`. Spring Framework 7 no longer defaults `ProblemDetail.type` to `about:blank`: it is `null`, and the Jackson mixin (`@JsonInclude(NON_EMPTY)`) omits it, so the `415`/`405` responses had no `type`, which fails the R-7 scenarios. `createResponseEntity` is the hook the superclass Javadoc names for changing the body. `handleExceptionInternal` does not work because the body is still `null` there for `ErrorResponse` exceptions.
   `type` values are relative URIs (`URI.create("/problems/invalid-request")`), as PRD §3.2 writes them. Spring writes a `ProblemDetail` body as `application/problem+json`. No catch-all `Exception` handler: R-7 covers client-caused rejections; an unexpected server error falls to Boot's `/error`, which by default includes no message or trace. Alternative rejected: `spring.mvc.problemdetails.enabled=true` alone — it gives `about:blank` for everything, so the two custom handlers would still need an advice, and two mechanisms would then exist.

5. **Web-slice tests use the real use case.** `@WebMvcTest(CalculationController.class)` plus `@Import(PerformCalculation.class)`: the domain is pure and fast, so no mock is needed and the slice tests prove the real Results. `@Import` also removes any doubt about whether a slice picks up `@Configuration` classes.
   *Changed during apply (L-1):* the plan said `@Import(UseCaseConfig.class)`. `ArchitectureTest` analyses test classes too, and `CalculationControllerTest` (in `adapter.in.web`) referencing `config.UseCaseConfig` breaks the layer rule "Config may not be accessed by any layer". Importing the application class directly (Adapter → Application is allowed) keeps the real use case without weakening the rule. The `UseCaseConfig` wiring is still covered by the `@SpringBootTest` smoke test.

6. **Dependency: `org.springframework.boot:spring-boot-starter-validation`** (`implementation`, version from Boot's BOM; ledger L-2). It brings Hibernate Validator so `@Valid`/`@NotNull` work on the request record. No extra test artifact: `spring-boot-starter-webmvc-test` already provides `@WebMvcTest` and MockMvc. Alternatives rejected: `@JsonProperty(required = true)` (does not catch explicit `null`, and gives a generic parser error with no field name); hand-written null checks in the controller (input validation code in an adapter for what the stack already supplies). STANDARDS.md §1 already lists validation in the stack, so no standards change.

7. **Smoke test**: one method `performsAnAdditionEndToEnd` added to the existing `MathApiApplicationTest` (keeps exactly one `@SpringBootTest` context): `POST /api/v1/calculations` `{"operation":"ADD","a":"0.1","b":"0.2"}` → `200`, `result` `"0.3"`.

## Framework defaults relied on (ledger L-3)

Each must be confirmed against Boot 4.1.2 (Spring Framework 7, Jackson 3 `tools.jackson`), not Boot 3 / Jackson 2. If one differs, the spec scenario governs; fix the code and record the change here (L-1).

| Default | Relied on for | Scenario that pins it |
|---|---|---|
| Jackson reads a JSON number into `BigDecimal` from its text, not via `double` | `0.1 + 0.2 = 0.3` from JSON numbers (R-1, BR-3) | "Decimal fractions given as JSON numbers add exactly" |
| Jackson coerces a JSON string to `BigDecimal` (scalar coercion on by default) | R-1 | "One Operand as number, one as string" |
| Jackson rejects a boolean, and non-numeric text, for a `BigDecimal` | R-2 | "Operand is a boolean", "Operand is non-numeric text" |
| Jackson turns `""` into `null` for a `BigDecimal` (then `@NotNull` fails) or rejects it — either gives `invalid-request` | R-2 | "Operand is empty text" |
| Jackson coerces a JSON number to `String` for `operation` | `{"operation":5}` reaches the lookup and is an unknown operation (not specified; accepted) | none |
| Boot's Jackson setup ignores unknown JSON properties | Extra fields are ignored (not specified; accepted) | none |
| Jackson's default `StreamReadConstraints` (number length 1000) | Nothing in this change; a longer number already yields `invalid-request`. B-4 builds on it | none |
| `@Valid @RequestBody` failures raise `MethodArgumentNotValidException` (not `HandlerMethodValidationException`) | Decision 4 field-name detail | "Operand a missing", "Operand b null", "Operation missing" |
| `ResponseEntityExceptionHandler` maps `405`, `415`, `404` to `ProblemDetail` with `about:blank` and status title; Spring renders it as `application/problem+json` | R-7 | "Unsupported media type", "Method not allowed" |
| **Differs (confirmed in apply):** in Spring Framework 7.0.10, `ProblemDetail.getType()` is `null` by default and is left out of the JSON. Status title, `detail` and `application/problem+json` are as expected. Fix: `ApiExceptionHandler.createResponseEntity` sets `about:blank` (Decision 4) | R-7 | same |
| `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure` (Boot 4 module split, as `@AutoConfigureMockMvc` in B-1) | Tests | n/a |
| `BigDecimal.stripTrailingZeros()` of zero returns `BigDecimal.ZERO` (JDK, since 8) | BR-4 zero is `0` | "Result of zero", "Multiply by zero" |

Apply result (Boot 4.1.2, Spring Framework 7.0.10, Jackson `tools.jackson` 3.1.7): every scenario-pinned row held, except the `ProblemDetail` `type` default noted above. The Jackson rows with no pinning scenario (number to `String`, unknown properties ignored, `StreamReadConstraints`) were not checked separately, as planned.

## Risks / Trade-offs

- [No Operand limits until B-4] → `1E+999999999` is accepted; `MULTIPLY` of two such values can overflow `BigDecimal` scale (`ArithmeticException`, a `500`), and `toPlainString()` of a huge exponent can exhaust memory. That `500` comes from Boot's `/error`, not Problem Details, so it also breaks the requirement "Every rejection is Problem Details without internals" (R-7, PRD §3.2) until B-4. Gate 1 accepted both. B-4 (BR-1) closes it. Do not add ad-hoc limits or a catch-all handler here.
- [Jackson 3 coercion defaults differ from Jackson 2] → The defaults table above lists each one with the scenario that catches a difference.
- [`ArchitectureTest` rules carry `allowEmptyShould(true)` and `withOptionalLayers(true)` from B-1, when the layer packages were empty] → Every layer now has a class, so task 4.4 removes both: a rule that matches no classes (for example after a package rename) then fails instead of passing vacuously. The B-1 fixture test that proves the domain rule fails stays.
- [STANDARDS.md §4 says every endpoint is in the OpenAPI docs] → Deliberately deferred to B-5 per the backlog; no springdoc yet.

## Migration Plan

None: additive endpoint. Rollback is reverting the change.
