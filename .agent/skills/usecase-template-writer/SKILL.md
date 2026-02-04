---
name: usecase-template-writer
description: Create use-case documents using the repository template. Use when defining or updating use cases.
---

# Usecase Template Writer

## Scope
- Produce use-case docs aligned to the template.

## Inputs
- Provide use case id and bounded context.
- Provide related features/skills and domain details.

## Outputs
- Produce `docs/design/usecases/<usecase-id>.md` following the template.

## Procedure
1. Copy `agents/templates/usecase-template.md` to the target file.
2. Fill front matter (`id`, `bounded_context`, `related_features`, `related_skills`, `status`).
3. Complete all numbered sections with concrete details.
4. Ensure the Evidence section includes rationale or references.

## Evaluation
- Define must-pass sections including Evidence and Failure Modes.
- Verify the document follows the template structure.
- Score missing Evidence or Failure Modes as failures.

## Do Not
- Leave Evidence or Failure Modes empty.

## References
- `agents/templates/usecase-template.md`
- `scripts/evidence-lint.sh`
