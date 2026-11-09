# Proposal

## Why

A Developer integrating a Client service can learn the Calculation contract today only from the code or the PRD. B-5 (UC-005) completes v1 scope (PRD §3.3): the service publishes its own OpenAPI 3.1 documentation, machine-readable and interactive, so the Developer can learn every Operation, the request, the Result, every rejection and every limit, and try a Calculation, without reading the code.

## What Changes

- Add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0` (the version STANDARDS.md §1 pins). With its defaults the service publishes `GET /v3/api-docs` (OpenAPI 3.1 JSON) and `GET /swagger-ui.html` (interactive pages, redirect to `/swagger-ui/index.html`). No credentials (UC-005 R-1, PRD §5).
- `POST /api/v1/calculations` is documented completely (STANDARDS.md §4):
  - the request: `operation` limited to the supported Operations, generated from the domain (R-2); `a` and `b` documented as a JSON number or string, with the BR-1 limits generated from the domain;
  - `200` as `application/json` (not springdoc's default `*/*`) with the Result, all decimal values as strings, and an example generated from the domain;
  - `400` and `422` as `application/problem+json`, with one example per Problem Details `type`: `invalid-request` (unreadable or unparseable, missing field, too large), `unknown-operation`, `operand-out-of-range`, `division-by-zero` (R-3). Each example is the exact body the service returns for a named request (R-5);
  - the API description states the BR-1 limits, the request limits (16,384 bytes, 1,000 characters per value) (R-4), and that other client errors (4xx) are Problem Details with `type` `about:blank`.
- Only the Calculation service is documented. `/actuator/health` is not (springdoc's default).
- New `ApiDocumentationTest` checks all of the above against the published documentation and the live responses.
- No behaviour of the Calculation service changes. **Not breaking.**

## Capabilities

### New Capabilities
- `api-documentation`: the OpenAPI documentation the service publishes about itself: where it is, what it documents, and that it matches the service's behaviour.

### Modified Capabilities
- None. The `calculation` requirements and its Purpose line stay true (ledger L-10: no follow-up needed).

## Out of scope

- Turning the documentation off under the `prod` profile (B-6). PRD §3.3 already states the target ("off under the `prod` profile"); until B-6 lands, the documentation is on in every profile. This change does not add `application-prod.yaml`.
- Documenting the availability check (`/actuator/health`, UC-001) or any other management information.
- New Operations (B-7, B-8), Arity and an optional `b` (B-8), the Operation listing service (B-9). They appear in the documentation when built (R-2).
- Per-operation documentation of framework errors (`404`, `405`, `406`, `415`). They are described once, in the API description (design.md Decision 6, open question Q-1).
- Hand-written documentation (README, guides), authentication of the documentation.

## Docs to update

- **PRD.md**: §3.3 OpenAPI bullet: state that the documentation covers only `POST /api/v1/calculations` (not the availability check) and gives one `application/problem+json` example per error `type`. Change log row.
- **STANDARDS.md**: §4: each documented Problem Details example is the exact body the service returns for a named request, and `ApiDocumentationTest` sends that request and compares. Change log row.
- **DOMAIN-MODEL.md**: none. No new term or business rule.

## Impact

- Build: one new dependency (above), justified by STANDARDS.md §1, which already pins it.
- Code: new `config.OpenApiConfig`; `@ApiResponse` annotations on `adapter.in.web.CalculationController`; `domain.Calculation`'s two BR-1 constants become `public` so the documentation can be generated from them. No behaviour change.
- Tests: new `com.paulrule.mathapi.ApiDocumentationTest`; existing tests unchanged.
- API: new `GET` endpoints `/v3/api-docs`, `/v3/api-docs/swagger-config`, `/swagger-ui.html`, `/swagger-ui/**` (and springdoc's `/v3/api-docs.yaml`). The Calculation contract does not change.
