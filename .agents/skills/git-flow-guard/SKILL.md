---
name: git-flow-guard
description: Enforce repository git flow, branch naming, and PR rules. Use when starting work, creating branches, committing, or opening PRs.
---

# Git Flow Guard

## Scope
- Enforce branch naming, commit format, and PR rules.

## Inputs
- Provide task scope and type (docs, feat, fix, refactor, test, chore).

## Outputs
- Create a correct branch name.
- Produce a compliant commit message.
- Produce a PR checklist aligned to repo rules.

## Procedure
1. Start from `main` and pull latest changes.
2. Create a feature branch with the correct prefix.
3. Use commit format `<type>: <summary>`.
4. Ensure direct pushes to `main` never occur.
5. Ensure PR content follows required template sections.

## Evaluation
- Define success criteria for branch, commit, and PR compliance.
- Capture git command trace if evaluating via `codex exec --json`.
- Score for violations (wrong branch, wrong commit format, missing PR sections).

## Do Not
- Work directly on `main`.
- Push directly to `main`.

## References
- `agents/rules.md`
- `.agent/workflows/git-commands.md`
