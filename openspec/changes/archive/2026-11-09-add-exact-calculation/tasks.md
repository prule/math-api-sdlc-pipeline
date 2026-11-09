# Tasks

Follows the profile's Task order. Step 8 (OpenAPI docs, `ApiDocumentationTest`) has no work: B-5. Test names describe behaviour (STANDARDS.md §6); tests enforcing a rule cite its `BR-n` / `R-n` in a comment.

**Departures (ledger L-1):** if you add a dependency, pin a version, set a config property or create a class that design.md does not name, or a framework default in design.md "Framework defaults relied on" turns out different, update design.md and tasks.md in the same apply step, before QA.

## 1. Docs

- [x] 1.1 `docs/PRD.md` §3.2: after the "Every error response ... is Problem Details" bullet, add: "Errors outside the table below (for example `404`, `405`, `415`) use `type` `about:blank` with the HTTP status phrase as `title` (RFC 9457 §4.2.1)." Add a §8 change-log row dated today citing UC-002. DOMAIN-MODEL.md and STANDARDS.md: no change (proposal.md - Docs to update). Verify by reading the diff: only PRD §3.2 and §8 change.

## 2. Domain (failing tests first)

- [x] 2.1 Write `src/test/java/com/paulrule/mathapi/domain/OperationTest.java` (plain JUnit + AssertJ), failing until 2.3:
  - `findsEachSupportedOperationByItsExactName`: `named("ADD")`, `named("SUBTRACT")`, `named("MULTIPLY")` return the constants.
  - `rejectsASupportedNameInTheWrongLetterCase` (R-3): `named("add")` throws `UnknownOperationException`.
  - `rejectsAnUnsupportedName`: `named("DIVIDE")`, `named("")` and `named(null)` throw `UnknownOperationException`.
  - `unknownOperationReasonListsSupportedOperationsInOrder` (R-3): the message contains `String.join(", ", names of Operation.values())`, built from `values()` in the test, not a literal.
- [x] 2.2 Write `src/test/java/com/paulrule/mathapi/domain/CalculationTest.java`, failing until 2.3. Each test builds `new Calculation(op, new BigDecimal("<a>"), new BigDecimal("<b>"))` and asserts `result()` with `isEqualTo(new BigDecimal("<expected>"))` (scale-sensitive). Every value below is written as `new BigDecimal("...")` with the String constructor, never from a double (`new BigDecimal(0.1)`, `BigDecimal.valueOf(0.1)`) or a shorthand:
  - BR-3 ADD: `addsDecimalFractionsExactly` (`0.1`, `0.2` → `0.3`); `addsNegativeOperands` (`-1.5`, `-2.25` → `-3.75`); `addsWithLargePrecision` (`12345678901234567890.123456789012345678901234567890`, `0.000000000000000000000000000001` → `12345678901234567890.123456789012345678901234567891`).
  - BR-3 SUBTRACT: `subtractsToZero` (`1.50`, `1.5` → `BigDecimal.ZERO`); `subtractsToANegativeResult` (`2`, `5.5` → `-3.5`).
  - BR-3 MULTIPLY: `multipliesByZero` (`123.45`, `0` → `BigDecimal.ZERO`); `multipliesNegativeByPositive` (`-2`, `3` → `-6`); `multipliesNegativeByNegative` (`-2`, `-3` → `6`); `multiplyGrowsScale` (`1.25`, `1.25` → `1.5625`).
  - BR-4: `stripsTrailingZerosFromTheResult` (ADD `1.10`, `2.20` → `3.3`, scale 1); `normalisesAWholeResult` (ADD `50`, `50` → `new BigDecimal("1E+2")`).
  - `rejectsMissingParts`: a `null` Operation, `a` or `b` throws `NullPointerException`.
- [x] 2.3 Implement `domain.Operation`, `domain.UnknownOperationException`, `domain.Calculation` per design.md Decision 1 (plain Java, no framework import). Verify `./gradlew test --tests '*domain*'` and `--tests '*ArchitectureTest'` pass.

## 3. Application and config (failing test first)

- [x] 3.1 Write `src/test/java/com/paulrule/mathapi/application/PerformCalculationTest.java`, failing until 3.2: `performsTheNamedOperation` (`perform("ADD", new BigDecimal("0.1"), new BigDecimal("0.2"))` returns a Calculation with Operation `ADD`, `a` equal to `new BigDecimal("0.1")`, `b` equal to `new BigDecimal("0.2")`, `result()` equal to `new BigDecimal("0.3")`); `rejectsAnUnknownOperationName` (`perform("add", new BigDecimal("1"), new BigDecimal("2"))` throws `UnknownOperationException`).
- [x] 3.2 Implement `application.PerformCalculation` and `config.UseCaseConfig` (`@Bean`) per design.md Decision 1. Verify `PerformCalculationTest` and `ArchitectureTest` pass.

## 4. Web adapter and Problem Details (failing tests first)

- [x] 4.1 Add `implementation("org.springframework.boot:spring-boot-starter-validation")` to `build.gradle.kts` (design.md Decision 6; no version, Boot BOM manages it). Verify `./gradlew dependencies --configuration runtimeClasspath` lists `org.hibernate.validator:hibernate-validator`.
- [x] 4.2 Write `src/test/java/com/paulrule/mathapi/adapter/in/web/CalculationControllerTest.java`: `@WebMvcTest(CalculationController.class)` `@Import(PerformCalculation.class)` (design.md Decision 5; changed in apply from `UseCaseConfig`, which breaks the ArchUnit layer rule from adapter-package test code), MockMvc. Every test sends `POST /api/v1/calculations` with `Content-Type: application/json` and the exact body below, unless stated. Success tests assert status `200` and the stated `result` (or exact body); rejection tests assert the status, content type `application/problem+json` and `type`. One test per spec scenario:
  - ADD is exact: `addsDecimalFractionsExactly` `{"operation":"ADD","a":"0.1","b":"0.2"}` → body exactly `{"operation":"ADD","a":"0.1","b":"0.2","result":"0.3"}`; `addsDecimalFractionsGivenAsNumbersExactly` `{"operation":"ADD","a":0.1,"b":0.2}` → `"0.3"`; `addsNegativeOperands` `{"operation":"ADD","a":"-1.5","b":"-2.25"}` → `"-3.75"`; `addsWithLargePrecision` `{"operation":"ADD","a":"12345678901234567890.123456789012345678901234567890","b":"0.000000000000000000000000000001"}` → `"12345678901234567890.123456789012345678901234567891"`.
  - SUBTRACT is exact: `subtractsToZero` `{"operation":"SUBTRACT","a":"1.50","b":"1.5"}` → `"0"`; `subtractsToANegativeResult` `{"operation":"SUBTRACT","a":"2","b":"5.5"}` → `"-3.5"`.
  - MULTIPLY is exact: `multipliesByZero` `{"operation":"MULTIPLY","a":"123.45","b":"0"}` → `"0"`; `multipliesNegativeByPositive` `{"operation":"MULTIPLY","a":"-2","b":"3"}` → `"-6"`; `multipliesNegativeByNegative` `{"operation":"MULTIPLY","a":"-2","b":"-3"}` → `"6"`; `multiplyGrowsScale` `{"operation":"MULTIPLY","a":"1.25","b":"1.25"}` → `"1.5625"`.
  - Result is normalised (BR-4): `stripsTrailingZeros` `{"operation":"ADD","a":"1.10","b":"2.20"}` → `"3.3"`; `presentsALargeResultInPlainNotation` `{"operation":"ADD","a":"50","b":"50"}` → `"100"`; `presentsASmallResultInPlainNotation` `{"operation":"MULTIPLY","a":"0.001","b":"0.001"}` → `"0.000001"`.
  - R-1: `acceptsOperandsAsNumberOrText` `{"operation":"ADD","a":1.5,"b":"2.5"}` → body exactly `{"operation":"ADD","a":"1.5","b":"2.5","result":"4"}`.
  - R-5/R-6: `echoesOperandsWithTheirGivenScaleInPlainNotation` `{"operation":"MULTIPLY","a":1e2,"b":"1.50"}` → body exactly `{"operation":"MULTIPLY","a":"100","b":"1.50","result":"150"}`; `presentsDecimalValuesAsJsonStrings` `{"operation":"ADD","a":1,"b":2}` → `$.a`, `$.b`, `$.result` are JSON strings `"1"`, `"2"`, `"3"` (assert the raw body is exactly `{"operation":"ADD","a":"1","b":"2","result":"3"}`).
  - R-2 invalid request (`400`, `/problems/invalid-request`): `rejectsMalformedJson` body `{"operation":"ADD","a":1,`; `rejectsAnEmptyBody` empty body; `rejectsAMissingOperation` `{"a":1,"b":2}` and `detail` contains `operation`; `rejectsAMissingOperand` `{"operation":"ADD","b":2}` and `detail` contains `'a'`; `rejectsANullOperand` `{"operation":"ADD","a":1,"b":null}` and `detail` contains `'b'`; `rejectsNonNumericText` `{"operation":"ADD","a":"abc","b":2}`; `rejectsABooleanOperand` `{"operation":"ADD","a":true,"b":2}`; `rejectsEmptyText` `{"operation":"ADD","a":"","b":2}`.
  - R-3 unknown operation (`400`, `/problems/unknown-operation`): `rejectsASupportedNameInTheWrongLetterCase` `{"operation":"add","a":1,"b":2}` and `detail` contains `ADD, SUBTRACT, MULTIPLY`; `rejectsAnOperationNotYetSupported` `{"operation":"DIVIDE","a":1,"b":2}`; `doesNotEchoTheGivenOperationName` `{"operation":"CUBE_ROOT","a":1,"b":2}` and `detail` does not contain `CUBE_ROOT`; `rejectsAnEmptyOperationName` `{"operation":"","a":1,"b":2}`.
  - R-4 first failure (`400`, `/problems/invalid-request`): `reportsANonNumericOperandBeforeAnUnknownOperation` `{"operation":"add","a":"abc","b":2}`; `reportsAMissingOperandBeforeAnUnknownOperation` `{"operation":"add","b":2}`; `reportsMalformedJsonBeforeAnUnknownOperation` body `{"operation":"add","a":1,`.
  - R-7: `rejectionDetailExposesNoInternals` `{"operation":"ADD","a":"abc","b":2}` → `title`, `status`, `detail` present and `detail` contains none of `Exception`, `java.`, `tools.jackson`, `com.fasterxml`, `org.springframework`, `com.paulrule`; `rejectsAnUnsupportedMediaType` `POST` with `Content-Type: text/plain`, body `ADD 1 2` → `415`, `application/problem+json`, `type` `about:blank`, `title` `Unsupported Media Type`, `status` `415`, `detail` present; `rejectsAnUnsupportedMethod` `GET /api/v1/calculations` → `405`, `application/problem+json`, `type` `about:blank`, `title` `Method Not Allowed`, `detail` present; `rejectsAnUnknownPath` `POST /api/v1/unknown` → `404`, `application/problem+json`, `type` `about:blank`, `title` `Not Found`, `detail` present (*added at code review*: the PRD §3.2 `about:blank` bullet names `404`).
  Verify the class fails (no controller yet).
- [x] 4.3 Implement `adapter.in.web.CalculationRequest`, `CalculationResponse`, `CalculationController` per design.md Decision 1, and `ApiExceptionHandler` per Decision 4. This includes the `createResponseEntity` override that sets `type` `about:blank`, added in apply because Spring Framework 7 leaves `ProblemDetail.type` null (design.md defaults table). Verify all `CalculationControllerTest` tests and `ArchitectureTest` pass. If a test fails because a Jackson 3 / Boot 4.1.2 default differs from design.md's table, fix the code (not the test) and record it (L-1).
- [x] 4.4 In `src/test/java/com/paulrule/mathapi/ArchitectureTest.java`, remove `withOptionalLayers(true)` from the layered rule and `allowEmptyShould(true)` from all three rules: every layer now has a class (`domain` and `application` after 2.3/3.2, `config` after 3.2, `adapter.in.web` after 4.3). Keep `domainRuleRejectsAFrameworkDependentDomainClass` and its fixture unchanged (design.md Risks). Verify `./gradlew test --tests '*ArchitectureTest'` passes, including the fixture test.

## 5. Smoke test

- [x] 5.1 Update the class Javadoc of `MathApiApplicationTest` to say it covers service availability (UC-001) and the end-to-end Calculation smoke test (UC-002). Add `performsAnAdditionEndToEnd` to the existing `src/test/java/com/paulrule/mathapi/MathApiApplicationTest.java` (design.md Decision 7; do not add a second `@SpringBootTest` class): `POST /api/v1/calculations` `{"operation":"ADD","a":"0.1","b":"0.2"}` → `200`, `$.result` `"0.3"`. Verify the whole class passes, including B-1's health tests.

## 6. Verify

- [x] 6.1 Run `./gradlew clean build` and paste the output: compile, all tests (domain, application, web slice, smoke, ArchUnit) and Spotless check pass. If Spotless fails, run `./gradlew spotlessApply` and re-run.

## Workflow follow-up

- Tick B-2 in `BACKLOG.md`, archive the change and write the retrospective (done by the pipeline after Gate 2).
