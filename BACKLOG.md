# Backlog

<!-- Product: a stateless service that applies one arithmetic Operation to its Operands and returns an exact decimal Result (docs/PRD.md).
     Primary actor: a client service or developer calling the API. Core goal: get an exact Result for a Calculation.
     Entries mirror the original math-api backlog one-for-one (see "Original item") so the two builds can be compared.
     Original item 5b (repo hygiene, from its v2 audit of its own code) has no entry. -->

## MVP (2/5 built)

- [x] **B-1: Check the service is available** — built: [UC-001](use-cases/UC-001-check-service-availability.md), change add-service-availability
  - **Actor:** Client service
  - **Goal:** Learn whether the service is up and ready to accept Calculations.
  - **Why here:** Every later entry needs a running, buildable service; this one carries no business behaviour.
  - **Depends on:** None
  - **Original item:** 1, walking skeleton (`add-walking-skeleton`)
- [x] **B-2: Perform an exact Calculation (ADD, SUBTRACT, MULTIPLY)** — built: [UC-002](use-cases/UC-002-perform-exact-calculation.md), change add-exact-calculation
  - **Actor:** Client service
  - **Goal:** Get the exact Result of adding, subtracting or multiplying two Operands, or a clear rejection of an invalid or unknown request.
  - **Why here:** The core goal itself, for the Operations that need no rounding.
  - **Depends on:** B-1
  - **Original item:** 2 (`add-exact-arithmetic`)
- [ ] **B-3: Divide two Operands**
  - **Actor:** Client service
  - **Goal:** Get the Result of a division, rounded per BR-3, or a rejection when dividing by zero (BR-2).
  - **Why here:** Completes the four basic Operations promised by v1.
  - **Depends on:** B-2
  - **Original item:** 3 (`add-divide`)
- [ ] **B-4: Reject Operands outside the limits**
  - **Actor:** Client service
  - **Goal:** Have an out-of-range Operand (BR-1) or an oversized request rejected before any arithmetic.
  - **Why here:** Closes the resource-exhaustion hole before anyone else calls the API.
  - **Depends on:** B-2
  - **Original item:** 4 (`add-operand-limits`)
- [ ] **B-5: Explore the API from its documentation**
  - **Actor:** Developer
  - **Goal:** Learn every Operation, request shape, Result and rejection from interactive API documentation.
  - **Why here:** Completes v1 scope (PRD §3.3): developers can integrate without reading the code.
  - **Depends on:** B-3, B-4
  - **Original item:** 5 (`add-api-docs`)

## Increments (0/4 built)

- [ ] **B-6: Hide the API documentation in production**
  - **Actor:** Operator
  - **Goal:** Keep the API documentation off in any environment reachable beyond developers.
  - **Why here:** v1 follow-up: the documentation should not be public in production.
  - **Depends on:** B-5
  - **Original item:** 5a (`add-api-docs-toggle`)
- [ ] **B-7: Raise to a power and take a modulo**
  - **Actor:** Client service
  - **Goal:** Get the Result of POWER (BR-5, BR-6) and MODULO (BR-7), with their rejections.
  - **Why here:** The most-asked-for binary Operations after the basic four.
  - **Depends on:** B-4
  - **Original item:** 6 (`add-power-modulo`)
- [ ] **B-8: Perform a unary Calculation (SQRT, NEGATE)**
  - **Actor:** Client service
  - **Goal:** Get the Result of an Operation on a single Operand (BR-8, BR-9).
  - **Why here:** Introduces Arity without breaking existing clients.
  - **Depends on:** B-7
  - **Original item:** 7 (`add-unary-operations`)
- [ ] **B-9: Discover the supported Operations**
  - **Actor:** Client service
  - **Goal:** Learn every supported Operation and its Arity at runtime.
  - **Why here:** Clients can adapt to new Operations without a release of their own.
  - **Depends on:** B-8
  - **Original item:** 8 (`add-operations-discovery`)

## Later (0/3 built)

<!-- Each needs a PRD update first: it moves something out of the non-goals or roadmap. Not built in the original either. -->

- [ ] **B-10: Choose the precision of a Result**
  - **Actor:** Client service
  - **Goal:** Ask for a precision and rounding mode other than the BR-3 default.
  - **Why here:** Roadmap item; no client has asked for it yet.
  - **Depends on:** B-3
  - **Original item:** 9
- [ ] **B-11: Review past Calculations**
  - **Actor:** Client service
  - **Goal:** Retrieve Calculations performed earlier.
  - **Why here:** Ends statelessness; a large change with no current demand.
  - **Depends on:** B-2
  - **Original item:** 10
- [ ] **B-12: Call the API as an authenticated, rate-limited client**
  - **Actor:** Client service
  - **Goal:** Be identified and kept within a fair request rate.
  - **Why here:** Only needed once the API leaves internal use.
  - **Depends on:** B-2
  - **Original item:** 11
