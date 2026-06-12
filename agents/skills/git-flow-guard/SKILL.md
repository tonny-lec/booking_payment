---
name: git-flow-guard
description: Enforce this repository's Git and PR workflow before branch creation, commits, pushes, PR creation, PR updates, or merge-related work. Use whenever Codex is about to run git or gh commands that change repository or GitHub state, and prefer this repository flow over generic GitHub/plugin publishing defaults.
---

# Git Flow Guard

## Overview

Use this skill to keep Git operations aligned with the repository-owned workflow. The canonical sources are `AGENTS.md`, `agents/rules.md`, `agents/templates/pr-template.md`, and `scripts/git-flow.sh`.

## Required Reads

Before changing branch, commit, push, or PR state, read the smallest relevant set:

- `AGENTS.md` for high-level repository policy.
- `agents/rules.md` section `Standard Git PR Flow` for the repository Git policy.
- `scripts/git-flow.sh` before starting or syncing task branches.
- `agents/templates/pr-template.md` before creating or updating a PR body.

## Workflow

1. Inspect scope with `git status --short --branch` and a relevant diff or diff stat.
2. Keep `main` read-only. For a new task, use a dedicated branch; prefer `bash scripts/git-flow.sh start <branch-name>` when starting from clean state.
3. Do not mix unrelated changes. Stage explicit paths unless the whole worktree is confirmed to belong to the task.
4. Commit with the repository format: `<type>: <summary>`.
5. Push the task branch, never push directly to `main`.
6. Create one PR per task. The PR should be ready for review unless the user explicitly asks for draft or the work is intentionally incomplete.
7. Use the repository PR template and include summary, evidence, validation, and related specs or tasks.
8. Leave merging to a human.

## Guardrails

- Repository Git flow overrides generic GitHub/plugin publishing defaults.
- Do not create draft PRs by default.
- Do not run destructive Git commands such as `git reset --hard` without explicit user approval.
- Do not silently revert or overwrite user changes.
- For code or infrastructure changes, confirm an approved PRD before implementation and run `bash scripts/prd-gate.sh` as part of validation.

## Completion Check

Before reporting completion, run `git status --short --branch` and, when a commit was made, `git log -1 --oneline`. For PR work, confirm the PR URL, base branch, head branch, draft state, and validation performed.
