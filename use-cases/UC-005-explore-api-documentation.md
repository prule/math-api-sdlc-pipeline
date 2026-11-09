# UC-005: Explore the API from its documentation

**Primary actor:** Developer (integrating a Client service with Math API)
**Secondary actors:** None
**Goal:** Learn every Operation, how to ask for a Calculation, what a Result looks like and every kind of rejection, from documentation the service publishes itself, and try a Calculation, without reading the code.
**Scope:** Math API (single context: stateless arithmetic, docs/PRD.md §3.3)
**Level:** User-goal
**Status:** Draft

## Preconditions
- Math API is available (UC-001), and it performs Calculations (UC-002 to UC-004).

## Postconditions
- **Success:** the Developer knows the supported Operations, the request and the Result, every rejection kind with an example, and the limits on Operands and requests. They have tried a Calculation from the documentation. Nothing is stored.
- **Failure:** none specific to this use case. If the service is not available, the documentation is not either (UC-001).

## Main flow (basic course of events)
1. The Developer asks the system for its API documentation, either in a machine-readable form (for tools and client generators) or as interactive pages (for people) (R-1).
2. The system presents documentation of the Calculation service. It covers:
   - the request: an Operation and its Operands, which may be given as numbers or text;
   - the Result: all decimal values as text;
   - the supported Operations, generated from the system's own list, so they always match what it accepts (R-2);
   - every rejection the service can give, each with its kind, its meaning and an example (R-3);
   - the limits on Operands (BR-1) and on request size (R-4).
3. The Developer reads it and chooses a Calculation to try.
4. From the interactive pages, the Developer sends that Calculation to the system and sees the real Result or rejection, as in UC-002 to UC-004.

## Alternative & exception flows
- **4a. The Developer tries a Calculation that is rejected (for example, division by zero):** the system gives the same rejection, with the same kind, as it gives any Client service, and the documentation describes that rejection (R-3). The flow ends.
- **2a. A new Operation is added to the system later:** the documentation lists it without anyone editing the documentation (R-2).

## Business rules
- **R-1:** the documentation is published by the service itself, in a machine-readable form and as interactive pages. In v1 it needs no credentials, like the rest of the API (PRD §5).
- **R-2:** anything the documentation lists that comes from the domain, such as the supported Operations, is generated from the domain, never copied. So it can't drift from what the service accepts.
- **R-3:** every rejection kind the service can give is documented, with its meaning and an example:
  - *invalid request*, including oversized requests and numbers the service cannot read;
  - *unknown operation*;
  - *operand out of range*;
  - *division by zero*.

  Each documented response says exactly what form it takes; none is left unspecified.
- **R-4:** the documentation states the limits a Developer must respect: BR-1 (100 significant digits, scale within ±1000, checked as written), and requests of at most 16,384 bytes with values of at most 1,000 characters.
- **R-5:** the documentation describes the service as it is. Every Operation, rejection and limit it states matches the behaviour in UC-002 to UC-004 and PRD §3.2.

## Non-goals
- Turning the documentation off in production (B-6).
- Documenting the availability check (UC-001) or other management information. Only the Calculation service is documented.
- Hand-written documentation outside what the service publishes (README, guides).
- Authentication or access control for the documentation (PRD §5).
- New Operations (B-7, B-8) and the Operation listing service (B-9). They will appear in the documentation when they are built (R-2).

## Open questions (need a human decision before/at Gate 1)
- None.
