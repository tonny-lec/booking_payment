---
name: context-template-writer
description: Create or update context documents using the repository context template. Use when adding or revising context docs.
---

# Context Template Writer

## Scope
- Produce context documents aligned to the template.

## Inputs
- Provide context name, scope, and key references.

## Outputs
- Produce a context document using the template structure.

## Procedure
1. Copy `agents/templates/context-template.md` to the target file.
2. Fill all sections with concrete context details.
3. Keep references consistent with repository naming rules.

## Evaluation
- Define must-pass checks for required sections.
- Validate structure against the template.
- Record missing sections as failures in evals.

## Do Not
- Leave required sections empty.

## References
- `agents/templates/context-template.md`
