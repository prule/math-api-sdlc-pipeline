# api-documentation Specification

## Purpose
Lets a Developer learn the Calculation service (every Operation, the request, the Result, every rejection and every limit) and try a Calculation, from OpenAPI documentation that Math API publishes about itself and that always matches its behaviour (UC-005).

## Requirements

### Requirement: Documentation is published in machine-readable form
Math API SHALL answer `GET /v3/api-docs` with `200`, content type `application/json`, and an OpenAPI 3.1 document describing the Calculation service. No credentials are needed (UC-005 R-1, PRD §5).

#### Scenario: Developer fetches the machine-readable documentation
- **WHEN** a Developer sends `GET /v3/api-docs` with no credentials
- **THEN** the response is `200` with content type `application/json`
- **AND** its `openapi` field is `3.1.0`
- **AND** its `info.title` is `Math API` and its `info.version` is `v1`

### Requirement: Documentation is published as interactive pages
Math API SHALL serve interactive documentation pages built from the machine-readable documentation, from which a Developer can send a Calculation to the service itself. No credentials are needed (UC-005 R-1, main flow 4).

#### Scenario: Developer opens the interactive pages
- **WHEN** a Developer sends `GET /swagger-ui.html` with no credentials
- **THEN** the response redirects (`302`) to `/swagger-ui/index.html`
- **AND** `GET /swagger-ui/index.html` answers `200` with content type `text/html`

#### Scenario: Interactive pages load the service's own documentation
- **WHEN** a Developer's browser sends `GET /v3/api-docs/swagger-config`
- **THEN** the response is `200` and its `url` is `/v3/api-docs`

#### Scenario: Calculations tried from the pages go to the service itself
- **WHEN** a Developer reads the documentation served by a running Math API
- **THEN** its first `servers` entry is the address the documentation was requested from (no fixed host)
- **AND** sending `{"operation":"ADD","a":"0.1","b":"0.2"}` to `POST <that address>/api/v1/calculations` gives `200` with `result` `"0.3"`

### Requirement: Only the Calculation service is documented
The documentation SHALL describe exactly one operation, `POST /api/v1/calculations`. The availability check and other management endpoints SHALL NOT appear in it (UC-005 non-goals).

#### Scenario: Documented paths
- **WHEN** a Developer reads `paths` in `/v3/api-docs`
- **THEN** it has exactly one key, `/api/v1/calculations`, with exactly one method, `post`
- **AND** no key starts with `/actuator`

### Requirement: Supported Operations are generated from the domain
The documented values of the request's `operation` SHALL be exactly the supported Operations, in the order the service lists them in the `unknown-operation` reason, generated from the service's own list so a new Operation appears without editing the documentation (UC-005 R-2, 2a).

#### Scenario: Documented Operations match the supported Operations
- **WHEN** a Developer reads the `operation` property of the request schema
- **THEN** it is a string whose allowed values are exactly `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, in that order
- **AND** the request schema lists `operation`, `a` and `b` as required

### Requirement: Operands are documented as numbers or text within their limits
Each Operand (`a`, `b`) in the request schema SHALL be documented as a JSON number or a JSON string, and its description SHALL state the BR-1 limits generated from the domain and the 1,000-character limit on a value (UC-005 R-4).

#### Scenario: Operand schema
- **WHEN** a Developer reads the `a` and `b` properties of the request schema
- **THEN** each allows the types `number` and `string`
- **AND** each description contains `at most 100 significant digits` and `a scale from -1000 to 1000` (BR-1) and `1,000 characters`

### Requirement: The Result is documented
The `200` response SHALL be documented with content type `application/json` only, a schema whose `operation`, `a`, `b` and `result` are strings, and an example that is the exact body the service returns for the documented request example.

#### Scenario: Documented 200 response matches a real Calculation
- **WHEN** a Developer sends the documented request example `{"operation":"DIVIDE","a":1,"b":"3"}` to `POST /api/v1/calculations`
- **THEN** the response is `200` and its body equals the documented `200` example, `{"operation":"DIVIDE","a":"1","b":"3","result":"0.3333333333333333333333333333333333"}` (34 significant digits, BR-3)
- **AND** the documented `200` response has exactly one content type, `application/json`, and every property of its schema is of type `string`

### Requirement: Every rejection kind is documented with the response the service gives
The `400` and `422` responses SHALL be documented as `application/problem+json` with one named example per rejection kind (UC-005 R-3). Each example SHALL be the exact Problem Details body the service returns for its named request, and its status SHALL be the documented status (R-5, 4a).

#### Scenario: Invalid request because the body cannot be read
- **WHEN** a Developer sends `{"operation":"ADD","a":"x","b":2}`, or the unparseable number `{"operation":"ADD","a":"1e2147483648","b":1}`
- **THEN** each response is `400` and its body equals the documented `400` example `invalid-request-unreadable`, whose `type` is `/problems/invalid-request`

#### Scenario: Invalid request because a field is missing
- **WHEN** a Developer sends `{"operation":"ADD","a":1}`
- **THEN** the response is `400` and its body equals the documented `400` example `invalid-request-missing-field`, whose `type` is `/problems/invalid-request` and whose `detail` is `Field 'b' is required.`

#### Scenario: Invalid request because the request is too large
- **WHEN** a Developer sends `{"operation":"ADD","a":"<1 repeated 1001 times>","b":2}`
- **THEN** the response is `400` and its body equals the documented `400` example `invalid-request-too-large`, whose `type` is `/problems/invalid-request` and whose `detail` contains `16,384 bytes` and `1,000 characters`

#### Scenario: Unknown operation
- **WHEN** a Developer sends `{"operation":"add","a":1,"b":2}`
- **THEN** the response is `400` and its body equals the documented `400` example `unknown-operation`, whose `type` is `/problems/unknown-operation` and whose `detail` names the supported Operations `ADD, SUBTRACT, MULTIPLY, DIVIDE`

#### Scenario: Operand out of range
- **WHEN** a Developer sends `{"operation":"ADD","a":"1","b":"1E+1001"}` (scale -1001, outside BR-1)
- **THEN** the response is `400` and its body equals the documented `400` example `operand-out-of-range`, whose `type` is `/problems/operand-out-of-range` and whose `detail` names Operand `b` but not its value

#### Scenario: Division by zero
- **WHEN** a Developer sends `{"operation":"DIVIDE","a":1,"b":0}` (BR-2)
- **THEN** the response is `422` and its body equals the documented `422` example `division-by-zero`, whose `type` is `/problems/division-by-zero`

#### Scenario: Rejection statuses are documented as Problem Details
- **WHEN** a Developer reads the documented responses of `POST /api/v1/calculations`
- **THEN** they are exactly `200`, `400` and `422`
- **AND** `400` has exactly the examples `invalid-request-unreadable`, `invalid-request-missing-field`, `invalid-request-too-large`, `unknown-operation`, `operand-out-of-range`
- **AND** `422` has exactly the example `division-by-zero`

### Requirement: Every documented response states its form
Every documented response SHALL name exactly one content type, never `*/*`. The documented Problem Details schema SHALL list exactly the fields the service returns: `type`, `title`, `status`, `detail`, `instance` (UC-005 R-3).

#### Scenario: No unspecified response form
- **WHEN** a Developer reads every response in `/v3/api-docs`
- **THEN** each has exactly one content type, `application/json` for `200` and `application/problem+json` for `400` and `422`
- **AND** none is `*/*`

#### Scenario: Problem Details schema matches the responses
- **WHEN** a Developer reads the schema of the `400` and `422` responses
- **THEN** its properties are exactly `type`, `title`, `status`, `detail` and `instance`

### Requirement: The limits and other errors are stated
The API description SHALL state the BR-1 limits (generated from the domain), the request limits (at most 16,384 bytes per request and 1,000 characters per value), and that every other client error (4xx) is Problem Details with `type` `about:blank` (UC-005 R-4, PRD §3.2).

#### Scenario: API description states the limits
- **WHEN** a Developer reads `info.description` in `/v3/api-docs`
- **THEN** it contains `at most 100 significant digits` and `a scale from -1000 to 1000` (BR-1)
- **AND** it contains `16,384 bytes` and `1,000 characters`
- **AND** it contains `about:blank`
