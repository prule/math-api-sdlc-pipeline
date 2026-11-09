# SDLC pipeline profile

<!-- Read by every sdlc-pipeline agent and skill. Detail lives in docs/STANDARDS.md; this file cites it.
Field reference: https://github.com/prule/sdlc-pipeline/blob/main/docs/profile.md -->

## Commands

- **Verify:** `./gradlew clean build` (compile, tests, ArchUnit, Spotless check). Every agent runs this
  to prove its work and pastes the output. Until B-1 is built there is no Gradle wrapper; B-1 creates it.
- **Code generation:** none.
- **Never run:** `./gradlew bootRun` in the foreground (it never exits); any `git push`, `gh pr merge`
  or deploy command.

## Paths

- **Domain knowledge:** `docs/DOMAIN-MODEL.md` (glossary, model, operations, business rules BR-n) and
  `docs/PRD.md` (scope, API contract, errors, non-goals)
- **Standards:** `docs/STANDARDS.md`
- **Use cases:** `use-cases/`
- **Retrospectives:** `retrospectives/`
- **Session reports:** `reports/sessions/`

## Git

- **Base branch:** `main`
- **Branch per use case:** `feat/uc-<n>-<slug>`, cut from the base branch before the pipeline starts
- **Pull requests target:** `main`
- **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`) (docs/STANDARDS.md §7)

## Pipeline

- **Implementer:** `junior-dev`. Override one run with `--senior` or `--junior`.
- **Static analysis:** `off`.

## Implementation rules

- Stack and versions are pinned: Java 25, Spring Boot 4.1.2, Gradle 9.8.2 Kotlin DSL, springdoc 3.2.0,
  ArchUnit 1.5.1, Spotless 8.11.0 + palantir-java-format 2.103.0. New dependencies are justified in the
  change (docs/STANDARDS.md §1).
- Clean architecture in one module, `com.paulrule.mathapi`: `domain` → `application` → `adapter.in.web`,
  `config` wires use cases as beans. `domain` imports no framework; `application` imports no Spring web
  or adapter types; adapters hold no business rules; DTOs stay in the adapter. ArchUnit enforces it
  (docs/STANDARDS.md §2).
- Numbers are `BigDecimal`, never `double`/`float`. Code and tests enforcing a business rule cite its
  `BR-n` (docs/STANDARDS.md §3).
- Adding an Operation is one `Operation` constant with its arity, its rules in DOMAIN-MODEL.md, and tests;
  anything else needing to change is design drift (docs/STANDARDS.md §3).
- Domain failures are domain exceptions, never HTTP-aware; one `@RestControllerAdvice` maps them to RFC
  9457 `ProblemDetail` with the stable `type` URIs in PRD §3.2. `400` invalid input, `422` domain-rule
  violation (docs/STANDARDS.md §3, §4).
- Request/response bodies are records; decimals in responses are JSON strings in plain notation;
  requests accept numbers or strings (docs/STANDARDS.md §4).
- Every endpoint is fully described in the OpenAPI docs (explicit `@ApiResponse` per status, a
  `application/problem+json` example per `type`); domain-derived values are generated, never copied
  (docs/STANDARDS.md §4).
- Records, no Lombok, no setters in the domain, no speculative abstraction (docs/STANDARDS.md §5).
- `application.yaml` holds local defaults; overrides go in `application-<profile>.yaml`, each with an
  `@ActiveProfiles` test (docs/STANDARDS.md §8).
- A change that alters scope, API, Operations, business rules or standards updates the matching doc in
  `docs/`, including its change log. Never renumber or reuse a BR id.

## Task order

1. docs (PRD / DOMAIN-MODEL / STANDARDS updates) → 2. failing domain test → 3. domain → 4. failing
   application test → 5. application use case + `config` wiring → 6. failing web-slice test → 7. web
   adapter + Problem Details mapping → 8. OpenAPI docs + `ApiDocumentationTest` → 9. smoke test →
   10. `./gradlew clean build`

## Review checklist

### Plan review (spec-reviewer)

- **`docs/STANDARDS.md §2`:** each new class has a named layer; no task puts a rule in an adapter or a
  framework type in `domain`.
- **`docs/STANDARDS.md §3`:** spec scenarios use DOMAIN-MODEL.md glossary terms exactly and cite BR-n;
  a new Operation needs only a constant, its rules and tests (plus an exception and handler for a new
  failure kind).
- **`docs/STANDARDS.md §4`:** every new error has a PRD `type`, the right status (400/422) and a place in
  the PRD §3.2 validation order.
- **`docs/STANDARDS.md §6`:** tasks are test-first; every DOMAIN-MODEL.md edge case for the Operation
  and every error path has a named test.
- **Docs:** the proposal names which of PRD.md, DOMAIN-MODEL.md, STANDARDS.md it updates (or "none"),
  and tasks.md updates each, including its change log.

### Verification (qa)

- **`docs/STANDARDS.md §6`:** domain unit test for every Operation, every edge case in DOMAIN-MODEL.md
  §3 and every business rule; MockMvc web-slice test for every Problem Details path in the PRD; one
  `@SpringBootTest` smoke test; ArchUnit for §2.
- **`docs/STANDARDS.md §4`:** `ApiDocumentationTest` covers every endpoint and error `type`.
- **`docs/STANDARDS.md §8`:** each profile file has an `@ActiveProfiles` test.

### Code review (senior-dev)

- **`docs/STANDARDS.md §2`:** layer violations, DTOs leaking inward, rules in controllers.
- **`docs/STANDARDS.md §3`:** `double`/`float` for values; missing BR-n citations; HTTP-aware domain
  exceptions.
- **`docs/STANDARDS.md §4`:** non-Problem-Details errors; leaked internals in `detail`; echoed
  out-of-range Operand values; documented values copied instead of generated.
- **`docs/STANDARDS.md §5`:** Lombok, setters, speculative abstraction, comments that restate the code.
- **`docs/STANDARDS.md §6`:** tests named after methods instead of behaviour; tests that can pass
  without exercising the rule.

## Formatting

Spotless (palantir-java-format) is checked by `./gradlew build`. No pre-commit hook: if the check fails,
run `./gradlew spotlessApply` and re-verify. Never disable or weaken Spotless, an ArchUnit rule or a test
to get green.
