# calculation Specification

## Purpose
Lets a Client service perform a Calculation over HTTP and get its exact Result, or a Problem Details rejection that states the kind of failure and a plain-language reason (UC-002).

## Requirements

### Requirement: ADD is exact
A `POST /api/v1/calculations` request naming Operation `ADD` with Operands `a` and `b` SHALL be answered with `200` and the Result `a + b`, with no rounding (BR-3).

#### Scenario: Decimal fractions add exactly
- **WHEN** a Client service sends `{"operation":"ADD","a":"0.1","b":"0.2"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"ADD","a":"0.1","b":"0.2","result":"0.3"}`

#### Scenario: Decimal fractions given as JSON numbers add exactly
- **WHEN** a Client service sends `{"operation":"ADD","a":0.1,"b":0.2}`
- **THEN** the response status is `200`
- **AND** `result` is `"0.3"`

#### Scenario: Negative Operands add
- **WHEN** a Client service sends `{"operation":"ADD","a":"-1.5","b":"-2.25"}`
- **THEN** `result` is `"-3.75"`

#### Scenario: Large precision is kept
- **WHEN** a Client service sends `{"operation":"ADD","a":"12345678901234567890.123456789012345678901234567890","b":"0.000000000000000000000000000001"}`
- **THEN** `result` is `"12345678901234567890.123456789012345678901234567891"`

### Requirement: SUBTRACT is exact
A request naming Operation `SUBTRACT` SHALL be answered with `200` and the Result `a − b`, with no rounding (BR-3).

#### Scenario: Result of zero
- **WHEN** a Client service sends `{"operation":"SUBTRACT","a":"1.50","b":"1.5"}`
- **THEN** the response status is `200`
- **AND** `result` is `"0"`

#### Scenario: Negative Result
- **WHEN** a Client service sends `{"operation":"SUBTRACT","a":"2","b":"5.5"}`
- **THEN** `result` is `"-3.5"`

### Requirement: MULTIPLY is exact
A request naming Operation `MULTIPLY` SHALL be answered with `200` and the Result `a × b`, with no rounding (BR-3).

#### Scenario: Multiply by zero
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"123.45","b":"0"}`
- **THEN** `result` is `"0"`

#### Scenario: Negative times positive
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"-2","b":"3"}`
- **THEN** `result` is `"-6"`

#### Scenario: Negative times negative
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"-2","b":"-3"}`
- **THEN** `result` is `"6"`

#### Scenario: Scale grows
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"1.25","b":"1.25"}`
- **THEN** `result` is `"1.5625"`

### Requirement: Result is normalised
Every Result SHALL have trailing zeros stripped and be presented in plain (non-scientific) notation; a zero Result is `0` (BR-4).

#### Scenario: Trailing zeros are stripped
- **WHEN** a Client service sends `{"operation":"ADD","a":"1.10","b":"2.20"}`
- **THEN** `result` is `"3.3"`

#### Scenario: Large Result stays in plain notation
- **WHEN** a Client service sends `{"operation":"ADD","a":"50","b":"50"}`
- **THEN** `result` is `"100"`

#### Scenario: Small Result stays in plain notation
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"0.001","b":"0.001"}`
- **THEN** `result` is `"0.000001"`

### Requirement: Operands are accepted as numbers or text
Each Operand SHALL be accepted as a JSON number or a JSON string, and both forms SHALL mean the same decimal value (R-1).

#### Scenario: One Operand as number, one as string
- **WHEN** a Client service sends `{"operation":"ADD","a":1.5,"b":"2.5"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"ADD","a":"1.5","b":"2.5","result":"4"}`

### Requirement: The Calculation is echoed as given
The response SHALL echo the Operation and both Operands: each Operand in plain notation with its given scale (R-5), and every decimal value (Operands and Result) as a JSON string (R-6). Only the Result is normalised.

#### Scenario: Operands keep their given scale and notation
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":1e2,"b":"1.50"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"MULTIPLY","a":"100","b":"1.50","result":"150"}`

#### Scenario: Decimal values are JSON strings
- **WHEN** a Client service sends `{"operation":"ADD","a":1,"b":2}`
- **THEN** `a`, `b` and `result` in the response are JSON strings (`"1"`, `"2"`, `"3"`), not JSON numbers

### Requirement: Invalid request is rejected
A request that cannot be read, lacks the Operation or either Operand (absent or `null`), or has an Operand that is not a decimal number SHALL be rejected with `400`, Problem Details `type` `/problems/invalid-request` and a plain-language `detail` (R-2).

#### Scenario: Malformed JSON
- **WHEN** a Client service sends the body `{"operation":"ADD","a":1,`
- **THEN** the response status is `400`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/invalid-request`

#### Scenario: Empty body
- **WHEN** a Client service sends `POST /api/v1/calculations` with `Content-Type: application/json` and an empty body
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Operation missing
- **WHEN** a Client service sends `{"a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`
- **AND** `detail` names the field `operation`

#### Scenario: Operand a missing
- **WHEN** a Client service sends `{"operation":"ADD","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`
- **AND** `detail` names the field `a`

#### Scenario: Operand b null
- **WHEN** a Client service sends `{"operation":"ADD","a":1,"b":null}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`
- **AND** `detail` names the field `b`

#### Scenario: Operand is non-numeric text
- **WHEN** a Client service sends `{"operation":"ADD","a":"abc","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Operand is a boolean
- **WHEN** a Client service sends `{"operation":"ADD","a":true,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Operand is empty text
- **WHEN** a Client service sends `{"operation":"ADD","a":"","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

### Requirement: Unknown operation is rejected
An Operation that is not exactly one of the supported names (`ADD`, `SUBTRACT`, `MULTIPLY`, upper case) SHALL be rejected with `400`, Problem Details `type` `/problems/unknown-operation`, and a `detail` listing the supported Operations in that order (R-3). The given name is not echoed.

#### Scenario: Supported name in the wrong letter case
- **WHEN** a Client service sends `{"operation":"add","a":1,"b":2}`
- **THEN** the response status is `400`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/unknown-operation`
- **AND** `detail` contains `ADD, SUBTRACT, MULTIPLY`

#### Scenario: Operation not yet supported
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

#### Scenario: Given name is not echoed
- **WHEN** a Client service sends `{"operation":"CUBE_ROOT","a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`
- **AND** `detail` does not contain `CUBE_ROOT`

#### Scenario: Empty Operation name
- **WHEN** a Client service sends `{"operation":"","a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

### Requirement: Only the first failure is reported
When a request has more than one failure, only the first SHALL be reported, checked in this order: invalid request, then unknown operation (R-4).

#### Scenario: Non-numeric Operand and unknown Operation
- **WHEN** a Client service sends `{"operation":"add","a":"abc","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Missing Operand and unknown Operation
- **WHEN** a Client service sends `{"operation":"add","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Malformed JSON naming an unknown Operation
- **WHEN** a Client service sends the body `{"operation":"add","a":1,`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

### Requirement: Every rejection is Problem Details without internals
Every error response SHALL be `application/problem+json` with `type`, `title`, `status` and a human-readable `detail`, including errors this spec does not list (R-7). A `detail` SHALL NOT contain exception class names, package names or stack traces. Errors outside the PRD §3.2 table use `type` `about:blank`.

#### Scenario: Rejection detail exposes no internals
- **WHEN** a Client service sends `{"operation":"ADD","a":"abc","b":2}`
- **THEN** `title`, `status` and `detail` are present
- **AND** `detail` contains none of `Exception`, `java.`, `tools.jackson`, `com.fasterxml`, `org.springframework`, `com.paulrule`

#### Scenario: Unsupported media type
- **WHEN** a Client service sends `POST /api/v1/calculations` with `Content-Type: text/plain` and body `ADD 1 2`
- **THEN** the response status is `415`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `about:blank`, `title` is `Unsupported Media Type`, `status` is `415` and `detail` is present

#### Scenario: Method not allowed
- **WHEN** a Client service sends `GET /api/v1/calculations`
- **THEN** the response status is `405`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `about:blank`, `title` is `Method Not Allowed` and `detail` is present
