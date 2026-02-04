---
name: tool-usage-guard
description: Enforce tool usage rules and write scope constraints. Use when running commands, editing files, or choosing tools.
---

# Tool Usage Guard

## Scope
- Apply tool contract rules and write scope boundaries.

## Inputs
- Provide the intended command or file operation.

## Outputs
- Produce a compliant tool/command plan.

## Procedure
1. Check the tool contract for allowed operations.
2. Prefer `rg` for searches and non-interactive git commands.
3. Avoid destructive git commands unless explicitly approved.
4. Keep edits within writable roots.

## Evaluation
- Define must-pass checks for tool compliance.
- Capture command trace when evaluating via `codex exec --json`.
- Score any disallowed command usage as failures.

## Do Not
- Run destructive git operations without approval.

## References
- `agents/tool-contract.md`
- `.agent/workflows/git-commands.md`
