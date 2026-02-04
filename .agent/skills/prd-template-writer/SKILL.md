---
name: prd-template-writer
description: Create or update PRDs using the repository template. Use when a change requires a PRD.
---

# PRD Template Writer

## Scope
- Produce PRDs aligned to the template and gating rules.

## Inputs
- Provide PRD id and status.
- Provide user story, scope, constraints, architecture, and acceptance criteria.

## Outputs
- Produce `docs/prd-<id>.md` following the template.

## Procedure
1. Copy `agents/templates/prd-template.md` into `docs/prd-<id>.md`.
2. Set `doc_type`, `id`, and `status` in front matter.
3. Fill all sections with concrete requirements and constraints.
4. Keep `status: proposed` until explicit approval is granted.

## Evaluation
- Define must-pass sections for the PRD.
- Verify the PRD follows the template structure.
- Score missing sections or invalid status as failures.

## Do Not
- Leave required sections empty.
- Mark a PRD as approved without approval.

## References
- `agents/templates/prd-template.md`
- `scripts/prd-gate.sh`
