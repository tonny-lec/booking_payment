---
name: completion-self-check
description: Run the repository completion checklist before declaring work done. Use before reporting completion.
---

# Completion Self Check

## Scope
- Verify required completion items before final reporting.

## Inputs
- Provide the PRD and plan reference.
- Provide test or validation results.

## Outputs
- Produce a completion checklist with pass/fail notes.

## Procedure
1. Confirm changes match the PRD specification.
2. Confirm no context pollution occurred.
3. Confirm modular rules were followed.
4. Propose rule updates for any errors.
5. Confirm tests and checks passed or note omissions.

## Evaluation
- Define must-pass checklist items.
- Capture artifact evidence (test logs, commands) where possible.
- Score any missing checklist items as failures.

## Do Not
- Declare completion without the checklist.

## References
- `agents/self-check.md`
