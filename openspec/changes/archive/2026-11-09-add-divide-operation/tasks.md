# Tasks

These tasks follow the profile's Task order. Some steps have no work:
- Step 1 (docs): proposal.md - Docs to update says none.
- Steps 4 and 5 (application and config): design.md Decision 4. `PerformCalculation` and `UseCaseConfig` do not change.
- Step 8 (OpenAPI docs): B-5.
- Step 9 (smoke test): design.md Decision 7. The existing smoke test must still pass.

Test names describe behaviour (STANDARDS.md §6). A test that enforces a rule cites its `BR-n` / `R-n` in a comment. Write every `BigDecimal` test value with the String constructor, `new BigDecimal("<text>")`. Never build one from a double (ledger L-9).

**Departures (ledger L-1):** in these cases, update **both** design.md and tasks.md in the same apply step, before QA, and say so in your report:
- you add a dependency, set a config property or create a class that design.md does not name;
- a default in design.md "Framework and JDK defaults relied on" turns out different;
- you otherwise depart from either file.

**Red evidence (ledger L-6):** each failing-test task asks for the red output of `./gradlew test --tests …`. Paste that output into your report before you start the implementing task.

## 1. Docs

- [x] 1.1 No doc edit. PRD.md, DOMAIN-MODEL.md and STANDARDS.md already describe `DIVIDE`, BR-2, BR-3, BR-4, the `division-by-zero` row and the order of failures (proposal.md - Docs to update). To verify, read PRD §3.1, §3.2 and DOMAIN-MODEL §2–§4 and confirm that no statement contradicts the spec delta. If one does, stop and report it. Do not edit the doc on your own.

## 2. Domain (failing tests first)

- [x] 2.1 Write the failing domain tests (plain JUnit + AssertJ). They fail to compile until 2.2 because `Operation.DIVIDE` and `DivisionByZeroException` do not exist yet.
  - In `src/test/java/com/paulrule/mathapi/domain/OperationTest.java`:
    - `findsEachSupportedOperationByItsExactName`: also asserts that `named("DIVIDE")` returns `Operation.DIVIDE`.
    - `rejectsAnUnsupportedName`: replace `named("DIVIDE")` with `named("POWER")` (design.md Decision 6).
  - In `src/test/java/com/paulrule/mathapi/domain/CalculationTest.java`, add the tests below. Use the existing `result(op, a, b)` helper and assert `isEqualTo(new BigDecimal("<expected>"))`. That check is scale-sensitive, so it also proves BR-4:
    - BR-3: `dividesOneByThreeToThirtyFourSignificantDigits`: `"1"`, `"3"` → `"0.3333333333333333333333333333333333"`.
    - BR-3: `roundsTheLastDigitOfTwoThirdsUp`: `"2"`, `"3"` → `"0.6666666666666666666666666666666667"`.
    - BR-3: `roundsATieDownToTheEvenDigit`: `"12345678901234567890123456789012345"`, `"10"` → `"1234567890123456789012345678901234"`.
    - BR-3: `roundsATieUpToTheEvenDigit`: `"12345678901234567890123456789012335"`, `"10"` → `"1234567890123456789012345678901234"`.
    - BR-3: `dividesExactlyWithoutPadding`: `"1"`, `"4"` → `"0.25"`.
    - `dividesByANegativeDivisor`: `"1"`, `"-4"` → `"-0.25"`.
    - `dividesZeroToZero`: `"0.00"`, `"-3"` → `BigDecimal.ZERO`.
    - BR-4: `normalisesAWholeQuotient`: `"1.50"`, `"0.5"` → `"3"`.
    - BR-4: `normalisesALargeQuotient`: `"100"`, `"0.01"` → `"1E+4"`.
    - BR-2: `rejectsDivisionByZeroInEveryZeroForm`: for each `b` in `"0"`, `"0.00"`, `"-0"`, `new Calculation(Operation.DIVIDE, new BigDecimal("1"), new BigDecimal(b)).result()` throws `DivisionByZeroException`. Use a `@ParameterizedTest` with `@ValueSource`, or three asserts.
    - BR-2: `rejectsZeroDividedByZero`: `"0"`, `"0"` throws `DivisionByZeroException`, not `ArithmeticException`. Assert `isExactlyInstanceOf(DivisionByZeroException.class)`.
    - R-2: `divisionByZeroReasonEchoesNoOperand`: the exception message for `"98765"` ÷ `"0.00"` contains neither `98765` nor `0.00`.
  Verify: run `./gradlew test --tests '*domain*'` and paste the red output (a compilation failure on `DIVIDE` / `DivisionByZeroException`).
- [x] 2.2 Implement the `DIVIDE` constant in `domain.Operation` (design.md Decision 1, after `MULTIPLY`) and `domain.DivisionByZeroException` (Decision 2). Correct the `Calculation.result()` Javadoc (Decision 5). Do not import any framework. Verify: `./gradlew test --tests '*domain*' --tests '*ArchitectureTest' --tests '*PerformCalculationTest'` passes.

## 3. Web adapter and Problem Details (failing tests first)

- [x] 3.1 Edit `src/test/java/com/paulrule/mathapi/adapter/in/web/CalculationControllerTest.java`. Add a constant `DIVISION_BY_ZERO = "/problems/division-by-zero"`. Add a helper `expectDivisionByZero(String body)` that asserts status `422`, content type `application/problem+json` and `$.type` `/problems/division-by-zero`. Do not change `expectProblem`: it must keep asserting `400`. Every test sends `POST /api/v1/calculations` with `Content-Type: application/json` and the exact body below. Write one test per spec scenario:
  - Existing tests to change (modified req "Unknown operation is rejected"):
    - `rejectsASupportedNameInTheWrongLetterCase`: `{"operation":"add","a":1,"b":2}`. `detail` must contain `ADD, SUBTRACT, MULTIPLY, DIVIDE`.
    - `rejectsAnOperationNotYetSupported`: `{"operation":"POWER","a":1,"b":2}`.
  - "DIVIDE is correctly rounded" (BR-3, BR-4):
    - `dividesOneByThreeToThirtyFourSignificantDigits`: `{"operation":"DIVIDE","a":1,"b":"3"}` → body exactly `{"operation":"DIVIDE","a":"1","b":"3","result":"0.3333333333333333333333333333333333"}`.
    - `roundsTheLastDigitOfTwoThirdsUp`: `{"operation":"DIVIDE","a":"2","b":"3"}` → `"0.6666666666666666666666666666666667"`.
    - `roundsATieDownToTheEvenDigit`: `{"operation":"DIVIDE","a":"12345678901234567890123456789012345","b":"10"}` → `"1234567890123456789012345678901234"`.
    - `roundsATieUpToTheEvenDigit`: `{"operation":"DIVIDE","a":"12345678901234567890123456789012335","b":"10"}` → `"1234567890123456789012345678901234"`.
    - `dividesExactlyWithoutPadding`: `{"operation":"DIVIDE","a":"1","b":"4"}` → body exactly `{"operation":"DIVIDE","a":"1","b":"4","result":"0.25"}`.
    - `dividesByANegativeDivisor`: `{"operation":"DIVIDE","a":"1","b":"-4"}` → `"-0.25"`.
    - `dividesZeroToZero`: `{"operation":"DIVIDE","a":"0.00","b":"-3"}` → body exactly `{"operation":"DIVIDE","a":"0.00","b":"-3","result":"0"}`.
    - `normalisesAWholeQuotient`: `{"operation":"DIVIDE","a":"1.50","b":"0.5"}` → body exactly `{"operation":"DIVIDE","a":"1.50","b":"0.5","result":"3"}`.
    - `presentsALargeQuotientInPlainNotation`: `{"operation":"DIVIDE","a":"100","b":"0.01"}` → `"10000"`.
  - "Division by zero is rejected" (BR-2; each test uses `expectDivisionByZero`):
    - `rejectsAZeroDivisorAsText`: `{"operation":"DIVIDE","a":"1","b":"0"}`. Also assert that `$.status` is `422` and that `$.title` and `$.detail` exist.
    - `rejectsAZeroDivisorAsANumber`: `{"operation":"DIVIDE","a":1,"b":0}`.
    - `rejectsAScaledZeroDivisorAsText`: `{"operation":"DIVIDE","a":"1","b":"0.00"}`.
    - `rejectsAScaledZeroDivisorAsANumber`: `{"operation":"DIVIDE","a":1,"b":0.00}`.
    - `rejectsANegativeZeroDivisorAsText`: `{"operation":"DIVIDE","a":"1","b":"-0"}`.
    - `rejectsANegativeZeroDivisorAsANumber`: `{"operation":"DIVIDE","a":1,"b":-0}`.
    - `rejectsZeroDividedByZero`: `{"operation":"DIVIDE","a":"0","b":"0"}`.
    - R-2/R-7: `divisionByZeroReasonEchoesNoOperandAndNoInternals`: `{"operation":"DIVIDE","a":"98765","b":"0.00"}`. `detail` must contain none of `98765`, `0.00`, `Exception`, `java.`, `org.springframework`, `com.paulrule`.
  - "Only the first failure is reported" (UC-003 R-1):
    - `reportsANonNumericDividendBeforeDivisionByZero`: `{"operation":"DIVIDE","a":"abc","b":0}` → `400` `/problems/invalid-request`.
    - `reportsAMissingDividendBeforeDivisionByZero`: `{"operation":"DIVIDE","b":0}` → `400` `/problems/invalid-request`.
    - `reportsAnUnknownOperationBeforeDivisionByZero`: `{"operation":"divide","a":1,"b":0}` → `400` `/problems/unknown-operation`.
  Keep `@Import(PerformCalculation.class)`. Never import `config` (ledger L-5).
  Verify: run `./gradlew test --tests '*CalculationControllerTest'` and paste the red output. Expect the division-by-zero tests to fail because no handler maps `DivisionByZeroException`. In `@WebMvcTest`, an unhandled exception usually surfaces as a `ServletException` (wrapping `DivisionByZeroException`) thrown from `mvc.perform(...)`, not as a `500` status. Either form counts as the expected red. If they fail any other way, the test is wrong.
- [x] 3.2 In `ApiExceptionHandler`, add the `DivisionByZeroException` handler with `HttpStatus.UNPROCESSABLE_CONTENT` (design.md Decision 3). Verify: run `./gradlew test --tests '*CalculationControllerTest' --tests '*ArchitectureTest'` and confirm it passes. Then confirm the open rows of design.md "Framework and JDK defaults relied on" (Jackson `0.00` / `-0` numbers, the 422 status) and mark them confirmed or differing. If one differs, follow L-1.

## 4. Verify

- [x] 4.1 Run `./gradlew clean build` and paste the output. Compile, all tests (domain, application, web slice, the unchanged smoke test `MathApiApplicationTest`, ArchUnit) and the Spotless check must pass. If Spotless fails, run `./gradlew spotlessApply` and re-run. Never weaken a test, an ArchUnit rule or Spotless.

## Workflow follow-up

- Gate 1 accepted (2026-11-09) the open risk: until B-4, a huge Operand can still give a `500` that is not Problem Details, which breaks "Every rejection is Problem Details without internals" (design.md Risks).
- After Gate 2, the pipeline ticks B-3 in `BACKLOG.md`, archives the change and writes the retrospective.
- After archive: in `openspec/specs/calculation/spec.md` § Purpose, replace "get its exact Result" with wording that also covers DIVIDE's rounded Result. For example: "get its Result (exact, or rounded to 34 significant digits for `DIVIDE`, BR-3)". A delta cannot change Purpose, which is why this is a separate step. Verify: `openspec validate calculation --type spec --strict` passes.
