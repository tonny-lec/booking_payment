# Repository Guidelines

## Project Structure & Module Organization
This repository is a documentation-first DDD template for a booking/payment platform. Most artifacts live under `docs/`:
- `docs/design/` for contexts and use cases.
- `docs/api/openapi/` for service contracts (YAML).
- `docs/security/`, `docs/test/`, `docs/plan/`, and `docs/runbook/` for operational guidance.
- `docs/templates/` for ADRs, PRDs, and other repeatable formats.
Project-wide references include `checkpoint.md` and the PRDs (`docs/prd-*.md`).
Primary agent SSOTs live under `docs/agent/`.

---

## 5 Core Principles (必ず守る)

| # | Principle | Description |
|---|-----------|-------------|
| 1 | **PRD-First** | Never implement without approved PRD. No PRD = experimental work only |
| 2 | **Modular Rules** | Keep global rules short; load detailed rules only when needed |
| 3 | **Commandify Everything** | If you do it twice, make it a reusable template/command |
| 4 | **Context Reset** | Separate planning from execution; carry only Plan to implementation |
| 5 | **System Evolution** | Bugs are not just fixes—update rules/templates to prevent recurrence |

---

## Development Workflow (7 Phases)

```
Phase 0: Prime      → Understand scope and dependencies
Phase 1: PRD        → Create/update requirements document
Phase 2: Plan       → Write implementation plan
Phase 3: Reset      → Clear context, carry only Plan forward
Phase 4: Implement  → Execute with small, reversible changes
Phase 5: Validate   → Test, lint, build, verify
Phase 6: Report     → Document changes and remaining work
```

**Full details**: `docs/agent/workflow.md`

---

## Self-Check Before Completion

Before declaring any task complete:
- [ ] Matches PRD specification?
- [ ] No context pollution?
- [ ] Followed modular rules?
- [ ] Proposed rule updates for errors?
- [ ] Tests pass?

**Full checklists**: `docs/agent/self-check.md`

---

## Build, Test, and Development Commands
This repo is template-mode (no `src/` yet). Use the provided scripts:
- `bash scripts/prd-gate.sh`: enforces PRD approval before code/infra changes.
- `bash scripts/evidence-lint.sh`: checks Evidence headings in use-case docs.
- `bash scripts/test-all.sh`: placeholder test runner.
- `bash scripts/new-adr.sh 0001 decision-slug`: creates a new ADR from template.
- `bash scripts/context-reset.sh`: refreshes `checkpoint.md` from template.

## Coding Style & Naming Conventions
- Follow existing Markdown conventions in `docs/` and reuse `docs/templates/*`.
- Use kebab-case for new document filenames (e.g., `docs/adr/0001-decision-slug.md`).
- OpenAPI specs are YAML under `docs/api/openapi/`.

## Testing Guidelines
- Current test runner is a template placeholder. If you add executable code, update `scripts/test-all.sh` and document how to run unit/integration tests in `docs/test/`.

## Commit & Pull Request Guidelines
- Commit message format: `<type>: <summary>` with optional body. Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- Use feature branches; never push directly to `main`. See `docs/agent/rules.md` for the full Git flow.
- Git flow details and branching rules: `docs/agent/rules.md`.
- PRs should reference the relevant PRD and include evidence for changes.

## Agent-Specific Instructions
- PRD-first is mandatory: no code/infra changes without a PRD in `status: approved`.
- `docs/prd-*.md` is treated as persistent context for Codex.
- Use `checkpoint.md` to summarize long-running work.
- Always review the Git flow rules before starting work: `docs/agent/rules.md`.
- Create a new branch for each new task; do not work directly on `main`.

## Agent SSOT & Policies (Read First)
- System rules (Must/Must Not, Git flow): `docs/agent/rules.md`.
- Workflow (7-phase development): `docs/agent/workflow.md`.
- Self-check checklists: `docs/agent/self-check.md`.
- Context policy & reference order: `docs/agent/context-policy.md`.
- Tool contract & write scope: `docs/agent/tool-contract.md`.
- Incident response (postmortem + rules update): `docs/agent/incident-policy.md`.
- Initial prompt to start new work: `docs/agent/initial-command.md`.
- Evaluation metrics and outputs: `docs/agent/evaluation.md`, `docs/agent/output/`.

## Common References by Task
- Product requirements: `docs/prd-*.md`.
- Domain glossary: `docs/domain/glossary.md`.
- API contracts: `docs/api/openapi/*.yaml`.
- Use cases: `docs/design/usecases/*.md`.
- Context overviews: `docs/design/contexts/*.md`, `docs/design/overview.md`.
- Plans & checkpoints: `docs/plan/*.md`, `checkpoint.md`.
- Test guidance: `docs/test/*.md`.
- Security policies: `docs/security/*.md`.
