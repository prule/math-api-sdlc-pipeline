# Math API — Domain Model

> Living document. Update it in the same change as any new or changed term, operation or business rule (see `CLAUDE.md`).
> Related: [PRD](PRD.md) · [Standards](STANDARDS.md)

The domain is framework-free Java in `com.paulrule.mathapi.domain`. Specs, code and tests cite business rules by ID (`BR-n`). Never renumber or reuse an ID; mark retired rules as *Retired*.

## 1. Glossary

| Term | Definition | Code |
|------|------------|------|
| **Operand** | A decimal number within the limits of BR-1. | `BigDecimal` (validated) |
| **Operation** | A named arithmetic function of one or two Operands (its Arity). Closed set. | `enum Operation` |
| **Arity** | How many Operands an Operation takes: 1 (unary, `a` only) or 2 (binary, `a` and `b`). | `Operation.arity()` |
| **Calculation** | One Operation applied to its Operands `(operation, a, b)`; `b` is absent for a unary Operation (BR-9). Immutable. | `record Calculation` |
| **Result** | The decimal value produced by performing a Calculation. | `BigDecimal` |

Use these words exactly in code, specs, API docs and conversation. Avoid synonyms ("operator", "argument", "answer").

## 2. Model

```
Calculation
  ├── operation : Operation   { ADD | SUBTRACT | MULTIPLY | DIVIDE | POWER | MODULO | SQRT | NEGATE }
  ├── a         : Operand
  └── b         : Operand, or absent for a unary Operation (BR-9)

Operation.arity()     → 1 | 2
Operation.apply(a, b) → Result   (b is null for a unary Operation)
```

- Each `Operation` constant declares its arity and implements `apply(BigDecimal a, BigDecimal b)`; a unary constant ignores `b`.
- Adding an operation = one new constant with its arity + its business rules here + tests. A new kind of failure also needs a domain exception and its Problem Details handler; nothing else changes.
- `Operation.named(name)` is an exact, case-sensitive lookup; any other name raises `UnknownOperationException`.
- The `Calculation` constructor checks BR-9 (arity), then BR-1 on `a`, then on `b` (only the first failure is reported).
- `Calculation.result()` applies the Operation and normalises the Result (BR-4 trailing-zero stripping). Plain-notation rendering happens at the web adapter.
- Domain failures are domain exceptions (`UnknownOperationException`, `ArityMismatchException`, `DivisionByZeroException`, `OperandOutOfRangeException`, `InvalidExponentException`, `NegativeSquareRootException`, `ResultOutOfRangeException`); the web adapter maps them to Problem Details.

## 3. Operations

| Operation | Rule | Edge cases that must be tested |
|-----------|------|--------------------------------|
| `ADD` | `a + b`, exact | `0.1 + 0.2 = 0.3`; negatives; large precision |
| `SUBTRACT` | `a − b`, exact | result zero; negative result |
| `MULTIPLY` | `a × b`, exact | × 0; sign rules; scale growth |
| `DIVIDE` | `a ÷ b` under BR-3 | BR-2; `1 ÷ 3` rounding; exact `1 ÷ 4 = 0.25`; negative divisor |
| `POWER` | `a ^ b`, integer b (BR-5), under BR-3, result bounded (BR-6) | `2^10 = 1024`; `2^−2 = 0.25`; `3^−1` rounding; `(−2)^3 = −8`; `2^200` rounded; `0^0 = 1`; `2^3.0 = 8`; `2^0.5`, `2^1000` rejected; `0^−1` (BR-2); `100^999`, `0.01^999` rejected; worst operands within 1 s |
| `MODULO` | `a mod b`, truncated remainder (BR-7) | `−7 mod 3 = −1`; `7 mod −3 = 1`; `5.5 mod 2 = 1.5`; `−7.5 mod 2 = −1.5`; `6 mod 3 = 0`; `1E+1000 mod 7 = 4`; b = 0 (BR-2) |
| `SQRT` | `√a`, unary, under BR-3 | `√4 = 2`; `√0.25 = 0.5`; `√2`, `√10` rounding; ties to even; `√1E+1000 = 1E+500`; `√0`, `√−0`, `√0.00 = 0`; negative a (BR-8); b given (BR-9) |
| `NEGATE` | `−a`, unary, exact | `5 → −5`; `−2.50 → 2.5`; every zero form → `0`; b given (BR-9) |

## 4. Business rules

| ID | Rule |
|----|------|
| **BR-1** | An Operand, **as written**, has at most **100 significant digits** (`precision() ≤ 100`) and a scale within **±1000** (`-1000 ≤ scale() ≤ 1000`). Otherwise the Calculation is rejected (operand out of range), before any arithmetic. Examples: `1E+1000` and `1E-1000` are accepted; `1E+1001` and `1E-1001` are rejected; a 101-digit integer such as `1` followed by 100 zeros is rejected even though its value is small — write it as `1E+100`. Checking the written form (not the normalised value) is deliberate: it bounds both the arithmetic and the echoed operands. |
| **BR-2** | Division by zero is rejected: a `DIVIDE` or `MODULO` whose `b` is zero, and a `POWER` of zero to a negative exponent. Applies to any zero representation (`0`, `0.00`, `-0`). |
| **BR-3** | `DIVIDE`, `POWER` and `SQRT` use `MathContext.DECIMAL128`: 34 significant digits, `RoundingMode.HALF_EVEN`, correctly rounded (`POWER` computes the exact power, then rounds once). `ADD`, `SUBTRACT`, `MULTIPLY`, `MODULO`, `NEGATE` are exact (no rounding). |
| **BR-4** | Every Result is normalised with trailing zeros stripped and returned in plain (non-scientific) notation; zero is `0`. |
| **BR-5** | A `POWER` exponent `b` must be an integer (`3.0` counts as 3) with \|b\| ≤ 999. Otherwise the Calculation is rejected (invalid exponent). A negative `b` means `1 / a^\|b\|`; `a^0 = 1` for every `a`, including zero. |
| **BR-6** | A `POWER` Result, after rounding and normalisation (BR-4), must have a scale within ±1000 (the BR-1 bounds). Otherwise it is rejected (result out of range). |
| **BR-7** | `MODULO` is the remainder truncated toward zero, `a − b × trunc(a ÷ b)`, so it takes the sign of `a` (`−7 mod 3 = −1`). |
| **BR-8** | `SQRT` of an Operand below zero is rejected (negative square root). Every zero representation (`0`, `-0`, `0.00`) is not below zero; its square root is `0`. |
| **BR-9** | An Operation takes exactly its arity of Operands. `SQRT` and `NEGATE` are unary: `b` must be absent. Every other Operation is binary: `b` must be present. Otherwise the Calculation is rejected (invalid request), before BR-1. |

## 5. Change log

| Date | Change |
|------|--------|
| 2026-11-09 | Copied from math-api (its final v1 docs) as the target for the sdlc-pipeline rebuild |
| 2026-11-09 | §2: BR-1 checks `a` before `b` (UC-004) |
