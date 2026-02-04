---
name: checkpoint-template-writer
description: Update checkpoint summaries using the repository checkpoint template. Use when summarizing long-running work.
---

# Checkpoint Template Writer

## Scope
- Produce checkpoint updates aligned to the template.

## Inputs
- Provide current task status, completed work, and next steps.

## Outputs
- Produce an updated `checkpoint.md` using the template structure.

## Procedure
1. Open `agents/templates/checkpoint-template.md`.
2. Update `checkpoint.md` with current status and next steps.
3. Keep references concise and actionable.

## Evaluation
- Define must-pass sections for checkpoint updates.
- Verify the summary is concise and complete.
- Score missing next steps as failures.

## Do Not
- Overwrite unrelated checkpoints without noting impact.

## References
- `agents/templates/checkpoint-template.md`
- `checkpoint.md`
