# calculation Specification

## Purpose
Lets a Client service perform a Calculation over HTTP and get its Result (exact, or rounded to 34 significant digits for `DIVIDE`, BR-3), or a Problem Details rejection that states the kind of failure and a plain-language reason (UC-002, UC-003).

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
An Operation that is not exactly one of the supported names (`ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, upper case) SHALL be rejected with `400`, Problem Details `type` `/problems/unknown-operation`, and a `detail` listing the supported Operations in that order (R-3). The given name is not echoed.

#### Scenario: Supported name in the wrong letter case
- **WHEN** a Client service sends `{"operation":"add","a":1,"b":2}`
- **THEN** the response status is `400`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/unknown-operation`
- **AND** `detail` contains `ADD, SUBTRACT, MULTIPLY, DIVIDE`

#### Scenario: Operation not yet supported
- **WHEN** a Client service sends `{"operation":"POWER","a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

#### Scenario: Given name is not echoed
- **WHEN** a Client service sends `{"operation":"CUBE_ROOT","a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`
- **AND** `detail` does not contain `CUBE_ROOT`

#### Scenario: Empty Operation name
- **WHEN** a Client service sends `{"operation":"","a":1,"b":2}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

### Requirement: Only the first failure is reported
When a request has more than one failure, only the first SHALL be reported, checked in this order: invalid request, then unknown operation, then division by zero (R-4, UC-003 R-1, PRD §3.2).

#### Scenario: Non-numeric Operand and unknown Operation
- **WHEN** a Client service sends `{"operation":"add","a":"abc","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Missing Operand and unknown Operation
- **WHEN** a Client service sends `{"operation":"add","b":2}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Malformed JSON naming an unknown Operation
- **WHEN** a Client service sends the body `{"operation":"add","a":1,`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Non-numeric dividend and zero divisor
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"abc","b":0}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Missing dividend and zero divisor
- **WHEN** a Client service sends `{"operation":"DIVIDE","b":0}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Unknown Operation and zero divisor
- **WHEN** a Client service sends `{"operation":"divide","a":1,"b":0}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

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

### Requirement: DIVIDE is correctly rounded
A request naming Operation `DIVIDE` with a non-zero Operand `b` SHALL be answered with `200` and the Result `a ÷ b`. If the quotient cannot be written exactly in 34 significant digits, it is rounded to 34 significant digits, half to even (BR-3). An exact quotient that fits is not padded or rounded. The Result is then normalised (BR-4), and `a` and `b` are echoed as given (UC-002 R-5, R-6).

#### Scenario: One divided by three is rounded to 34 significant digits
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":1,"b":"3"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"DIVIDE","a":"1","b":"3","result":"0.3333333333333333333333333333333333"}`

#### Scenario: Rounding is correct in the last digit
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"2","b":"3"}`
- **THEN** the response status is `200`
- **AND** `result` is `"0.6666666666666666666666666666666667"`

#### Scenario: A tie rounds down to the even digit
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"12345678901234567890123456789012345","b":"10"}`
- **THEN** the response status is `200`
- **AND** `result` is `"1234567890123456789012345678901234"`

#### Scenario: A tie rounds up to the even digit
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"12345678901234567890123456789012335","b":"10"}`
- **THEN** the response status is `200`
- **AND** `result` is `"1234567890123456789012345678901234"`

#### Scenario: An exact quotient is not padded
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1","b":"4"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"DIVIDE","a":"1","b":"4","result":"0.25"}`

#### Scenario: A negative divisor gives a negative Result
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1","b":"-4"}`
- **THEN** the response status is `200`
- **AND** `result` is `"-0.25"`

#### Scenario: A zero dividend gives zero
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"0.00","b":"-3"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"DIVIDE","a":"0.00","b":"-3","result":"0"}`

#### Scenario: A whole quotient is normalised
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1.50","b":"0.5"}`
- **THEN** the response status is `200`
- **AND** the JSON body is exactly `{"operation":"DIVIDE","a":"1.50","b":"0.5","result":"3"}`

#### Scenario: A large quotient stays in plain notation
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"100","b":"0.01"}`
- **THEN** the response status is `200`
- **AND** `result` is `"10000"`

### Requirement: Division by zero is rejected
A `DIVIDE` whose Operand `b` is zero, in any zero form (`0`, `0.00`, `-0`) and given as a JSON number or a JSON string, SHALL be rejected with `422`, Problem Details `type` `/problems/division-by-zero`, a `title`, and a plain-language `detail` (BR-2). The `detail` SHALL NOT echo either Operand (UC-003 R-2) and SHALL NOT expose internals (UC-002 R-7).

#### Scenario: Zero divisor as text
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1","b":"0"}`
- **THEN** the response status is `422`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/division-by-zero`, `status` is `422`, and `title` and `detail` are present

#### Scenario: Zero divisor as a number
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":1,"b":0}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`

#### Scenario: Zero divisor with a scale, as text
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1","b":"0.00"}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`

#### Scenario: Zero divisor with a scale, as a number
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":1,"b":0.00}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`

#### Scenario: Negative zero divisor as text
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1","b":"-0"}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`

#### Scenario: Negative zero divisor as a number
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":1,"b":-0}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`

#### Scenario: Zero divided by zero
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"0","b":"0"}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`

#### Scenario: The reason echoes no Operand and no internals
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"98765","b":"0.00"}`
- **THEN** the response status is `422` and `type` is `/problems/division-by-zero`
- **AND** `detail` contains neither `98765` nor `0.00`
- **AND** `detail` contains none of `Exception`, `java.`, `org.springframework`, `com.paulrule`
