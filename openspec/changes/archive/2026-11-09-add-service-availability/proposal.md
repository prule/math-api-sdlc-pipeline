# Proposal

## Why

UC-001 (B-1): a Client service needs to learn whether Math API is up and ready to accept Calculations before it sends any. The repository is greenfield (docs and OpenSpec only: no build, no source, no CI), so every later use case (B-2 onward) also needs a buildable, layered, checked service to grow in.

**Scope confirmed at Gate 1:** the UC-001 open question was answered **yes**, so this change also lays the foundations every later use case relies on (Gradle build and wrapper, the layered package structure with ArchUnit enforcement, Spotless, CI).

## What Changes

- **Availability check**: `GET /actuator/health` (already named in PRD §3.3) answers `200` with `{"status":"UP"}` when Math API is ready to accept Calculations, and `503` with a status-only body when it is running but not ready (still starting or shutting down). The answer exposes nothing else: no components, details, versions or configuration (R-2). No authentication, no side effects (R-3).
- **Only the health endpoint is exposed** among management endpoints; others (e.g. `/actuator/env`, `/actuator/info`) are not reachable over HTTP.
- **Foundations (confirmed at Gate 1)**:
  - Gradle 9.8.2 Kotlin DSL build with committed wrapper, Java 25 toolchain, Spring Boot 4.1.2; dependencies limited to what this change uses: webmvc starter, actuator starter, test starters, ArchUnit 1.5.1.
  - Application entry point in `com.paulrule.mathapi`; ArchUnit tests encoding the STANDARDS.md §2 dependency rules, proven to fail on a deliberate violation.
  - Spotless 8.11.0 (palantir-java-format 2.103.0 for Java, ktlint for `*.gradle.kts`) checked by `./gradlew build`.
  - GitHub Actions `.github/workflows/build.yml` running `./gradlew build` on every PR and push to `main`.

## Out of scope

- Any Calculation or Operation behaviour, domain classes, `@RestControllerAdvice` / Problem Details mapping (B-2 onward). Consequently, framework error responses such as `404` for unknown paths are not yet Problem Details; B-2 introduces the single advice.
- OpenAPI docs, springdoc dependency and `ApiDocumentationTest` (B-5); the `prod` profile (B-6).
- `spring-boot-starter-validation` (first needed by B-2).
- Separate liveness and readiness answers (UC-001 non-goal; R-1 makes them the same).
- Monitoring, alerting, uptime history.

## Docs to update

- **PRD.md**: yes. §3.3 — state the `/actuator/health` contract (200 `UP` when ready, 503 when not, status only, no details, no authentication) and add a change-log row.
- **DOMAIN-MODEL.md**: none. "Available" is an operational property of the service, not a domain term; no Operand, Operation, Calculation or BR-n changes.
- **STANDARDS.md**: none. The stack, structure, tooling and CI introduced here are exactly those STANDARDS.md already prescribes.

## Capabilities

### New Capabilities

- `service-availability`: how a Client service learns whether Math API is ready to accept Calculations, and what that answer may reveal.

### Modified Capabilities

None.

## Impact

- New files: `settings.gradle.kts`, `build.gradle.kts`, Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), `src/main/java/com/paulrule/mathapi/MathApiApplication.java`, `src/main/resources/application.yaml`, tests under `src/test/java/com/paulrule/mathapi/`, `.github/workflows/build.yml`.
- New dependencies (justified in design.md): Spring Boot webmvc and actuator starters, Spring Boot test starters, ArchUnit JUnit 5.
- Public surface: `GET /actuator/health` only.
