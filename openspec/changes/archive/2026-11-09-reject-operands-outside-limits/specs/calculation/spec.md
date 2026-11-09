# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

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
