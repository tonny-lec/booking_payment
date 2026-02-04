---
name: context-governor
description: Apply the repository context policy and reference order. Use when loading or summarizing context for a task.
---

# Context Governor

## Scope
- Enforce context policy and reference order.

## Inputs
- Provide the task request and candidate references.

## Outputs
- Produce a minimal context set aligned to policy.

## Procedure
1. Load required PRDs and policy docs first.
2. Minimize context by reading only needed files.
3. Avoid deep reference chasing unless blocked.
4. Use `checkpoint.md` for long-running summaries.

## Evaluation
- Define success criteria for minimal yet sufficient context.
- Record which files were loaded and why.
- Score violations for loading unnecessary or disallowed context.

## Do Not
- Load bulk documents without a blocking reason.

## References
- `agents/context-policy.md`
- `checkpoint.md`
