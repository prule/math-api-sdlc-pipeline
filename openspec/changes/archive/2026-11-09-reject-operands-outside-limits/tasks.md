# Tasks

These tasks follow the profile's Task order. Some steps have no work:
- Steps 4 and 5 (application and config wiring): design.md Decision 7. `PerformCalculation` and `UseCaseConfig` do not change.
- Step 8 (OpenAPI docs): B-5.
- Step 9 (smoke test): design.md Decision 7. The existing smoke test must still pass.

Test names describe behaviour (STANDARDS.md §6). A test that enforces a rule cites its `BR-n` / `R-n` in a comment. Write every `BigDecimal` test value with the String constructor, `new BigDecimal("<text>")`; never from a double (ledger L-9). Build long values with `String.repeat`.

**Departures (ledger L-1):** in these cases, update **both** design.md and tasks.md in the same apply step, before QA, and say so in your report:
- you add a dependency, set a config property or create a class that design.md does not name;
- a default in design.md "Framework and JDK defaults relied on" turns out different;
- you otherwise depart from either file.

**Post-Gate-1 departure (2026-11-09):** tasks 2.1/3.1 and the spec scenario "The reason names the Operand but not its value" changed `98765E+1000` (within BR-1) to `98765E+1001`; architect fix after an implementer blocker (design.md "Departures after Gate 1").

**Red evidence (ledger L-6):** each failing-test task asks for the red output of `./gradlew test --tests …`. Paste it into your report before you start the implementing task.

## 1. Docs

- [x] 1.1 Update the docs (proposal.md - Docs to update):
  - `docs/PRD.md` §3.2: in the "Only the first failure is reported" bullet, change `operand out of range (BR-1)` to `operand out of range (BR-1, \`a\` before \`b\`)`. In the "Request limits" bullet, after "a body over 16,384 characters", add "(counted as bytes of the UTF-8 body)", and append the sentence "A number that cannot be represented as a decimal at all (for example `1e2147483648`) is `invalid-request`." Add a §8 change-log row dated today: `§3.2: Operand \`a\` is checked against BR-1 before \`b\`; the body limit counts UTF-8 bytes; an unrepresentable number is invalid-request (UC-004)`.
  - `docs/DOMAIN-MODEL.md` §2: change "then BR-1 on each Operand present." to "then BR-1 on `a`, then on `b` (only the first failure is reported)." Add a §5 change-log row: `§2: BR-1 checks \`a\` before \`b\` (UC-004)`. Do not edit or renumber BR-1.
  - `docs/STANDARDS.md`: no edit.
  Verify: re-read PRD §3.2 and DOMAIN-MODEL §2/§4 and confirm no statement contradicts `specs/calculation/spec.md`. If one does, stop and report it.

## 2. Domain (failing tests first)

- [x] 2.1 Write the failing domain tests in `src/test/java/com/paulrule/mathapi/domain/CalculationTest.java` (plain JUnit + AssertJ). They fail to compile until 2.2 because `OperandOutOfRangeException` does not exist. Build each Calculation with `new Calculation(op, new BigDecimal("<text>"), new BigDecimal("<text>"))`:
  - BR-1: `acceptsAScaleOfMinusOneThousand`: `ADD`, `"1E+1000"`, `"0"` constructs without exception.
  - BR-1: `acceptsAScaleOfOneThousand`: `ADD`, `"1E-1000"`, `"0"` constructs.
  - BR-1: `acceptsOneHundredSignificantDigits`: `MULTIPLY`, `"1".repeat(100)`, `"1"` constructs.
  - BR-1: `measuresAnOperandAsWritten`: `MULTIPLY`, `"1E+100"`, `"1"` constructs.
  - BR-1: `rejectsAScaleOfMinusOneThousandAndOne`: `ADD`, `"1E+1001"`, `"1"` throws `OperandOutOfRangeException` whose message contains `Operand a`.
  - BR-1: `rejectsAScaleOfOneThousandAndOneInB`: `ADD`, `"1"`, `"1E-1001"` throws, message contains `Operand b`.
  - BR-1: `rejectsOneHundredAndOneSignificantDigitsEvenForASmallValue`: `ADD`, `"1" + "0".repeat(100)`, `"1"` throws.
  - BR-1/R-4: `rejectsAShortButEnormousOperandAtOnce`: wrap `new Calculation(MULTIPLY, "1e999999999", "1e999999999")` in `assertTimeoutPreemptively(Duration.ofSeconds(1), …)` and assert it throws `OperandOutOfRangeException`.
  - BR-1: `rejectsEnormousScalesInADivisionBeforeAnyArithmetic`: `DIVIDE`, `"1E-999999999"`, `"1E+999999999"` throws `OperandOutOfRangeException` from the constructor.
  - R-3: `checksOperandABeforeOperandB`: `ADD`, `"1E+1001"`, `"1E-1001"` throws with message containing `Operand a` and not `Operand b`.
  - R-3: `reportsOperandOutOfRangeBeforeDivisionByZero`: `DIVIDE`, `"1E+1001"`, `"0"` throws `OperandOutOfRangeException` from the constructor (never reaches `result()`).
  - R-2: `operandOutOfRangeReasonEchoesNoValue`: `ADD`, `"1"`, `"98765E+1001"`: message contains `Operand b` and neither `98765` nor `98765E+1001`.
  Verify: run `./gradlew test --tests '*domain*'` and paste the red output (a compilation failure on `OperandOutOfRangeException`).
- [x] 2.2 Add `domain.OperandOutOfRangeException` (design.md Decision 4) and the BR-1 check in the `Calculation` constructor, `a` then `b` (Decision 1). Fix the `UnknownOperationException` Javadoc (Decision 6). No framework import. Verify: `./gradlew test --tests '*domain*' --tests '*ArchitectureTest' --tests '*PerformCalculationTest'` passes, and every existing domain test still passes unchanged.

## 3. Web adapter, Problem Details and request limits (failing tests first)

- [x] 3.1 Edit `src/test/java/com/paulrule/mathapi/adapter/in/web/CalculationControllerTest.java`. Add a constant `OPERAND_OUT_OF_RANGE = "/problems/operand-out-of-range"` and a helper `padTo(String body, int length)` that inserts spaces before the final `}` until `body.length() == length`. Every test sends `POST /api/v1/calculations` with `Content-Type: application/json` and the exact body below (copied from the spec scenarios). Use `expectProblem(body, type)` (it asserts `400`, `application/problem+json` and `$.type`). One test per scenario:
  - "Operand outside the limits is rejected" (BR-1, R-2, R-4):
    - `acceptsAScaleOfMinusOneThousand`: `{"operation":"ADD","a":"1E+1000","b":"0"}` → `200`, `$.result` is `"1" + "0".repeat(1000)`.
    - `acceptsAScaleOfOneThousand`: `{"operation":"ADD","a":"1E-1000","b":"0"}` → `200`, `$.result` is `"0." + "0".repeat(999) + "1"`.
    - `acceptsOneHundredSignificantDigits`: `{"operation":"MULTIPLY","a":"<"1".repeat(100)>","b":"1"}` → `200`, `$.result` is `"1".repeat(100)`.
    - `measuresAnOperandAsWritten`: `{"operation":"MULTIPLY","a":"1E+100","b":"1"}` → `200`, `$.result` is `"1" + "0".repeat(100)`.
    - `rejectsAScaleOfMinusOneThousandAndOne`: `{"operation":"ADD","a":"1E+1001","b":"1"}` → `expectProblem(…, OPERAND_OUT_OF_RANGE)`; also `$.status` is `400`, `$.title` exists, `$.detail` contains `Operand a`.
    - `rejectsAScaleOfOneThousandAndOneInB`: `{"operation":"ADD","a":"1","b":"1E-1001"}` → operand-out-of-range, `$.detail` contains `Operand b`.
    - `rejectsOneHundredAndOneSignificantDigits`: `{"operation":"ADD","a":"<"1" + "0".repeat(100)>","b":"1"}` → operand-out-of-range.
    - `rejectsOneHundredAndOneSignificantDigitsAsANumber`: `{"operation":"ADD","a":<1 followed by 100 zeros, unquoted>,"b":1}` → operand-out-of-range.
    - `rejectsAShortButEnormousOperandAtOnce`: `{"operation":"MULTIPLY","a":1e999999999,"b":"1e999999999"}` → operand-out-of-range, inside `assertTimeoutPreemptively(Duration.ofSeconds(1), …)`.
    - `rejectsEnormousScalesInADivision`: `{"operation":"DIVIDE","a":"1E-999999999","b":"1E+999999999"}` → operand-out-of-range.
    - R-2/R-7: `operandOutOfRangeReasonNamesTheOperandButNotItsValue`: `{"operation":"ADD","a":"1","b":"98765E+1001"}` → operand-out-of-range; `$.detail` contains `Operand b` and none of `98765`, `98765E+1001`, `Exception`, `java.`, `tools.jackson`, `org.springframework`, `com.paulrule`.
  - "Oversized request is rejected" (R-1):
    - `rejectsABodyOneCharacterOverTheLimit`: `padTo("{\"operation\":\"ADD\",\"a\":1,\"b\":2}", 16385)` → invalid-request; `$.detail` contains `16,384` and `1,000`.
    - `acceptsABodyExactlyAtTheLimit`: same, padded to 16384 → `200`, `$.result` is `"3"`.
    - `rejectsOperandTextOneCharacterOverTheLimit`: `{"operation":"ADD","a":"<"1" + "0".repeat(1000)>","b":1}` → invalid-request.
    - `rejectsAnOperandNumberOneCharacterOverTheLimit`: `{"operation":"ADD","a":<1 followed by 1000 zeros, unquoted>,"b":1}` → invalid-request.
    - `doesNotRejectOperandTextExactlyAtTheLimitForSize`: `{"operation":"ADD","a":"<"1" + "0".repeat(999)>","b":1}` → operand-out-of-range.
    - `doesNotRejectAnOperandNumberExactlyAtTheLimitForSize`: `{"operation":"ADD","a":<1 followed by 999 zeros, unquoted>,"b":1}` → operand-out-of-range.
    - `rejectsAnOperationNameOverTheLimit`: `{"operation":"<"X".repeat(1001)>","a":1,"b":2}` → invalid-request.
    - `doesNotRejectAnOperationNameExactlyAtTheLimitForSize`: `{"operation":"<"X".repeat(1000)>","a":1,"b":2}` → unknown-operation.
  - "Operand that cannot be represented is rejected" (design.md Decision 5):
    - `rejectsAnUnrepresentableExponentAsText`: `{"operation":"ADD","a":"1e2147483648","b":1}` → invalid-request.
    - `rejectsAnUnrepresentableExponentAsANumber`: `{"operation":"ADD","a":1e2147483648,"b":1}` → invalid-request.
  - "Only the first failure is reported" (R-3; the six existing order tests stay):
    - `reportsAnOversizedBodyBeforeAnUnknownOperation`: `padTo("{\"operation\":\"add\",\"a\":1,\"b\":2}", 16385)` → invalid-request.
    - `reportsAMissingOperandBeforeAnOutOfRangeOperand`: `{"operation":"ADD","a":"1E+1001"}` → invalid-request.
    - `reportsAnUnknownOperationBeforeAnOutOfRangeOperand`: `{"operation":"add","a":"1E+1001","b":1}` → unknown-operation.
    - `reportsOnlyOperandAWhenBothAreOutOfRange`: `{"operation":"ADD","a":"1E+1001","b":"1E-1001"}` → operand-out-of-range; `$.detail` contains `Operand a` and not `Operand b`.
    - `reportsAnOutOfRangeDividendBeforeDivisionByZero`: `{"operation":"DIVIDE","a":"1E+1001","b":"0"}` → operand-out-of-range (status `400`, not `422`).
  Keep `@Import(PerformCalculation.class)`. Never import `config` (ledger L-5).
  Verify: run `./gradlew test --tests '*CalculationControllerTest'` and paste the red output. Expected red: the operand-out-of-range tests fail because no handler maps `OperandOutOfRangeException` (in `@WebMvcTest` this usually surfaces as a `ServletException` thrown from `mvc.perform(...)`); the oversized-string, operation-name and over-limit body tests fail because no constraint is set yet (they get `200` or `unknown-operation`); the size-`detail` assertion fails. The at-limit, number-over-limit (Jackson's default 1,000) and unrepresentable tests may already pass. Any other failure means the test is wrong.
- [x] 3.2 Implement: the `OperandOutOfRangeException` handler and the size-specific `detail` in `ApiExceptionHandler` (design.md Decisions 3, 4), and the three `spring.jackson.factory.constraints.read.*` properties in `src/main/resources/application.yaml` with their comment (Decision 2). Verify: `./gradlew test --tests '*CalculationControllerTest' --tests '*ArchitectureTest'` passes. Then mark each row of design.md "Framework and JDK defaults relied on" confirmed or differing, especially the two "confirm in apply" rows (properties bound to the MVC mapper; cause chain of `HttpMessageNotReadableException`). If one differs, follow L-1.
  - Apply result (2026-11-09): both "confirm in apply" rows confirmed. One row differs: through Spring MVC, the decimal JSON number `1.` + 999 zeros (1,001 chars) is not rejected for size and reaches BR-1 (`operand-out-of-range`). This is within the Gate 1 accepted risk (decimal numbers of 1,001–1,003 characters); no spec scenario uses it; no code change. Recorded in design.md (defaults table, Risks).

## 4. Verify

- [x] 4.1 Run `./gradlew clean build` and paste the output. Compile, all tests (domain, application, web slice, the unchanged smoke test `MathApiApplicationTest`, ArchUnit) and the Spotless check must pass. If Spotless fails, run `./gradlew spotlessApply` and re-run. Never weaken a test, an ArchUnit rule or Spotless.

## Workflow follow-up

- Gate 1 accepted (2026-11-09): (a) the risk that some decimal JSON numbers of 1,001–1,003 characters pass the size check (a 1,001-character `0.000…1` returns `200`), which breaks the letter of "Oversized request is rejected" (UC-004 R-1; design.md Risks, ledger L-7); (b) A-1: the 16,384 body limit counts UTF-8 bytes; (c) A-2, a departure from UC-004 4a: an unrepresentable exponent such as `1e2147483648` is `invalid-request`, not `operand-out-of-range`.
- This change closes the B-2/B-3 accepted risk "huge Operand gives a `500` that is not Problem Details" (design.md Risks).
- After Gate 2, the pipeline ticks B-4 in `BACKLOG.md`, archives the change and writes the retrospective. The `calculation` Purpose line stays true; no Purpose edit is needed (ledger L-10).
- B-5 must document `/problems/operand-out-of-range` and the size `detail`.
