# Repository Guidelines

## Project Structure & Module Organization
This repository is a documentation-first DDD template for a booking/payment platform. Most artifacts live under `docs/`:
- `docs/design/` for contexts and use cases.
- `docs/api/openapi/` for service contracts (YAML).
- `docs/security/`, `docs/test/`, `docs/plan/`, and `docs/runbook/` for operational guidance.
Project-wide references include `checkpoint.md` and the PRDs (`docs/prd-*.md`).

Agent-specific documents (rules, workflows, templates) live under `agents/`.

---

## 5 Core Principles (必ず守る)

| # | Principle | Description |
|---|-----------|-------------|
| 1 | **PRD-First** | Never implement code/infra changes without approved PRD. No PRD = experimental work only |
| 2 | **Modular Rules** | Keep global rules short; load detailed rules only when needed |
| 3 | **Commandify Everything** | If you do it twice, make it a reusable template/command |
| 4 | **Explore First** | Inspect the repo and resolve discoverable facts before asking questions |
| 5 | **System Evolution** | Bugs are not just fixes—update rules/templates to prevent recurrence |

---

## Development Workflow

```
Default: Explore -> Confirm if needed -> Edit/Analyze -> Validate -> Report
Extended: Prime -> PRD -> Plan -> Reset -> Implement -> Validate -> Report
```

**Full details**: `agents/workflow.md`

---

## Self-Check Before Completion

Before declaring any task complete:
- [ ] Matches PRD specification?
- [ ] No context pollution?
- [ ] Followed modular rules?
- [ ] Proposed rule updates for errors?
- [ ] Tests pass?

**Full checklists**: `agents/self-check.md`

---

## Build, Test, and Development Commands
This repo is template-mode (no `src/` yet). Use the provided scripts:
- `bash scripts/prd-gate.sh`: enforces PRD approval before code/infra changes.
- `bash scripts/evidence-lint.sh`: checks Evidence headings in use-case docs.
- `bash scripts/test-all.sh`: placeholder test runner.
- `bash scripts/new-adr.sh 0001 decision-slug`: creates a new ADR from template.
- `bash scripts/context-reset.sh`: refreshes `checkpoint.md` from template.

## Coding Style & Naming Conventions
- Follow existing Markdown conventions in `docs/` and reuse `agents/templates/*`.
- Use kebab-case for new document filenames (e.g., `docs/adr/0001-decision-slug.md`).
- OpenAPI specs are YAML under `docs/api/openapi/`.

## Testing Guidelines
- Current test runner is a template placeholder. If you add executable code, update `scripts/test-all.sh` and document how to run unit/integration tests in `docs/test/`.

## Commit & Pull Request Guidelines
- Commit message format: `<type>: <summary>` with optional body. Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- Standard flow is normal Git with `1 task = 1 branch = 1 PR`.
- Never push directly to `main`.
- PRs should reference the relevant PRD and include evidence for changes.

## Agent-Specific Instructions
- PRD-first is mandatory for code/infra changes: no such changes without a PRD in `status: approved`.
- `docs/prd-*.md` is treated as persistent context for Codex.
- Use `checkpoint.md` to summarize long-running work.
- Explore the repo before asking questions that can be answered locally.
- Create a new branch for each new task; do not work directly on `main`.

## Agent SSOT & Policies (Read First)
- System rules (Must/Must Not, standard flow, optional profiles): `agents/rules.md`.
- Workflow (7-phase development): `agents/workflow.md`.
- Self-check checklists: `agents/self-check.md`.
- Context policy & reference order: `agents/context-policy.md`.
- Tool contract & write scope: `agents/tool-contract.md`.
- Initial prompt to start new work: `agents/initial-command.md`.
- Evaluation metrics and outputs: `agents/evaluation.md`, `agents/output/`.

## Common References by Task
- Product requirements: `docs/prd-*.md`.
- Domain glossary: `docs/domain/glossary.md`.
- API contracts: `docs/api/openapi/*.yaml`.
- Use cases: `docs/design/usecases/*.md`.
- Context overviews: `docs/design/contexts/*.md`, `docs/design/overview.md`.
- Plans & checkpoints: `docs/plan/*.md`, `checkpoint.md`.
- Test guidance: `docs/test/*.md`.
- Security policies: `docs/security/*.md`.
