# UC-002: Perform an exact Calculation (ADD, SUBTRACT, MULTIPLY)

**Primary actor:** Client service
**Secondary actors:** None
**Goal:** Get the exact Result of adding, subtracting or multiplying two Operands, or a clear reason why the request was rejected.
**Scope:** Math API (single context: stateless arithmetic, docs/PRD.md)
**Level:** User-goal
**Status:** Draft

## Preconditions
- Math API is available (UC-001).

## Postconditions
- **Success:** The Client service has the Calculation it asked for (the Operation and both Operands exactly as it gave them) together with its exact Result. Nothing is stored.
- **Failure:** The Client service is told the request was rejected, the kind of rejection, and a plain-language reason that reveals nothing about the system's internals. Nothing is stored.

## Main flow (basic course of events)
1. The Client service asks the system to perform a Calculation, naming an Operation (`ADD`, `SUBTRACT` or `MULTIPLY`) and giving two Operands, `a` and `b`. Each Operand may be given as a number or as text (R-1).
2. The system checks the request is well formed and names a supported Operation (R-2, R-3, R-4).
3. The system applies the Operation exactly, with no rounding (BR-3): `a + b`, `a − b` or `a × b`.
4. The system normalises the Result: trailing zeros stripped, plain notation, zero as `0` (BR-4).
5. The system presents the Calculation and its Result to the Client service: the Operation, `a` and `b` exactly as given (R-5), and the Result. Every decimal value is presented so that no precision can be lost by the client (R-6).

## Alternative & exception flows
- **2a. The request is malformed, an Operand is missing, or an Operand is not a decimal number:** the system rejects it as an *invalid request* with a plain-language reason (R-2). The flow ends.
- **2b. The Operation is not a supported name, including a supported name in the wrong letter case (`add`):** the system rejects it as an *unknown operation*, and the reason lists the supported Operations (R-3). The flow ends.
- **2c. The request is both malformed and names an unknown Operation:** only the first failure, in the order of R-4, is reported. Here that is *invalid request*. The flow ends.

## Business rules
- **BR-3 (DOMAIN-MODEL):** `ADD`, `SUBTRACT` and `MULTIPLY` are exact; no rounding.
- **BR-4 (DOMAIN-MODEL):** every Result is normalised, with trailing zeros stripped, in plain (non-scientific) notation; zero is `0`.
- **R-1:** An Operand may be given as a number or as text. Both mean the same decimal value.
- **R-2:** A request is invalid when it cannot be read, when the Operation or either Operand is missing, or when an Operand is not a decimal number.
- **R-3:** An Operation must be exactly one of the supported names, in upper case. Any other name is an unknown operation.
- **R-4:** Only the first failure is reported, checked in this order: invalid request, then unknown operation.
- **R-5:** The Operands are echoed exactly as given, in plain notation and with their given scale (`1e2` is echoed as `100`, `1.50` as `1.50`). Only the Result is normalised (BR-4).
- **R-6:** Every decimal value the system presents (Operands and Result) is presented as text, so a client cannot lose precision reading it.
- **R-7:** Every rejection states its kind and a human-readable reason, and never exposes internal detail. This applies to all rejections, including ones the Client service causes in ways this use case doesn't list.

Edge cases that must hold (DOMAIN-MODEL §3):
- `ADD`: `0.1 + 0.2 = 0.3`; negatives; large precision.
- `SUBTRACT`: a result of zero; a negative result.
- `MULTIPLY`: × 0; sign rules; scale growth.

## Non-goals
- `DIVIDE` (B-3), and any rounding.
- Operand limits (BR-1) and request size limits (B-4).
- API documentation (B-5).
- Other Operations: `POWER`, `MODULO`, `SQRT`, `NEGATE` (B-7, B-8), and unary Calculations generally. Here every Operation takes two Operands.
- Listing the supported Operations as a service of its own (B-9). Only the unknown-operation reason lists them.
- Storing Calculations, authentication, client-chosen precision (PRD §5).

## Open questions (need a human decision before/at Gate 1)
- None.
