# math-api (sdlc-pipeline rebuild)

> Demonstration project: a rebuild of [`math-api`](../math-api) from the same PRD, domain model,
> standards and backlog, this time delivered by the multi-agent
> [sdlc-pipeline](https://github.com/prule/sdlc-pipeline) plugin instead of a single-agent workflow.

A stateless REST service for exact decimal arithmetic. See [docs/PRD.md](docs/PRD.md).

## Comparing with the original

| | Original `math-api` | This rebuild |
|---|---|---|
| Workflow | One agent: `/opsx:propose` → ponytail plan review → apply → ponytail + `/code-review` → archive → retro | `/sdlc-pipeline:build-use-case`: architect → spec-reviewer → Gate 1 → junior-dev → qa → senior-dev → Gate 2 → archive → retro |
| Backlog | `docs/BACKLOG.md`, items 1–8 as OpenSpec prompts | [BACKLOG.md](BACKLOG.md), B-1–B-9 as use cases, each naming its original item |
| Docs at start | Grew from a PRD and domain model | Its final PRD, DOMAIN-MODEL and STANDARDS, change logs reset |
| Records | `openspec/changes/archive/*/retrospective.md`, `reports/sessions/` | `retrospectives/`, `reports/sessions/`, `openspec/changes/archive/` |

The original's starting point is commit `a8cf8cb`; its v1-complete point is `aea85a0` (item 5) and its
last item is `d57c1d6` (item 8).

## Building it

See [CLAUDE.md](CLAUDE.md) for the change workflow.
