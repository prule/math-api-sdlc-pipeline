# Tasks

Follows the profile's Task order. Steps 2–5 (domain, application) and 6–8 (web adapter, Problem Details, OpenAPI) have no work in this change: it adds no domain, application or adapter class and no API endpoint. The build skeleton (group 2) comes before the first test because no test can run without it.

## 1. Docs

- [x] 1.1 Update `docs/PRD.md` §3.3: replace the `GET /actuator/health` bullet with the contract: `200` `{"status":"UP"}` when ready to accept Calculations; `503` with a status-only body (`OUT_OF_SERVICE`) while starting or shutting down; no components, details, versions or configuration; no authentication; no other management endpoint exposed over HTTP. Add a `docs/PRD.md` §8 change-log row dated today citing UC-001. Verify by reading the diff: only §3.3 and §8 change. DOMAIN-MODEL.md and STANDARDS.md: no change (proposal.md - Docs to update).

## 2. Build skeleton

- [x] 2.1 Install Gradle 9.8.2 locally (e.g. `sdk install gradle 9.8.2`) and run `gradle wrapper --gradle-version 9.8.2` in the project root. Verify `./gradlew --version` reports Gradle 9.8.2 and `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew`, `gradlew.bat` exist.
- [x] 2.2 Write `settings.gradle.kts` (`rootProject.name = "math-api"`) and `build.gradle.kts` per design.md Decision 6: Boot 4.1.2, dependency management plugin 1.1.7, Java 25 toolchain, group `com.paulrule`, dependencies `spring-boot-starter-webmvc`, `spring-boot-starter-actuator`, `spring-boot-starter-webmvc-test` (test scope), `archunit-junit5:1.5.1`, `junit-platform-launcher` (`testRuntimeOnly`, needed by Gradle 9), JUnit Platform for tests; Spotless 8.11.0 with `palantirJavaFormat("2.103.0")` for Java and `ktlint()` for `*.gradle.kts`. No springdoc, no validation starter.
- [x] 2.3 Add `src/main/java/com/paulrule/mathapi/MathApiApplication.java` (`@SpringBootApplication` with `main`) and `src/main/resources/application.yaml` with `spring.application.name: math-api` only. Verify `./gradlew clean build` succeeds (no tests yet).
- [x] 2.4 Prove Spotless is wired: misformat `MathApiApplication.java` (e.g. collapse it onto one line), verify `./gradlew build` fails on `spotlessJavaCheck`, run `./gradlew spotlessApply`, verify `./gradlew build` passes again.

## 3. Architecture enforcement (STANDARDS.md §2)

- [x] 3.1 Write the failing fixture test first: add test-only `src/test/java/com/paulrule/archfixture/domain/FrameworkDependentDomainClass.java` that uses a Spring annotation (e.g. `@org.springframework.stereotype.Component`), and in `src/test/java/com/paulrule/mathapi/ArchitectureTest.java` a plain JUnit test `domainRuleRejectsAFrameworkDependentDomainClass` that imports only `com.paulrule.archfixture` (via `ClassFileImporter`) and asserts the domain-is-framework-free rule throws `AssertionError`. Verify it fails (rule not written yet / does not compile).
- [x] 3.2 Implement the rules in `ArchitectureTest` per design.md Decision 5 as `static final ArchRule` fields with `allowEmptyShould(true)`, selecting packages only by the relative patterns `..domain..`, `..application..`, `..adapter..`, `..config..` (so the domain rule matches the fixture's `com.paulrule.archfixture.domain`): layered architecture (`domain`, `application`, `adapter.in.web`, `config`; optional layers; `config` may access all; no layer accesses `config`; only `config` accesses `adapter`), `domainDependsOnNoFramework` (`org.springframework..`, `jakarta..`, `com.fasterxml..`, `tools.jackson..`), `applicationDependsOnNoWebOrAdapter` (`org.springframework.web..`, `..adapter..`). Check them with `@ArchTest`s under `@AnalyzeClasses(packages = "com.paulrule.mathapi")`, so production checks import only `com.paulrule.mathapi` and never see the fixture. Verify `./gradlew test --tests '*ArchitectureTest'` passes, including 3.1's test.

## 4. Service availability (spec: service-availability)

- [x] 4.1 Write failing tests in one `@SpringBootTest` `@AutoConfigureMockMvc` class `src/test/java/com/paulrule/mathapi/MathApiApplicationTest.java`, one test per scenario, each sending the scenario's exact request:
  - `answersAvailableWhenStarted`: `GET /actuator/health` → `200`, body exactly `{"status":"UP"}` (Scenario: Started service answers available).
  - `answersNotAvailableWhileRefusingTraffic`: publish `AvailabilityChangeEvent` `ReadinessState.REFUSING_TRAFFIC`; `GET /actuator/health` → `503`, body exactly `{"status":"OUT_OF_SERVICE"}`; then publish `ACCEPTING_TRAFFIC` and `GET /actuator/health` → `200`, body exactly `{"status":"UP"}` (Scenarios: Service refusing traffic answers not available; Service becomes available again once ready). Restore `ACCEPTING_TRAFFIC` in a `finally` so other tests are unaffected.
  - `availabilityAnswerHasOnlyStatus`: `GET /actuator/health` → body has exactly one key `status`, no `components`, no `details` (Scenario: Health body has no detail).
  - `doesNotExposeOtherManagementEndpoints`: `GET /actuator/env` → `404` and `GET /actuator/info` → `404` (Scenario: Other management endpoints are not exposed).
  - `answersWithoutCredentials`: `GET /actuator/health` with no `Authorization` header → `200` (Scenario: No credentials needed).
  - `repeatedChecksGiveTheSameAnswer`: two consecutive `GET /actuator/health` → both `200` `{"status":"UP"}` (Scenario: Repeated checks give the same answer).
  Verify `answersNotAvailableWhileRefusingTraffic` fails before 4.2 (health stays `UP` without readiness in the health group).
- [x] 4.2 Add `management.health.readinessstate.enabled: true` and `management.endpoint.health.probes.enabled: false` (Boot 4.1.2 turns probes on by default) to `application.yaml` (design.md Decision 2), each with a one-line comment why. Do not change `show-details`/`show-components`, or widen endpoint exposure. Verify all `MathApiApplicationTest` tests pass.

## 5. CI

- [x] 5.1 Add `.github/workflows/build.yml` per design.md Decision 8 (on `pull_request` and `push` to `main`; checkout, Temurin 25 via `actions/setup-java`, `gradle/actions/setup-gradle`, `./gradlew build`). Verify by inspection that it runs `./gradlew build` on both triggers with Java 25; it is proven by the first PR.

## 6. Verify

- [x] 6.1 Run `./gradlew clean build` and paste the output: compile, `ArchitectureTest`, `MathApiApplicationTest` and Spotless check all pass.

## Workflow follow-up

- Tick B-1 in `BACKLOG.md`, archive the change and write the retrospective (done by the pipeline after Gate 2).
