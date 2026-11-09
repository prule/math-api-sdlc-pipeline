# UC-003: Divide two Operands

**Primary actor:** Client service
**Secondary actors:** None
**Goal:** Get the Result of dividing one Operand by another, rounded to a fixed precision, or a clear rejection when dividing by zero.
**Scope:** Math API (single context: stateless arithmetic, docs/PRD.md)
**Level:** User-goal
**Status:** Draft

## Preconditions
- Math API is available (UC-001), and it already performs exact Calculations (UC-002).

## Postconditions
- **Success:** The Client service has the Calculation (`DIVIDE`, `a` and `b` exactly as given) and its Result, `a ÷ b` rounded per BR-3 and normalised per BR-4. Nothing is stored.
- **Failure:** The Client service is told the Calculation was rejected, the kind of rejection, and a plain-language reason that reveals no internals. Nothing is stored.

## Main flow (basic course of events)
1. The Client service asks the system to perform a Calculation with the Operation `DIVIDE` and two Operands, `a` (the dividend) and `b` (the divisor), as in UC-002.
2. The system checks the request as in UC-002 (well formed, supported Operation).
3. The system checks that `b` is not zero (BR-2).
4. The system divides `a` by `b`. If the quotient cannot be written exactly in 34 significant digits, it rounds to 34 significant digits, half to even (BR-3). An exact quotient that fits is not padded or changed.
5. The system normalises the Result (BR-4) and presents the Calculation and its Result, as in UC-002.

## Alternative & exception flows
- **2a. The request is invalid or names an unknown Operation:** rejected exactly as in UC-002. The flow ends.
- **3a. `b` is zero, in any form (`0`, `0.00`, `-0`, given as a number or as text):** the system rejects the Calculation as *division by zero*, with a plain-language reason (BR-2). This is a valid request that breaks a business rule, so it is reported as a different kind of rejection from an invalid request. The flow ends.
- **3b. `a` is zero and `b` is not:** this is not a rejection. The Result is `0`.
- **3c. The request is both invalid and divides by zero (for example, `b` is `0` and `a` is not a number):** only the first failure is reported, in the order of R-1. Here that is *invalid request*. The flow ends.

## Business rules
- **BR-2 (DOMAIN-MODEL):** division by zero is rejected: a `DIVIDE` whose `b` is zero, in any zero representation (`0`, `0.00`, `-0`).
- **BR-3 (DOMAIN-MODEL):** `DIVIDE` is correctly rounded to 34 significant digits, rounding half to even. `ADD`, `SUBTRACT` and `MULTIPLY` stay exact.
- **BR-4 (DOMAIN-MODEL):** every Result is normalised, with trailing zeros stripped, in plain notation; zero is `0`.
- **R-1:** Only the first failure is reported, checked in this order: invalid request, then unknown operation, then division by zero.
- **R-2:** A division-by-zero reason does not echo the Operands.
- UC-002's rules R-1 to R-7 (Operands as number or text, echo as given, decimals as text, rejections without internals) apply unchanged. Its unknown-operation reason now also lists `DIVIDE`.

Edge cases that must hold (DOMAIN-MODEL §3):
- `1 ÷ 3` = `0.3333333333333333333333333333333333` (34 significant digits).
- `1 ÷ 4` = `0.25` (exact, not padded).
- A negative divisor, for example `1 ÷ -4` = `-0.25`.
- BR-2 for each zero representation: `0`, `0.00`, `-0`.

## Non-goals
- `POWER` and `MODULO`, including their own division-by-zero cases under BR-2 (B-7).
- Operand limits (BR-1) and request-size limits (B-4).
- Client-chosen precision or rounding (PRD §5, B-10).
- API documentation (B-5).

## Open questions (need a human decision before/at Gate 1)
- None.
