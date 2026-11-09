# UC-004: Reject Operands outside the limits

**Primary actor:** Client service
**Secondary actors:** None
**Goal:** Have a Calculation with an out-of-range Operand, or an oversized request, rejected promptly and clearly, before any arithmetic, so that no request can exhaust the service.
**Scope:** Math API (single context: stateless arithmetic, docs/PRD.md)
**Level:** User-goal (an extension of UC-002 and UC-003: a stricter business rule on every Calculation)
**Status:** Draft

## Preconditions
- Math API is available (UC-001), and it performs Calculations (UC-002, UC-003).

## Postconditions
- **Success (Operands within limits):** the Calculation proceeds exactly as in UC-002 and UC-003. Nothing about its Result changes.
- **Failure:** the Client service is told the request was rejected, the kind of rejection and a plain-language reason. The rejection comes before any arithmetic, quickly, whatever size of value was sent. It never echoes the offending value. Nothing is stored, and the service stays available for everyone else.

## Main flow (basic course of events)
1. The Client service asks the system to perform a Calculation, as in UC-002 or UC-003.
2. Before reading any number, the system checks the request is within the size limits (R-1).
3. The system checks the request is well formed and names a supported Operation, as in UC-002.
4. The system checks each Operand, as written, against the limits of BR-1.
5. The Calculation proceeds as in UC-002 or UC-003 (including the BR-2 division-by-zero check), and the system presents the Calculation and its Result.

## Alternative & exception flows
- **2a. The request is longer than 16,384 characters, or any single value in it (number or text) is longer than 1,000 characters:** the system rejects it as an *invalid request*, without reading it into numbers (R-1). The flow ends.
- **4a. An Operand has more than 100 significant digits as written, or a scale outside ±1000:** the system rejects the Calculation as *operand out of range*. The reason names which Operand (`a` or `b`) but never its value (BR-1, R-2). No arithmetic is done. The flow ends.
- **4b. Both Operands are out of range:** only the first failure is reported. Operand `a` is checked before `b` (R-3). The flow ends.
- **4c. An Operand is out of range and the Calculation would also divide by zero (for example `a` = `1E+1001`, `b` = `0`, `DIVIDE`):** *operand out of range* is reported, because it comes before division by zero (R-3). The flow ends.

## Business rules
- **BR-1 (DOMAIN-MODEL):** an Operand, as written, has at most 100 significant digits and a scale within ±1000. Otherwise the Calculation is rejected (operand out of range), before any arithmetic.
  - `1E+1000` and `1E-1000` are accepted; `1E+1001` and `1E-1001` are rejected.
  - A 101-digit integer (`1` followed by 100 zeros) is rejected, even though its value is small; written as `1E+100` it is accepted.
  - Checking the written form, not the normalised value, is deliberate: it bounds both the arithmetic and the echoed Operands.
- **R-1:** a request longer than 16,384 characters, or with any single number or text value longer than 1,000 characters, is an *invalid request*. It is rejected before any value is read as a number.
- **R-2:** an operand-out-of-range reason names the Operand (`a` or `b`) and never echoes its value.
- **R-3:** only the first failure is reported, checked in this order: invalid request (including R-1), then unknown operation, then operand out of range (BR-1, `a` before `b`), then division by zero (BR-2).
- **R-4:** every out-of-range or oversized value is rejected quickly. Even a value such as `1e999999999`, which is short to write but enormous in value, is rejected before any arithmetic, so it cannot use up the service's time or memory.
- UC-002's rules R-1 to R-7 and UC-003's rules still apply unchanged to Calculations within the limits.

Edge cases that must hold:
- Exactly at each limit is accepted: 100 significant digits; scale `1000` (`1E-1000`); scale `-1000` (`1E+1000`).
- One past each limit is rejected: 101 significant digits; `1E-1001`; `1E+1001`.
- `1e999999999 × 1e999999999` is rejected at once as operand out of range.
- `1E-999999999 ÷ 1E+999999999` is rejected as operand out of range, not as a server failure.
- A request of exactly 16,384 characters, and a value of exactly 1,000 characters, are not rejected for size (they may still break another rule).

## Non-goals
- Making the limits configurable (PRD: limits are fixed).
- Limits on the Result of `ADD`, `SUBTRACT`, `MULTIPLY` or `DIVIDE`. Bounding the Operands bounds them. The POWER Result bound (BR-6) comes with B-7.
- Rate limiting or authentication (PRD §5, B-12).
- API documentation (B-5).

## Open questions (need a human decision before/at Gate 1)
- None.
