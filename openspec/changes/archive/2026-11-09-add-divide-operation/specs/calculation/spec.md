## ADDED Requirements

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

## MODIFIED Requirements

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
