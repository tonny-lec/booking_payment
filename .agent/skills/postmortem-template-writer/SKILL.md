---
name: postmortem-template-writer
description: Create postmortems using the repository template. Use after incidents that require a post-incident report.
---

# Postmortem Template Writer

## Scope
- Produce postmortems aligned to the template.

## Inputs
- Provide summary, timeline, impact, root cause, and action items.

## Outputs
- Produce a postmortem document using the template.

## Procedure
1. Open `agents/templates/postmortem-template.md`.
2. Fill Summary, Timeline, Impact, Root Cause, and Actions sections.
3. Record System Fix items for recurrence prevention.

## Evaluation
- Define must-pass sections and action items.
- Verify System Fix items are included.
- Score missing root cause or actions as failures.

## Do Not
- Close a postmortem without System Fix items.

## References
- `agents/templates/postmortem-template.md`
- `agents/incident-policy.md`
