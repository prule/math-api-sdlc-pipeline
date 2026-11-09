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
When a request has more than one failure, only the first SHALL be reported, checked in this order: invalid request (including an oversized request, UC-004 R-1), then unknown operation, then operand out of range (BR-1, Operand `a` before Operand `b`), then division by zero (R-4, UC-003 R-1, UC-004 R-3, PRD §3.2).

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

#### Scenario: Oversized body naming an unknown Operation
- **WHEN** a Client service sends `{"operation":"add","a":1,"b":2}` with spaces added before the final `}` until the body is exactly 16,385 bytes
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Missing Operand and an out-of-range Operand
- **WHEN** a Client service sends `{"operation":"ADD","a":"1E+1001"}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Unknown Operation and an out-of-range Operand
- **WHEN** a Client service sends `{"operation":"add","a":"1E+1001","b":1}`
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

#### Scenario: Both Operands out of range
- **WHEN** a Client service sends `{"operation":"ADD","a":"1E+1001","b":"1E-1001"}`
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`
- **AND** `detail` contains `Operand a` and does not contain `Operand b`

#### Scenario: Out-of-range dividend and zero divisor
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1E+1001","b":"0"}`
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`, not `/problems/division-by-zero`

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

### Requirement: Operand outside the limits is rejected
Each Operand, as written, SHALL have at most 100 significant digits and a scale from -1000 to 1000 (BR-1). Otherwise the Calculation SHALL be rejected before any arithmetic with `400`, Problem Details `type` `/problems/operand-out-of-range`, a `title` and a `detail` that names the Operand (`a` or `b`) but never echoes its value (UC-004 R-2). The rejection SHALL be prompt whatever the size of the value (UC-004 R-4).

#### Scenario: Scale of -1000 is accepted
- **WHEN** a Client service sends `{"operation":"ADD","a":"1E+1000","b":"0"}`
- **THEN** the response status is `200`
- **AND** `result` is `1` followed by 1000 zeros

#### Scenario: Scale of 1000 is accepted
- **WHEN** a Client service sends `{"operation":"ADD","a":"1E-1000","b":"0"}`
- **THEN** the response status is `200`
- **AND** `result` is `0.` followed by 999 zeros and then `1`

#### Scenario: 100 significant digits are accepted
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"<a>","b":"1"}` where `<a>` is the digit `1` written 100 times
- **THEN** the response status is `200`
- **AND** `result` is the digit `1` written 100 times

#### Scenario: A value written with an exponent is measured as written
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":"1E+100","b":"1"}`
- **THEN** the response status is `200`
- **AND** `result` is `1` followed by 100 zeros

#### Scenario: Scale of -1001 is rejected
- **WHEN** a Client service sends `{"operation":"ADD","a":"1E+1001","b":"1"}`
- **THEN** the response status is `400`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/operand-out-of-range`, `status` is `400`, and `title` and `detail` are present
- **AND** `detail` contains `Operand a`

#### Scenario: Scale of 1001 is rejected
- **WHEN** a Client service sends `{"operation":"ADD","a":"1","b":"1E-1001"}`
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`
- **AND** `detail` contains `Operand b`

#### Scenario: 101 significant digits are rejected even for a small value
- **WHEN** a Client service sends `{"operation":"ADD","a":"<a>","b":"1"}` where `<a>` is `1` followed by 100 zeros
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`

#### Scenario: 101 significant digits as a JSON number are rejected
- **WHEN** a Client service sends `{"operation":"ADD","a":<a>,"b":1}` where `<a>` is the JSON number `1` followed by 100 zeros
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`

#### Scenario: A short but enormous value is rejected at once
- **WHEN** a Client service sends `{"operation":"MULTIPLY","a":1e999999999,"b":"1e999999999"}`
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`
- **AND** the response arrives within 1 second

#### Scenario: Enormous scales in a division are rejected, not a server failure
- **WHEN** a Client service sends `{"operation":"DIVIDE","a":"1E-999999999","b":"1E+999999999"}`
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`

#### Scenario: The reason names the Operand but not its value
- **WHEN** a Client service sends `{"operation":"ADD","a":"1","b":"98765E+1001"}`
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range`
- **AND** `detail` contains `Operand b`
- **AND** `detail` contains neither `98765` nor `98765E+1001`
- **AND** `detail` contains none of `Exception`, `java.`, `tools.jackson`, `org.springframework`, `com.paulrule`

### Requirement: Oversized request is rejected
A request body longer than 16,384 bytes (UTF-8), or with any JSON string or number value longer than 1,000 characters, SHALL be rejected with `400`, Problem Details `type` `/problems/invalid-request` and a `detail` that states the size limits, before any value is read as a number (UC-004 R-1). A body of exactly 16,384 bytes, or a value of exactly 1,000 characters, SHALL NOT be rejected for size.

#### Scenario: Body one character over the limit
- **WHEN** a Client service sends `{"operation":"ADD","a":1,"b":2}` with spaces added before the final `}` until the body is exactly 16,385 bytes
- **THEN** the response status is `400`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/invalid-request`
- **AND** `detail` contains `16,384` and `1,000`

#### Scenario: Body exactly at the limit
- **WHEN** a Client service sends `{"operation":"ADD","a":1,"b":2}` with spaces added before the final `}` until the body is exactly 16,384 bytes
- **THEN** the response status is `200`
- **AND** `result` is `"3"`

#### Scenario: Operand text one character over the limit
- **WHEN** a Client service sends `{"operation":"ADD","a":"<a>","b":1}` where `<a>` is `1` followed by 1,000 zeros (1,001 characters)
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Operand JSON number one character over the limit
- **WHEN** a Client service sends `{"operation":"ADD","a":<a>,"b":1}` where `<a>` is the JSON number `1` followed by 1,000 zeros (1,001 characters)
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`

#### Scenario: Operand text exactly at the limit is not rejected for size
- **WHEN** a Client service sends `{"operation":"ADD","a":"<a>","b":1}` where `<a>` is `1` followed by 999 zeros (1,000 characters)
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range` (BR-1), not `/problems/invalid-request`

#### Scenario: Operand JSON number exactly at the limit is not rejected for size
- **WHEN** a Client service sends `{"operation":"ADD","a":<a>,"b":1}` where `<a>` is the JSON number `1` followed by 999 zeros (1,000 characters)
- **THEN** the response status is `400` and `type` is `/problems/operand-out-of-range` (BR-1), not `/problems/invalid-request`

#### Scenario: Operation name over the limit
- **WHEN** a Client service sends `{"operation":"<op>","a":1,"b":2}` where `<op>` is the letter `X` written 1,001 times
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`, not `/problems/unknown-operation`

#### Scenario: Operation name exactly at the limit is not rejected for size
- **WHEN** a Client service sends `{"operation":"<op>","a":1,"b":2}` where `<op>` is the letter `X` written 1,000 times
- **THEN** the response status is `400` and `type` is `/problems/unknown-operation`

### Requirement: Operand that cannot be represented is rejected
An Operand whose exponent is too large to represent as a decimal number at all (scale beyond ±2,147,483,647) SHALL be rejected with `400` and Problem Details `type` `/problems/invalid-request`, given as text or as a JSON number. It SHALL NOT give a `500`.

#### Scenario: Unrepresentable exponent as text
- **WHEN** a Client service sends `{"operation":"ADD","a":"1e2147483648","b":1}`
- **THEN** the response status is `400`
- **AND** the content type is `application/problem+json`
- **AND** `type` is `/problems/invalid-request`

#### Scenario: Unrepresentable exponent as a JSON number
- **WHEN** a Client service sends `{"operation":"ADD","a":1e2147483648,"b":1}`
- **THEN** the response status is `400` and `type` is `/problems/invalid-request`
