---
name: adr-template-writer
description: Create ADRs using the repository ADR template and numbering rules. Use when a design decision needs a recorded rationale.
---

# ADR Template Writer

## Scope
- Produce ADRs aligned to the template and numbering rules.

## Inputs
- Provide ADR number and decision slug.
- Provide context, decision, consequences, alternatives, and evidence.

## Outputs
- Produce a new ADR markdown file following the template.

## Procedure
1. Run `bash scripts/new-adr.sh <number> <decision-slug>`.
2. Fill Context, Decision, Consequences, Alternatives, and Evidence sections.
3. Keep `status` as `proposed` until approved.

## Evaluation
- Define must-pass checks for required sections.
- Verify the ADR file follows the template structure.
- Score missing Evidence or status errors as failures.

## Do Not
- Skip the Evidence section.

## References
- `agents/templates/adr-template.md`
- `scripts/new-adr.sh`
