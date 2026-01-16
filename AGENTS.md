# Repository Guidelines

## Project Structure & Module Organization
This repository is a documentation-first DDD template for a booking/payment platform. Most artifacts live under `docs/`:
- `docs/design/` for contexts and use cases.
- `docs/api/openapi/` for service contracts (YAML).
- `docs/security/`, `docs/test/`, `docs/plan/`, and `docs/runbook/` for operational guidance.
- `docs/templates/` for ADRs, PRDs, and other repeatable formats.
Project-wide references include `spec_slo_booking_payment_ddd_v0.6.md`, `checkpoint.md`, and the PRDs (`docs/prd-*.md`).

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
- PRs should reference the relevant PRD and include evidence for changes.

## Agent-Specific Instructions
- PRD-first is mandatory: no code/infra changes without a PRD in `status: approved`.
- `docs/prd-*.md` is treated as persistent context for Codex.
- Use `checkpoint.md` to summarize long-running work.
