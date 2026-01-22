# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DDD + Contract-First template for a booking/payment platform. Currently documentation-only (no `src/` yet). The repository demonstrates DDD (Bounded Contexts, Aggregates, Domain Events), Hexagonal Architecture, and SLO/Observability-first design.

## Core Principles

1. **PRD-First (North Star)**: No implementation without approved PRD (`status: approved`)
2. **Context is Currency**: Minimize context window consumption; load only necessary information
3. **System Evolution**: Bugs are fuel for system improvement, not just fixes. Update rules after every incident

## Development Phases

Follow this 7-phase workflow for all development tasks:

| Phase | Name | Purpose |
|-------|------|---------|
| 0 | **Prime** | Understand scope, dependencies, and impact area |
| 1 | **PRD** | Create/update PRD with goals, non-goals, acceptance criteria |
| 2 | **Plan** | Create implementation plan with steps, files, and verification |
| 3 | **Context Reset** | Clear conversation history; carry only Plan to next phase |
| 4 | **Implement** | Execute plan with small, reversible changes |
| 5 | **Validate** | Run tests, lint, build, manual verification |
| 6 | **Report** | Document what changed, how it was verified, remaining tasks |

**Reference**: `docs/agent/workflow.md` for detailed phase instructions.

## Self-Check (Before Task Completion)

- [ ] Implementation matches PRD specification?
- [ ] No context pollution from old conversation history?
- [ ] Followed modular rules (API/UI/Security guidelines)?
- [ ] Proposed rule updates for any errors encountered?
- [ ] All tests pass?

**Reference**: `docs/agent/self-check.md` for phase-specific checklists.

## Commands

```bash
# PRD approval gate (required before code changes)
bash scripts/prd-gate.sh

# Lint evidence headings in use-case docs
bash scripts/evidence-lint.sh

# Run all tests (placeholder)
bash scripts/test-all.sh

# Create new ADR
bash scripts/new-adr.sh 0001 decision-slug

# Reset checkpoint from template
bash scripts/context-reset.sh
```

## Architecture

### Tech Stack (planned)
- Java 25 with Spring Boot
- PostgreSQL (primary data store)
- Redis (cache, rate limiting, distributed lock)
- Kafka (messaging, event-driven)
- OpenTelemetry for observability
- OpenAPI 3.0 for contracts
- Gradle for build
- Terraform for IaC

### DDD Bounded Contexts
- **IAM**: Authentication/authorization (User, AccessToken, RefreshToken)
- **Booking**: Reservation management (Booking, BookingStatus, TimeRange)
- **Payment**: Payment processing (Payment, PaymentStatus, IdempotencyKey)
- **Notification**: Event-driven notifications
- **Audit**: Audit logging
- **Ledger**: Financial projections

### Hexagonal Structure (per module)
```
src/main/java/.../<module>/
  domain/       # Core business logic
  application/  # Use cases
  adapter/in/   # Inbound adapters (REST controllers)
  adapter/out/  # Outbound adapters (DB, external APIs)
  config/       # Spring configuration
```

**Forbidden dependencies**: `domain → adapter`, `domain → spring`

## Key Rules (from docs/agent/rules.md)

### Must
1. **PRD-First**: No code changes without approved PRD (`status: approved`)
2. **Evidence-First**: All proposals/changes require evidence (diff/log/metrics/spec)
3. **Small Changes**: Minimal changes, verify each increment
4. **Tests Gate**: Failing tests = rejected changes
5. **No Secrets/PII**: Never output secrets or PII to logs, traces, or AI output
6. **Git Flow**: Always use feature branches, never push directly to main

### Must Not
- Ignore SSOT and change only code
- Assert inference as fact (mark uncertain items as inference)
- Add external webhooks without explicit permission
- Push directly to main or work on main branch

## Git Flow

```bash
# Start work
git checkout main
git pull origin main
git checkout -b <type>/<description>

# Branch types: feature/, fix/, refactor/, docs/, chore/
# Commit format: <type>: <summary>
# Types: feat, fix, docs, refactor, test, chore
```

## SSOT Reference Order

When seeking information, check in this order:
1. `docs/agent/rules.md` - System rules
2. `docs/agent/workflow.md` - Workflow procedures
3. `docs/prd-*.md` - Product requirements
4. `docs/domain/glossary.md` - Domain terminology
5. `docs/api/openapi/*.yaml` - API contracts
6. `docs/design/usecases/*.md` - Use case designs
7. `docs/design/contexts/*.md` - Context designs
8. `docs/plan/*.md` - Plans
9. `docs/test/*.md` - Test guidance
10. `checkpoint.md` - Session state

## Before Starting Work

Ask yourself:
- Which domain area? (Frontend / Backend / Auth / DB / Messaging / Observability / Security)
- What type of change? (feature / bugfix / performance / compatibility / security)
- What verification? (unit / integration / contract / e2e / load)

## Long Conversations

When context grows large, update `checkpoint.md` to summarize state and use it as SSOT going forward.
