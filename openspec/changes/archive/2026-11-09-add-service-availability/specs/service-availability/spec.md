# Spec Delta

## Purpose

Lets a Client service learn whether Math API is ready to accept Calculations, through an answer that reveals only available or not available (UC-001).

## ADDED Requirements

### Requirement: Report available when ready to accept Calculations
When Math API has started and is ready to accept Calculations (UC-001 R-1), a `GET /actuator/health` request SHALL be answered with HTTP `200` and a JSON body whose `status` is `UP`.

#### Scenario: Started service answers available
- **WHEN** a Client service sends `GET /actuator/health` to a started Math API
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"status":"UP"}`

### Requirement: Report not available when running but not ready
When Math API is running but not, or no longer, ready to accept Calculations (still starting, or shutting down), a `GET /actuator/health` request SHALL be answered with HTTP `503` and a JSON body whose `status` is not `UP` (UC-001 flow 2a, R-1).

#### Scenario: Service refusing traffic answers not available
- **WHEN** Math API's readiness changes to refusing traffic
- **AND** a Client service sends `GET /actuator/health`
- **THEN** the response status is `503`
- **AND** the JSON body is exactly `{"status":"OUT_OF_SERVICE"}`

#### Scenario: Service becomes available again once ready
- **WHEN** Math API's readiness changes back to accepting traffic
- **AND** a Client service sends `GET /actuator/health`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"status":"UP"}`

### Requirement: Availability answer reveals nothing else
The availability answer SHALL contain only the `status` field: no components, details, versions or configuration (UC-001 R-2). No other management endpoint SHALL be reachable over HTTP.

#### Scenario: Health body has no detail
- **WHEN** a Client service sends `GET /actuator/health`
- **THEN** the JSON body has exactly one key, `status`
- **AND** it has no `components` or `details` key

#### Scenario: Other management endpoints are not exposed
- **WHEN** a Client service sends `GET /actuator/env` or `GET /actuator/info`
- **THEN** the response status is `404`

### Requirement: Availability check is open and side-effect free
Any caller SHALL be able to ask whether Math API is available without credentials, and asking SHALL change nothing (UC-001 R-3; no authentication in v1, PRD §5).

#### Scenario: No credentials needed
- **WHEN** a Client service sends `GET /actuator/health` with no `Authorization` header
- **THEN** the response status is `200`

#### Scenario: Repeated checks give the same answer
- **WHEN** a Client service sends `GET /actuator/health` twice in a row to a started Math API
- **THEN** both responses are `200` with body `{"status":"UP"}`
