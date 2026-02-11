---
name: graphite-stacked-pr-workflow
description: Enforce Graphite stacked PR workflow on GitHub. Use when starting work, creating branches/commits, updating mid-stack PRs, and submitting PR stacks.
---

# Graphite Stacked PR Workflow

## Scope
- Standardize repository workflow on Graphite (`gt`) stacked PR operations.

## Inputs
- Task type (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`).
- Expected stack shape (single PR or multi-PR stack).

## Outputs
- A valid stacked branch/PR sequence created with Graphite.
- Compliant commit messages and submission commands.

## Procedure
1. Verify Graphite is initialized (`gt init` if needed).
2. Start from trunk and sync: `gt checkout main`, `gt sync`.
3. Implement changes for one logical slice.
4. Stage changes with `git add`.
5. Create branch+commit with `gt create --message "<type>: <summary>"`.
6. For additional slices, repeat on top of the current branch to form a stack.
7. Submit with `gt submit --no-interactive` (or stack submit if required by context).
8. If a lower branch changes after review, use `gt modify` and then re-submit.

## Evaluation
- Branch/PR operations use `gt` commands, not ad-hoc `git push`.
- Commit message format is `<type>: <summary>`.
- Stack remains restacked and submit-ready after updates.

## Do Not
- Push directly to `main`.
- Use `git commit` / `git push` as the primary flow for PR creation.
- Collapse unrelated tasks into one PR.

## References
- `agents/rules.md`
- `.agent/workflows/graphite-commands.md`
