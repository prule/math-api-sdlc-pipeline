# Design

## Context

`POST /api/v1/calculations` is served by `adapter.in.web.CalculationController` (request record `CalculationRequest(String operation, BigDecimal a, BigDecimal b)`, each `@NotNull`; response record `CalculationResponse(String operation, String a, String b, String result)`). `ApiExceptionHandler` (the single `@RestControllerAdvice`, extends `ResponseEntityExceptionHandler`) maps failures to Problem Details: `invalid-request` (three `detail` texts: unreadable, missing field, too large), `unknown-operation`, `operand-out-of-range` (400) and `division-by-zero` (422); framework errors get `about:blank`. Supported Operations: `domain.Operation` = `ADD, SUBTRACT, MULTIPLY, DIVIDE`. BR-1 limits are `private static final` constants in `domain.Calculation`. There is no springdoc dependency and no `OpenApiConfig` yet. For the motivation, see proposal.md - Why. For the requirements, see `specs/api-documentation/spec.md`.

**Rule for the implementer (ledger L-1):** if you depart from this design or from tasks.md (a new dependency, a config property or class not named here, a default in "Framework defaults relied on" that turns out different, or any edit to the spec or tasks after Gate 1, by anyone), update **both** design.md and tasks.md in the same apply step, before QA, and say so in your report.

## Goals / Non-Goals

**Goals:**
- Complete, accurate documentation with springdoc's defaults plus the least code: annotations for the response statuses, one `OpenApiCustomizer` for everything generated or corrected.
- Every documented example is proved against the live service by a test (R-5).

**Non-Goals:**
- Any `springdoc.*` property, `application-prod.yaml` or `prod` behaviour (B-6). Any change to Calculation behaviour, `ApiExceptionHandler`, `PerformCalculation` or `UseCaseConfig`.
- Per-operation `404`/`405`/`406`/`415` responses (Decision 6). Tags, operation summaries, a custom `ProblemDetail` record.

## Use case mapping

| UC-005 item | Covered by |
|---|---|
| Main flow 1, R-1 (machine-readable and interactive, no credentials) | Reqs "Documentation is published in machine-readable form", "…as interactive pages" |
| Main flow 2: request, numbers or text | Reqs "Supported Operations are generated…" (required fields), "Operands are documented as numbers or text…" |
| Main flow 2: Result as text | Req "The Result is documented" |
| Main flow 2 / R-2 / 2a: Operations generated | Req "Supported Operations are generated from the domain"; the `unknown-operation` example and the `200` example are generated too (Decision 4) |
| Main flow 2 / R-3: every rejection with kind, meaning, example | Req "Every rejection kind is documented…": one scenario per `type` and per `invalid-request` reason, incl. unparseable numbers and size |
| R-3 "each documented response says exactly what form it takes" | Req "Every documented response states its form" |
| Main flow 2 / R-4: BR-1 and request limits | Reqs "Operands are documented…", "The limits and other errors are stated" |
| Main flow 3–4: try a Calculation from the pages | Req "…as interactive pages" scenario "Calculations tried from the pages go to the service itself" (task 4.1, against a running app) |
| 4a / R-5: a rejected try gives the same rejection, and it is documented | Each rejection scenario sends the request and compares with the documented example |
| Postcondition "nothing is stored" | No persistence exists; documentation endpoints are `GET` only |
| Failure postcondition (service unavailable → docs unavailable) | Follows from the docs being served by the service; no requirement |
| Non-goals: B-6, availability check, hand-written docs, auth, B-7/B-8/B-9 | proposal.md Out of scope; req "Only the Calculation service is documented" |
| L-4: documented statuses `200`, `400`, `422`; types `invalid-request`, `unknown-operation`, `operand-out-of-range`, `division-by-zero` | One scenario each (the `400`/`422` status scenario plus one per type) |

## Decisions

1. **Dependency.** In `build.gradle.kts` `dependencies`, add `implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")` (exact artifact and version, L-2; STANDARDS.md §1 pins it). It brings `springdoc-openapi-starter-webmvc-api`, `springdoc-openapi-starter-common`, swagger-core/annotations and the swagger-ui webjar transitively. No other dependency. No `springdoc.*` property: the default paths are the ones PRD §3.3 names.

2. **Response statuses as annotations on the controller.** On `CalculationController.calculate`, three `io.swagger.v3.oas.annotations.responses.ApiResponse` annotations (repeatable):
   - `responseCode = "200"`, `description = "The Result of the Calculation"`, `content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalculationResponse.class))`;
   - `responseCode = "400"`, `description = "Invalid request, unknown operation or Operand out of range"`, `content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))`;
   - `responseCode = "422"`, `description = "Division by zero"`, same content as 400.
   Use `MediaType.APPLICATION_JSON_VALUE` / `MediaType.APPLICATION_PROBLEM_JSON_VALUE` for the strings. Do not import `io.swagger.v3.oas.annotations.Operation` (it clashes with `domain.Operation`; not needed).
   *Required:* every response names its media type; without it springdoc documents `200` as `*/*` (probed, row D2). Test: `everyDocumentedResponseNamesItsMediaType`.
   *Alternatives rejected:* adding the responses in the customizer (STANDARDS.md §4 asks for explicit `@ApiResponse` per status); `@ResponseStatus` on the advice handlers so springdoc picks them up (changes the advice, and gives no examples).

3. **One `OpenApiCustomizer` in `config.OpenApiConfig`.** A package-private `@Configuration class OpenApiConfig` with one `@Bean OpenApiCustomizer calculationDocumentation()` (`org.springdoc.core.customizers.OpenApiCustomizer`). It runs after springdoc has built the document and:
   a. sets `info`: `title` `Math API`, `version` `v1`, and `description` (Decision 5);
   b. on `components.schemas.CalculationRequest`: `operation.setEnum(<Operation.values() names, in declaration order>)`; for `a` and `b`, `setTypes(Set.of("number", "string"))`, `setType(null)` and `setDescription(<operand text>)` (Decision 5);
   c. on `components.schemas.ProblemDetail`: `getProperties().remove("properties")` (row D4: springdoc documents `ProblemDetail`'s internal `properties` map, which the JSON never contains);
   d. on the `post` of `/api/v1/calculations`: the request example and the `200` example, both named `divide` (Decision 4) on the `application/json` media types; the six Problem Details examples on the `application/problem+json` media types of `400` and `422`, each an `io.swagger.v3.oas.models.examples.Example` with a `summary` (the kind in words) and a `value` (a `Map` with `type`, `title`, `status`, `detail`, `instance`). Key order does not matter.
   Config may import `domain` (STANDARDS.md §2: config → all). It does not import `adapter` classes (all are package-private).
   *Alternatives rejected:* `@Schema(allowableValues=…)` or `@ExampleObject` text in annotations for domain values (copied, forbidden by STANDARDS.md §4); an `OpenAPI` `@Bean` (would replace springdoc's generated document instead of adjusting it); several customizers (one place is easier to read).

4. **Generated, never copied (R-2, STANDARDS.md §4).** In `OpenApiConfig`:
   - `operation` enum: `Arrays.stream(Operation.values()).map(Enum::name)`.
   - `unknown-operation` `detail`: `new UnknownOperationException().getMessage()`.
   - `operand-out-of-range` `detail`: `new OperandOutOfRangeException("b").getMessage()`.
   - `division-by-zero` `detail`: `new DivisionByZeroException().getMessage()`.
   - BR-1 numbers in descriptions: `Calculation.MAX_SIGNIFICANT_DIGITS` and `Calculation.MAX_SCALE`, which become `public static final` (today `private`; no other change to `Calculation`). `OperandOutOfRangeException` builds its message from the same two constants instead of the literals `100` and `1000`, with the text unchanged (Gate 1 finding A5), so the BR-1 numbers exist once in the domain.
   - Request example: `{"operation": Operation.DIVIDE.name(), "a": 1, "b": "3"}` (`a` an `Integer`, `b` a `String`: it shows both forms). `200` example: from `var c = new Calculation(Operation.DIVIDE, new BigDecimal("1"), new BigDecimal("3"))`: `operation` `c.operation().name()`, `a` `c.a().toPlainString()`, `b` `c.b().toPlainString()`, `result` `c.result().toPlainString()`; computed value `0.3333333333333333333333333333333333` (34 digits, checked in Python `Decimal` with precision 34 and in the probe's live response).
   Adapter-owned text is written as literals in `OpenApiConfig` and pinned by the live comparison: the three `invalid-request` `detail`s (copy exactly from `ApiExceptionHandler`), the `title`s (`Invalid request`, `Unknown operation`, `Operand out of range`, `Division by zero`), the `type` URIs, `instance` `/api/v1/calculations`, the request limits.
   *Required:* the values above are generated as listed. Test: `documentsExactlyTheDomainOperationsInOrder` compares with `Operation.values()`; each rejection test compares with the live body. **Limit (accepted at Gate 1, 2026-11-09):** no test can tell a correct copied literal from a generated value until the domain changes; these tests fail when the documentation drifts from the domain (B-7 adds Operations), and the senior-dev checklist ("documented values copied instead of generated") catches a copy in review.

5. **Description texts.** Operand description (`a`; for `b` the same with `b`): `"An Operand: a decimal number, as a JSON number or a JSON string. As written, at most " + MAX_SIGNIFICANT_DIGITS + " significant digits and a scale from -" + MAX_SCALE + " to " + MAX_SCALE + " (BR-1); at most 1,000 characters."`. `info.description`: one paragraph that says: Math API performs one Calculation (an Operation applied to Operands `a` and `b`) and returns its Result; Operands may be JSON numbers or strings, and every decimal value in a response is a string in plain notation; each Operand has at most N significant digits and a scale from -S to S, as written (BR-1, generated as above); a request is at most 16,384 bytes, and any value at most 1,000 characters; a rejection is RFC 9457 Problem Details (`application/problem+json`) with a stable `type`; every other client error (4xx, for example `404`, `405`, `415`) is Problem Details with `type` `about:blank` (not "every other HTTP error": an unhandled `500` is Boot's default `{timestamp,status,error,path}` JSON; post-Gate-1 edit QA-Q1). Use the glossary words (Operation, Operand, Calculation, Result); avoid "operator", "argument", "answer".

6. **Framework errors described once, not per operation.** UC-005 R-3 lists four rejection kinds; the framework's `about:blank` errors (`404`, `405`, `406`, `415`) are generic to every path, so they are stated in `info.description` only. *Alternative rejected:* `@ApiResponse` for `415` and `405` (more examples to keep in step, for errors a Developer does not choose). Open question Q-1.

7. **`ApiDocumentationTest`.** `src/test/java/com/paulrule/mathapi/ApiDocumentationTest.java`, root package (no ArchUnit layer, so it may import `domain`; it imports no `config` or `adapter` class, L-5), `@SpringBootTest` + `@AutoConfigureMockMvc`, the layer every probe below ran at. It reads `/v3/api-docs` once per test into a Jackson `tools.jackson.databind.JsonNode` (`JsonMapper` from the context, or `new ObjectMapper()` from `tools.jackson.databind`) and navigates with JSON Pointer (`node.at("/paths/~1api~1v1~1calculations/post/responses/400/content/application~1problem+json/examples/unknown-operation/value")`). Each rejection test sends the named request, asserts the documented status, and asserts `JsonNode` equality of the live body and the example value. It is a documentation test, not a second smoke test; it reuses the cached context of `MathApiApplicationTest`'s configuration. Test names in tasks.md.

## Framework defaults relied on (ledger L-3)

Probed on 2026-11-09 by the architect in a throwaway copy of this repo (scratchpad, not the project) with only `implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")` added, Spring Boot 4.1.2 / Spring Framework 7 / Java 25, Gradle `--offline`. **Layer:** `@SpringBootTest` + `@AutoConfigureMockMvc` (full context, MockMvc), except D11.

| # | Default | Input and configuration | Outcome | Status |
|---|---|---|---|---|
| D1 | Default paths | No `springdoc.*` property. `GET /v3/api-docs`; `GET /swagger-ui.html`; `GET /swagger-ui/index.html`; `GET /v3/api-docs/swagger-config`; `GET /v3/api-docs.yaml` | `200 application/json`, `"openapi":"3.1.0"`, default `info` `{"title":"OpenAPI definition","version":"v0"}`; `302` to `/swagger-ui/index.html`; `200 text/html`; `200` with `"url":"/v3/api-docs"`; `200` | Confirmed (apply, 2026-11-09): `ApiDocumentationTest` (`200 application/json`, `3.1.0`, `302`, `text/html`, swagger-config `url`); default `info` seen in the 3.2 run (`OpenAPI definition`). `/v3/api-docs.yaml` not re-checked (not relied on) |
| D2 | `200` media type | No `@ApiResponse` | `200` documented as `"*/*"` with `$ref CalculationResponse`, description `OK`. With Decision 2's annotation: only `application/json` | Confirmed: L-13 mutation (200 `mediaType` removed) gave `["*/*"]`, failing `everyDocumentedResponseNamesItsMediaType` |
| D3 | `@RestControllerAdvice` responses | Current `ApiExceptionHandler` (no `@ResponseStatus` on handlers) | No `400`, `422` or `415` documented at all | Not re-observed (no run with springdoc and without the annotations); superseded by Decision 2, which documents 400/422 explicitly |
| D4 | How `ProblemDetail` is documented | `@Schema(implementation = ProblemDetail.class)` on 400/422 | `components.schemas.ProblemDetail`: `type` (string, uri), `title`, `status` (integer, int32), `detail`, `instance` (string, uri), **and `properties` (object, additionalProperties)**, which no live response contains | Confirmed: 3.2 run, `problemDetailSchemaMatchesTheResponses` saw `properties` before the customizer removed it |
| D5 | Record schemas | Current DTOs | `CalculationRequest`: `operation` string, `a`/`b` `number`, `required` `[a, b, operation]` (from `@NotNull`); `CalculationResponse`: four `string` properties, no `required` | Confirmed: running app (task 4.1) and `required` assertion |
| D6 | Actuator not documented | Default `springdoc.show-actuator` (false), current `management.*` config | `paths` has only `/api/v1/calculations` | Confirmed: `documentsOnlyTheCalculationService`, running app `paths` = `[/api/v1/calculations]` |
| D7 | Customizer applied, 3.1 types | `OpenApiCustomizer` bean setting `enum` on `operation`, `setTypes(Set.of("number","string"))` + `setType(null)` on `a` | `"enum":["ADD","SUBTRACT","MULTIPLY","DIVIDE"]`; `"type":["string","number"]` (order from the `Set`: tests compare as a set) | Confirmed; the running app listed `"type":["number","string"]` (Set order differs from the probe, as expected; tests compare as a set) |
| D8 | Customizer examples | `addExamples("division-by-zero", new Example().summary(…).value(Map.of(…)))` on the `422` media type | Appears under `responses.422.content.application/problem+json.examples.division-by-zero` with `summary` and `value` | Confirmed: all seven examples under the expected keys |
| D9 | Live Problem Details bodies | The six requests in the spec scenarios | Each `application/problem+json` with exactly `type`, `title`, `status`, `detail`, `instance` (`/api/v1/calculations`); details as in `ApiExceptionHandler` and the domain exceptions; `415` for `text/plain` is `about:blank`, title `Unsupported Media Type` | Confirmed: every live-body comparison in `ApiDocumentationTest` passes |
| D10 | `servers` | Default | `[{"url":"http://localhost","description":"Generated server url"}]` under MockMvc: generated from the request | Confirmed: `http://localhost` under MockMvc; running app gave `http://localhost:18080` (task 4.1) |
| D11 | Swagger UI "Try it out" sends to `servers[0].url` | Browser | Not probed (no browser); task 4.1 checks the URL a running app publishes answers a Calculation | Relied on; task 4.1: `POST <servers[0].url>/api/v1/calculations` answered `result` `"0.3"` |
| D12 | Existing tests unaffected | Full `./gradlew test` with the dependency added | All existing test classes pass, 0 failures (`@WebMvcTest` slices unaffected) | Confirmed: 3.2 regression run, all existing tests pass |

## Risks / Trade-offs

- [Copied literal passes the tests (Decision 4 limit)] → equality tests fail on drift; reviewer checklist. Accepted at Gate 1, 2026-11-09; no spec requirement is broken.
- [Adapter `detail` texts copied into config] → each is pinned by a live-body comparison, so a change to `ApiExceptionHandler` fails `ApiDocumentationTest`.
- [Documentation on in every profile until B-6, contrary to the PRD §3.3 and STANDARDS §8 target text] → B-6 is next in the backlog; no deployment exists yet. Accepted at Gate 1, 2026-11-09.
- [Interactive "Try it out" not exercised in a browser] → task 4.1 checks the published server URL against a running app; swagger-ui itself is upstream.
- [B-8 makes `b` optional and changes the `200` shape] → B-8 updates the annotations, customizer and this test (STANDARDS.md §4: extend `ApiDocumentationTest`).

## Gate 1 decisions and post-Gate-1 edits

Accepted at Gate 1, 2026-11-09 (also in tasks.md "Workflow follow-up"):
- Q-1: framework errors (`404`, `405`, `406`, `415`, `about:blank`) are described once in the API description, not as per-operation responses (Decision 6).
- The documentation is on in every profile until B-6. PRD §3.3 and STANDARDS §8 ("prod turns off API docs") describe the target state and are untrue until then.
- The Decision 4 limit: a copied domain value passes the tests until the domain changes.

Post-Gate-1 edits (2026-11-09, architect, from the Gate 1 advisory findings; recorded in tasks.md too, ledger L-1):
- A1: tasks.md 1.1's PRD §3.3 sentence now says the docs cover "the API under `/api/v1`" (the PRD is target-state and lists `GET /api/v1/operations`, B-9). The spec requirement "Only the Calculation service is documented" is unchanged.
- A2: making the `Calculation` constants public moved to task 2.1, before the failing test (now 3.1), so its red output shows the real `404`s and assertion failures.
- A3: STANDARDS §8 is named with PRD §3.3 in the accepted item.
- A4: the request example is named `divide`; tasks.md 3.1 gives its JSON Pointer.
- A5: new task 2.2 builds `OperandOutOfRangeException`'s message from the constants, text unchanged (Decision 4).

Post-Gate-1 edit (2026-11-09, senior-dev code review, from QA finding Q1; recorded in tasks.md too, ledger L-1):
- QA-Q1: `info.description` (Decision 5), the spec requirement "The limits and other errors are stated" and proposal.md now say every other *client error (4xx)* is Problem Details with `type` `about:blank`, not every other *HTTP error*: an unhandled `500` returns Boot's default `{timestamp,status,error,path}` JSON. The scenario (contains `about:blank`) and its test are unchanged. PRD §3.2's pre-existing (UC-002) wording is not edited by this change.

## Open questions

- Q-1: resolved at Gate 1, 2026-11-09 (see above).
