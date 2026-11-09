# UC-001: Check the service is available

**Primary actor:** Client service (an internal service, or the platform running Math API on its behalf)
**Secondary actors:** None
**Goal:** Learn whether Math API is up and ready to accept Calculations.
**Scope:** Math API (single context: stateless arithmetic, docs/PRD.md)
**Level:** User-goal
**Status:** Draft

## Preconditions
- Math API has been started.

## Postconditions
- **Success:** The Client service knows Math API is available and can send it Calculations.
- **Failure:** The Client service knows Math API is not available (or gets no answer) and does not send it Calculations. Nothing changes in either case; the check has no side effects.

## Main flow (basic course of events)
1. The Client service asks the system whether it is available.
2. The system checks that it is ready to accept Calculations (R-1).
3. The system answers that it is available, revealing nothing else about itself (R-2).

## Alternative & exception flows
- **2a. The system is running but not yet, or no longer, ready to accept Calculations:** the system answers that it is not available (R-2). The flow ends.
- **1a. The system is not running:** the Client service gets no answer and treats Math API as not available. The flow ends.

## Business rules
- **R-1:** "Available" means ready to accept Calculations. Math API is stateless and depends on nothing else, so once it has started it is available until it stops.
- **R-2:** The answer is only *available* or *not available*. It exposes no internal detail (components, versions, configuration) to the caller.
- **R-3:** Asking whether the system is available never changes anything, and anyone who may call Math API may ask (no authentication in v1, PRD §5).

## Non-goals
- Performing any Calculation or Operation (B-2 onward).
- Documenting the API (B-5).
- Monitoring, alerting or uptime history.
- Separate answers for "running" and "ready". Math API depends on nothing else, so the two are the same (R-1).

## Open questions (need a human decision before/at Gate 1)
- This is the first use case on an empty codebase, and its only business behaviour is the availability answer. Should this change also lay the foundations every later use case relies on: the build, the layered structure and its enforcement, formatting and CI? The original math-api did this in its walking skeleton (item 1). The recommended answer is yes.
