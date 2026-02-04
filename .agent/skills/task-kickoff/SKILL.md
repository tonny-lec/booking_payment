---
name: task-kickoff
description: Start new work using the repository initial command workflow. Use when a new task begins.
---

# Task Kickoff

## Scope
- Start tasks with the required initial command and context.

## Inputs
- Provide task request and any referenced PRDs.

## Outputs
- Produce the initial command response and next-step guidance.

## Procedure
1. Follow the instructions in `agents/initial-command.md`.
2. Identify required PRDs and gating status.
3. Establish scope and next actions.

## Evaluation
- Define must-pass kickoff steps.
- Capture initial response and verify required references are included.
- Score missing PRD gating as failures.

## Do Not
- Start implementation without required PRD status.

## References
- `agents/initial-command.md`
