---
name: dev-workflow-runner
description: Apply the 7-phase workflow (Prime, PRD, Plan, Reset, Implement, Validate, Report). Use when starting a task or switching phases.
---

# Dev Workflow Runner

## Scope
- Apply the 7-phase workflow consistently.

## Inputs
- Provide the task request.
- Provide relevant PRDs and docs.

## Outputs
- Produce a phase-by-phase checklist.
- Produce phase-appropriate actions and artifacts.

## Procedure
1. Prime: read the smallest set of docs needed to define scope and impact.
2. PRD: confirm requirements, non-goals, constraints, and acceptance criteria.
3. Plan: write an ordered plan with implementation and validation steps.
4. Reset: state that only the plan and required refs will be used.
5. Implement: make small, reversible changes aligned to the plan.
6. Validate: run checks tied to acceptance criteria.
7. Report: summarize changes, verification, and remaining work.

## Evaluation
- Define must-pass success criteria (outcome, process, style, efficiency) before running.
- Capture trace and artifacts when executing via `codex exec --json` if available.
- Score results against criteria and add failures as new eval prompts.

## Do Not
- Skip a phase when the task requires it.

## References
- `agents/workflow.md`
