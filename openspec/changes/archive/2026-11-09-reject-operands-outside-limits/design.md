# Design

## Context

`POST /api/v1/calculations` reads `CalculationRequest(String operation, BigDecimal a, BigDecimal b)` with Jackson 3, checks `@NotNull`, then `PerformCalculation.perform` calls `Operation.named` (unknown operation) and `new Calculation(...)`. `Calculation.result()` applies the Operation (BR-2 fires there for `DIVIDE`). `ApiExceptionHandler` (the single advice) maps `HttpMessageNotReadableException` to `invalid-request` and each domain exception to its `type`. For the motivation, see proposal.md - Why. For the requirements, see `specs/calculation/spec.md`.

**Rule for the implementer (ledger L-1):** if you depart from this design or from tasks.md (a new dependency, a config property or class not named here, a default in "Framework and JDK defaults relied on" that turns out different), update **both** design.md and tasks.md in the same apply step, before QA, and say so in your report.

## Goals / Non-Goals

**Goals:**
- BR-1 in the domain, R-1 at the HTTP boundary, using what the stack already has. No new dependency, no new class beyond the one domain exception a new kind of failure needs (STANDARDS.md §3).

**Non-Goals:**
- A servlet filter, `RequestBodyAdvice` or custom deserializer for size limits; a catch-all `500` handler; a domain exception base class; configurable limits; any `PerformCalculation` or `UseCaseConfig` change; OpenAPI (B-5); `Operation.arity()` (B-8).

## Use case mapping

| UC-004 item | Covered by |
|---|---|
| Main flow 1, 3, 5; success postcondition (Results unchanged) | Existing `calculation` reqs, unchanged. The at-limit scenarios (`1E+1000`, `1E-1000`, 100 digits, `1E+100`) show acceptance |
| Main flow 2 / 2a / R-1 (body 16,384, value 1,000, before numbers) | Req "Oversized request is rejected": 8 scenarios incl. both exact boundaries, text and number forms, the `operation` value |
| Main flow 4 / 4a / BR-1 / R-2 | Req "Operand outside the limits is rejected": every BR-1 example, `a` and `b` named, value not echoed |
| 4b / R-3 (`a` before `b`) | Modified req "Only the first failure is reported": "Both Operands out of range" |
| 4c / R-3 (out of range before division by zero) | Same req: "Out-of-range dividend and zero divisor" |
| R-3 (invalid → unknown → out of range) | Same req: 3 new scenarios (oversized + unknown, missing + out of range, unknown + out of range) |
| R-4 (prompt, `1e999999999`) | Scenarios "A short but enormous value is rejected at once" (≤ 1 s), "Enormous scales in a division…" |
| Edge cases list (exact limits, one past, `1e999999999 × 1e999999999`, `1E-999999999 ÷ 1E+999999999`, 16,384 / 1,000 exactly) | One scenario each, above |
| Failure postcondition "never echoes", "kind and reason" | `detail` assertions in the scenarios; size `detail` states the limits |
| Failure postcondition "nothing stored, service stays available" | No persistence exists. Availability follows from R-1/BR-1 bounding work (Risks: closing the `500` risk) |
| L-4: PRD row `400 operand-out-of-range` | One status, one scenario family. The `400 invalid-request` row gains size scenarios |
| Non-goals (configurable limits, Result limits, rate limiting, B-5) | Out of scope (proposal.md) |

## Decisions

1. **BR-1 in the `Calculation` constructor, `a` then `b`.** After the `requireNonNull` checks: `checkWithinLimits("a", a); checkWithinLimits("b", b);`. The private static helper throws `new OperandOutOfRangeException(name)` when `value.precision() > MAX_SIGNIFICANT_DIGITS || value.scale() < -MAX_SCALE || value.scale() > MAX_SCALE`. Write the limits as two `private static final int` constants (`MAX_SIGNIFICANT_DIGITS = 100`, `MAX_SCALE = 1000`) with a `// BR-1` comment. `precision()` and `scale()` are on the value as parsed, which is the value as written (`new BigDecimal("1E+100")` has precision 1; Jackson keeps the written scale), and both are cheap for `1e999999999` (precision 1). This is where DOMAIN-MODEL §2 already puts it. The order of failures needs no code: Jackson and `@Valid` run before the controller; `Operation.named` is evaluated before the constructor runs in `PerformCalculation.perform`; `result()` (BR-2) runs after construction.
   *Alternatives rejected:* a check in `PerformCalculation` (a business rule outside the domain); a check in `Operation.apply` (per-Operation, and it fires after BR-2 for `DIVIDE`, breaking 4c).
   *Required:* compare the scale against both bounds as written above. Do not use `Math.abs(scale)`: `Math.abs(Integer.MIN_VALUE)` is negative, and the domain must not rely on how an adapter parses.

2. **R-1 via Jackson's stream read constraints, set with Spring Boot's own properties.** In `src/main/resources/application.yaml`:
   ```yaml
   spring:
     jackson:
       factory:
         constraints:
           read:
             # UC-004 R-1 (PRD §3.2): fixed request limits, checked by the parser before any value becomes a number.
             max-document-length: 16384
             max-string-length: 1000
             max-number-length: 1000
   ```
   Exact property names (L-2): `spring.jackson.factory.constraints.read.max-document-length`, `spring.jackson.factory.constraints.read.max-string-length`, `spring.jackson.factory.constraints.read.max-number-length` (from `spring-boot-jackson-4.1.2.jar` metadata). `max-number-length` is already Jackson's default (1000); set it anyway so R-1 does not depend on a default. These apply in every profile; `application-prod.yaml` does not override them.
   A violation throws `tools.jackson.core.exc.StreamConstraintsException` (a `JacksonException`); Spring's Jackson converter wraps every `JacksonException` as `HttpMessageNotReadableException`, which the advice already maps to `invalid-request`. The document length is counted in bytes when Jackson reads from the request `InputStream` (assumption A-1).
   *Alternatives rejected:* a `RequestBodyAdvice` or servlet filter counting bytes (more code for the same result); Tomcat `max-http-form-post-size` (forms only, and not exercised by MockMvc); a `JsonMapperBuilderCustomizer` bean (Boot already exposes the properties; STANDARDS.md §8 says not to duplicate a library's switches).

3. **A size-specific `detail`.** In `ApiExceptionHandler.handleHttpMessageNotReadable`, when `ex.getMostSpecificCause() instanceof StreamConstraintsException`, return `invalidRequest("The request is too large: at most 16,384 bytes in total, and at most 1,000 characters for any value.")`. Otherwise keep today's detail. The adapter may import Jackson (STANDARDS.md §2). Jackson's own message is never copied (it names Jackson classes). Nesting-depth violations (Jackson default 500 levels) also land here; "too large" is an acceptable reason for them.

4. **`domain.OperandOutOfRangeException extends RuntimeException`.** Constructor takes the Operand name (`"a"` or `"b"`) and builds the message `Operand <name> is out of range: an Operand may have at most 100 significant digits and a scale from -1000 to 1000.` It carries no value (R-2) and is not HTTP-aware. Handler in `ApiExceptionHandler`: `@ExceptionHandler(OperandOutOfRangeException.class)` → `ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage())`, `type` `URI.create("/problems/operand-out-of-range")` (new constant beside the others), `title` `Operand out of range`. Mirrors `handleUnknownOperation`.

5. **Unrepresentable exponents stay `invalid-request` (A-2: a departure from UC-004 4a, accepted at Gate 1, 2026-11-09).** UC-004 4a and BR-1 call a scale outside ±1000 *operand out of range*; this value's scale is outside ±1000, but it is reported as `invalid-request`. The JDK accepts `1e2147483648` (`new BigDecimal("1e2147483648")` has scale `Integer.MIN_VALUE`, which BR-1 rejects in the domain), but Jackson 3.1.7 refuses it first: it throws `InvalidFormatException` (text) or a `DatabindException` "value exceeds limits" (number), both `JacksonException`, so today's handler already gives `400 invalid-request`. No code: the two scenarios pin it. Mapping it to `operand-out-of-range` would need a custom deserializer for no change in status.

6. **Comment fix.** `UnknownOperationException`'s Javadoc says the name is not carried because "it is unbounded until B-4". Change it to: the given name is never echoed (UC-002 R-3). Behaviour unchanged.

7. **No application or smoke-test change.** `PerformCalculation` and `UseCaseConfig` do not change (task order steps 4–5 have no work). The web-slice test loads `application.yaml` and Boot's Jackson auto-configuration, so it proves the constraints; the existing smoke test must still pass. No new dependency (L-2: none to name).

### Design drift check (STANDARDS.md §3)

**No drift.** New failure kind → one domain exception + one handler (allowed). The constructor check is the place DOMAIN-MODEL §2 names. The size limits are transport config, not business rules, and live in `application.yaml` plus the advice.

## Framework and JDK defaults relied on (ledger L-3)

Confirm each against JDK 25, Boot 4.1.2 / Spring Framework 7.0.10 / Jackson 3.1.7 (`tools.jackson`). If one differs, the spec scenario governs; fix the code and record it here and in tasks.md (L-1). "Probed" = a standalone run against the cached `jackson-databind-3.1.7` / `jackson-core-3.1.7` jars with the constraints of Decision 2, reading from an `InputStream` into a record shaped like `CalculationRequest`.

| Default | Relied on for | Pinned by | Pre-checked | Apply (2026-11-09, `@WebMvcTest` via MockMvc) |
|---|---|---|---| --- |
| Boot 4.1.2 binds `spring.jackson.factory.constraints.read.*` onto the `JsonMapper` that Spring MVC's converter uses, also in `@WebMvcTest` | Decision 2 | "Body one character over the limit" | Property names in jar metadata: yes. Binding to the MVC mapper: **confirm in apply** | **Confirmed.** `rejectsABodyOneCharacterOverTheLimit` and `rejectsAnOperationNameOverTheLimit` were red (`200` / `unknown-operation`) before the `application.yaml` properties and green after, so Boot binds them to the MVC converter's mapper inside `@WebMvcTest` |
| `max-document-length` rejects at length 16,385 and accepts 16,384, counting bytes from an `InputStream`, including whitespace after the root value | R-1 body | Body boundary scenarios | Probed (same setup, `{"operation":"ADD","a":1,"b":1}` padded with spaces inside the object, or after it): 16,384 accepted; 16,385, 16,390, 20,000 rejected, "Document length (…) exceeds the maximum allowed (16384)"; whitespace after the root value counted | **Confirmed** through MVC: 16,384 → `200`; 16,385 → size `detail`, whether the spaces are inside the object or after the root value |
| `max-string-length` rejects a JSON string of 1,001 chars, accepts 1,000 (applies to `operation` too) | R-1 value | Text boundary and operation-name scenarios | Probed (same setup): `"1"` + 999 zeros as a string accepted; 1,001-char string rejected, "String value length (1001)"; `operation` of 1,001 `X` rejected the same way | **Confirmed** through MVC: 1,000-char string → BR-1; 1,001-char Operand string and 1,001-`X` `operation` → size `detail`. Note: even without the properties, a 1,001-digit Operand *string* was already rejected (Jackson's default number-length check when it turns text into a `BigDecimal`) |
| `max-number-length` rejects a 1,001-digit integer, accepts 1,000; a `-` sign is counted | R-1 value | Number boundary scenarios | Probed (constraints of Decision 2, `InputStream` into a `CalculationRequest`-shaped record): `1` + 999 zeros (1,000 chars) accepted; `1` + 1,000 zeros (1,001) rejected, "Number value length (1001)"; `-1` + 999 zeros rejected (by the string-length check). **But** decimal numbers depend on the parse path (Risks): `1.` + 999 zeros (1,001) rejected, "String value length (1001)"; `0.` + 998 zeros + `1` (1,001) accepted → `200`; `-1.` + 999 zeros (1,002) and `-1` + 998 zeros + `e-1` (1,003) get through to BR-1 → `operand-out-of-range` | **Differs for one decimal form** (integers confirmed: 1,000 digits → BR-1, 1,001 digits and `-1` + 999 zeros → size `detail`). Through MVC, `1.` + 999 zeros (1,001 chars) is **not** rejected for size; it reaches BR-1 → `operand-out-of-range`. `0.` + 998 zeros + `1` → `200`, `-1.` + 999 zeros and `-1` + 998 zeros + `e-1` → `operand-out-of-range`, as probed. Covered by the accepted L-7 risk (Risks); no spec scenario uses this value, so no code change |
| Jackson keeps the written form: `1E+100` → precision 1, `1e999999999` → scale -999999999, parsed instantly | BR-1 "as written", R-4 | BR-1 scenarios | Probed: `new BigDecimal("1e999999999")` → precision 1, scale -999999999, 0.07 ms; `1E+100` → precision 1; via the same Jackson setup, `1e999999999` as number and as string parsed with scale -999999999 | **Confirmed:** `1E+100`, `1e999999999` (number and text) reach BR-1 as written; the timed test passes well inside 1 s |
| `1e2147483648` (text or number) fails as a `JacksonException`, not a `NumberFormatException` escaping | Decision 5 | Unrepresentable scenarios | Probed (same setup): `{"operation":"ADD","a":"1e2147483648","b":1}` → `InvalidFormatException`; `{"operation":"ADD","a":1e2147483648,"b":1}` → `DatabindException` "value exceeds limits"; `1e-2147483649` (number) → `DatabindException` | **Confirmed:** text → `HttpMessageNotReadableException` ← `InvalidFormatException`; number → `HttpMessageNotReadableException` ← `DatabindException` ← `NumberFormatException` (the `NumberFormatException` is wrapped, never escapes); both give the generic `invalid-request` `detail` |
| Spring Framework 7 `AbstractJacksonHttpMessageConverter` wraps any `JacksonException` (other than `InvalidDefinitionException`) in `HttpMessageNotReadableException` with it as cause | Decisions 2, 3, 5 | Size and unrepresentable scenarios | javap on `spring-web-7.0.10.jar`: exception table catches `JacksonException` → `HttpMessageNotReadableException`. Cause chain: **confirm in apply** | **Confirmed.** Cause chain is exactly `HttpMessageNotReadableException` ← `StreamConstraintsException` for body, string, `operation` and integer-number limits, so `getMostSpecificCause() instanceof StreamConstraintsException` selects the size `detail` |
| `BigDecimal.precision()` counts significant digits of the unscaled value (`1.50` → 3, `0.00` → 1); `scale()` is the written scale (`1E+1000` → -1000, `1E-1000` → 1000) | BR-1 | BR-1 scenarios | jshell JDK 25.0.4: yes | **Confirmed** (domain tests) |
| Within BR-1, the worst `ADD`/`MULTIPLY`/`DIVIDE` + `stripTrailingZeros().toPlainString()` is about 2,200 characters and ~1 ms | Closing the `500` risk | "Scale of -1000 is accepted" | Probed: `9`×100 `E+900` squared, `9`×100 `E-1100` + that, and their quotient (DECIMAL128), each `stripTrailingZeros().toPlainString()`: about 1 ms in total, longest 2,101 chars; `1E+1000 + 1E-1000` → 2,002 chars | **Confirmed** (the `1E+1000` / `1E-1000` scenarios return their full plain results) |

## Risks / Trade-offs

- **Closes the accepted B-2/B-3 risk ("huge Operand gives a `500` that is not Problem Details"), for every known path.** Values reaching arithmetic now have ≤ 100 digits and |scale| ≤ 1000, so no `ArithmeticException` from scale overflow and no huge `toPlainString()`; unrepresentable exponents and oversized bodies become `400` Problem Details via Jackson. Residual, not part of that risk: there is still no catch-all handler, so an unforeseen bug would reach Boot's `/error`. `POWER` brings its own Result bound (BR-6, B-7).
- **[Accepted risk (ledger L-7), accepted at Gate 1, 2026-11-09] Some decimal JSON numbers over 1,000 characters pass the size check.** For a JSON *number* with a fraction or exponent, Jackson 3.1.7's check depends on the parse path: a digit count, plus a string-length check on some forms. Probed with the Decision 2 constraints, reading from an `InputStream` into a `CalculationRequest`-shaped record: `1.` + 999 zeros (1,001 chars) is rejected ("String value length (1001)"); **`0.` + 998 zeros + `1` (1,001 chars) gets through, has precision 1 and scale 999, passes BR-1 and returns `200`**; `-1.` + 999 zeros (1,002) and `-1` + 998 zeros + `e-1` (1,003) get through and return `operand-out-of-range`. In apply, through Spring MVC, `1.` + 999 zeros (1,001) also gets through the size check (BR-1 → `operand-out-of-range`), unlike the standalone probe. This breaks requirement **"Oversized request is rejected"** (UC-004 R-1) for those values. JSON strings, integers and the body limit are exact. No safety impact: the value is still at most ~1,003 characters and BR-1 bounds the arithmetic. Making it exact needs a custom deserializer. **Gate 1 (2026-11-09) accepted it.** The spec's boundary scenarios use integers and strings only.
- **[A-1] Body limit counts UTF-8 bytes, not characters.** Same result for ASCII. A body with non-ASCII text hits the limit sooner. Non-ASCII can appear in unknown extra fields (ignored) and in `operation` (for example `"ÄDD"`, which gives `unknown-operation`). PRD wording is updated to say so (task 1.1). Accepted at Gate 1, 2026-11-09.
- **[B-5]** `/problems/operand-out-of-range` and the size `detail` need OpenAPI examples in B-5 (STANDARDS.md §4).
- **[Test speed]** The 16,385-character and 1,000-character bodies are built in the test with `String.repeat`; trivial.

## Assumptions

- **A-1 (accepted at Gate 1, 2026-11-09):** R-1's "characters" for the whole body is counted as bytes of the UTF-8 body (Jackson counts bytes from the stream); the spec requirement and the size `detail` say "16,384 bytes". Per-value limits count decoded characters. Non-ASCII is possible in unknown extra fields and in `operation`.
- **A-2 (departure from UC-004 4a, accepted at Gate 1, 2026-11-09):** an Operand whose exponent cannot be represented (`1e2147483648`) is `invalid-request`, although 4a / BR-1 would call it `operand-out-of-range`. Same status (`400`); only `type` differs. Following 4a would need a custom deserializer. PRD §3.2 gets a one-line note (task 1.1).

## Departures after Gate 1 (ledger L-1)

- 2026-11-09, apply: the R-2 scenario "The reason names the Operand but not its value" and tasks 2.1/3.1 used `98765E+1000` (scale -1000, within BR-1, so not rejected); the architect corrected it to `98765E+1001` after the implementer reported it as a blocker.

## Migration Plan

None. The PRD already lists these limits and the `operand-out-of-range` row (PRD §3.4: non-breaking). Roll back by reverting the change.
