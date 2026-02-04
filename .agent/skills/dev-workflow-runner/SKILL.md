# SKILL: dev-workflow-runner

**Purpose**
- Apply the 7-phase workflow consistently (Prime, PRD, Plan, Reset, Implement, Validate, Report).

**Trigger**
- New task starts or a phase change is requested.

**Inputs**
- Task request
- PRD and relevant docs

**Outputs**
- Clear phase-by-phase checklist
- Phase-appropriate actions and artifacts

**Procedure**
1. Prime: read the smallest set of docs needed to define scope and impact.
2. PRD: confirm requirements, non-goals, constraints, and acceptance criteria.
3. Plan: create an ordered plan with implementation and validation steps.
4. Reset: state that only the plan and required refs will be used.
5. Implement: make small, reversible changes aligned to the plan.
6. Validate: run tests or checks tied to acceptance criteria.
7. Report: summarize changes, verification, and remaining work.

**Do Not**
- Skip a phase when the task requires it.

**References**
- `agents/workflow.md`
