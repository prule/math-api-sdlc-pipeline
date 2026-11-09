# Design

## Context

Greenfield: no build, source or CI. Java 25.0.4 is installed locally; Gradle is **not** (only needed once, to generate the wrapper). The stack, layering, formatting and CI are fixed by STANDARDS.md §1, §2, §5, §7. PRD §3.3 already names `GET /actuator/health` as the liveness/readiness endpoint. Gate 1 confirmed the foundations are in scope (see proposal.md - Why).

## Goals / Non-Goals

**Goals:**
- The `service-availability` spec, met with Spring Boot Actuator configuration only: no hand-written controller.
- A build every later use case can extend: `./gradlew clean build` compiles, tests, runs ArchUnit and checks Spotless; CI runs the same.

**Non-Goals:**
- Classes in `domain`, `application`, `adapter.in.web` or `config`: none are needed yet, so none are created (STANDARDS.md §5, no speculative abstraction). ArchUnit rules are written now so the first class B-2 adds is checked.
- Problem Details for framework errors (B-2 adds the single `@RestControllerAdvice`).

## Use case mapping

| UC-001 item | Covered by |
|---|---|
| Main flow 1–3 | Requirement "Report available when ready to accept Calculations" |
| 2a not / no longer ready | Requirement "Report not available when running but not ready" |
| 1a not running | Not a system behaviour (no process answers); the Client's fallback is its own. Out of scope. |
| R-1 available = ready | Readiness state drives the answer (Decision 2) |
| R-2 status only | Requirement "Availability answer reveals nothing else" |
| R-3 no side effects, no auth | Requirement "Availability check is open and side-effect free" |

## Decisions

1. **Actuator health, not a custom endpoint.** PRD §3.3 already promises `/actuator/health`; Actuator's defaults already give `show-details: never`, `show-components: never` and web exposure of `health` only, which satisfy R-2 with zero code. A custom controller would duplicate this and add an adapter class with nothing to adapt. Alternative rejected: `GET /api/v1/health` controller.
2. **Readiness drives the answer.** Set `management.health.readinessstate.enabled: true` in `application.yaml`. This adds Spring's readiness state to the root health group, so the answer is `OUT_OF_SERVICE` / `503` while the application refuses traffic (before it is ready, and during graceful shutdown, which is Boot's default) and `UP` / `200` once ready. Without it, health is `UP` as soon as the HTTP server answers, so flow 2a has no meaning. Also set `management.endpoint.health.probes.enabled: false`: Boot 4.1.2 turns probes on by default, which adds separate `/liveness` and `/readiness` answers (a UC-001 non-goal) and a `groups` key to the root health body (breaks R-2).
3. **State the defaults we rely on explicitly?** No, except the probes default overridden in Decision 2: `show-details`, `show-components` and exposure keep their defaults; the tests pin the behaviour instead, so a future change of default fails a test rather than silently leaking detail.
4. **One `@SpringBootTest` covers the spec** (STANDARDS.md §6 allows exactly one smoke test). Actuator is not a controller, so a web-slice test does not apply. Class `MathApiApplicationTest` with a mock MVC environment (`@AutoConfigureMockMvc`). The not-ready scenario publishes `AvailabilityChangeEvent` with `ReadinessState.REFUSING_TRAFFIC` from the test, asserts `503`, then publishes `ACCEPTING_TRAFFIC` and asserts `200` again, restoring shared context state without `@DirtiesContext`.
5. **ArchUnit with optional layers and a violation fixture.** `ArchitectureTest` in `com.paulrule.mathapi` encodes STANDARDS.md §2. Every rule selects packages with relative patterns (`..domain..`, `..application..`, `..adapter..`, `..config..`), never the absolute `com.paulrule.mathapi.*` names, so the same rule matches both production classes and the fixture. The rules are: a `layeredArchitecture().consideringOnlyDependenciesInLayers()` with `withOptionalLayers(true)` (layers `domain`, `application`, `adapter.in.web`, `config`; `config` may access all, nothing may access `config` or `adapter`); `domain` depends on no `org.springframework..`, `jakarta..`, `com.fasterxml..`, `tools.jackson..`; `application` depends on no `org.springframework.web..` and no `..adapter..`. Rules are `static final ArchRule` fields and use `allowEmptyShould(true)` because the layer packages are empty until B-2 (ArchUnit otherwise fails a rule that matches no classes). The production `@ArchTest`s import only `com.paulrule.mathapi` (`@AnalyzeClasses(packages = "com.paulrule.mathapi")`). Because empty packages mean the rules pass vacuously, a separate plain test imports only `com.paulrule.archfixture` (via `ClassFileImporter`), a test-only fixture package (`com.paulrule.archfixture.domain.FrameworkDependentDomainClass`, which imports a Spring annotation) and asserts the domain rule **fails** on it. Its package matches `..domain..`, so the domain rule applies and fails as expected. The fixture sits outside `com.paulrule.mathapi` so the production rules never see it.
6. **Build file.** Plugins: `java`, `org.springframework.boot` 4.1.2, `io.spring.dependency-management` 1.1.7 (the plugins block needs a version; 1.1.7 is the current release), `com.diffplug.spotless` 8.11.0. Toolchain Java 25. Dependencies and why:
   - `spring-boot-starter-webmvc`: HTTP server for the API (Boot 4 name of the former `starter-web`).
   - `spring-boot-starter-actuator`: the health endpoint (Decision 1).
   - `spring-boot-starter-webmvc-test` (`testImplementation`): provides `@AutoConfigureMockMvc` in Boot 4 and brings JUnit, AssertJ and Spring Test.
   - `com.tngtech.archunit:archunit-junit5:1.5.1`: §2 enforcement.
   - `org.junit.platform:junit-platform-launcher` (`testRuntimeOnly`, version from Boot's BOM): Gradle 9 no longer supplies the JUnit Platform launcher, so tests do not run without it.
   Spotless: `java { palantirJavaFormat("2.103.0") }`, `kotlinGradle { ktlint() }`. Spotless wires `spotlessCheck` into `check`, so `./gradlew build` fails on unformatted code. Group `com.paulrule`.
7. **Wrapper generation.** Gradle is not installed; the implementer installs Gradle 9.8.2 once (e.g. `sdk install gradle 9.8.2`, SDKMAN is present) and runs `gradle wrapper --gradle-version 9.8.2`, then commits `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and `gradle-wrapper.properties` (already un-ignored in `.gitignore`).
8. **CI.** `.github/workflows/build.yml`: on `pull_request` and `push` to `main`; `ubuntu-latest`; `actions/checkout`, `actions/setup-java` (Temurin 25), `gradle/actions/setup-gradle`; run `./gradlew build`. Not runnable locally; reviewed by inspection.

## Risks / Trade-offs

- [Boot 4 moved test auto-configuration into per-technology modules and renamed starters] → Confirm at build time that `spring-boot-starter-webmvc-test` resolves under Boot 4.1.2 and provides `@AutoConfigureMockMvc`; if it does not, fix the artifact name. Do not switch to a different test style.
- [`management.health.readinessstate.enabled` might differ in Boot 4] → The spec scenario governs; if the property does not add readiness to the root group, find the Boot 4 equivalent (do not add a custom `HealthIndicator`).
- [Vacuous ArchUnit rules on empty packages] → The violation-fixture test (Decision 5) proves the domain rule bites.
- [CI workflow untested until the first push] → Kept to the standard four steps; the first PR proves it.
- [`404` for unknown paths and `503` health bodies are not Problem Details] → Health uses Actuator's own JSON by design; general framework errors become Problem Details in B-2.

## Migration Plan

None: first deployable version. Rollback is reverting the change.
