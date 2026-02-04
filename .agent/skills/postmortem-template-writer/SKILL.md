# SKILL: postmortem-template-writer

**Purpose**
- Produce a postmortem using the standard template.

**Trigger**
- An incident or major failure needs documented analysis.

**Inputs**
- Incident summary, timeline, root cause, corrective actions

**Outputs**
- Postmortem document with System Fix notes

**Procedure**
1. Use `agents/templates/postmortem-template.md` as the structure.
2. Fill overview, timeline, detection, root cause, response, prevention, learnings, actions.
3. Explicitly record which rules/workflows/templates were updated as System Fix.

**Do Not**
- Close an incident without a System Fix entry.

**References**
- `agents/templates/postmortem-template.md`
- `agents/incident-policy.md`
