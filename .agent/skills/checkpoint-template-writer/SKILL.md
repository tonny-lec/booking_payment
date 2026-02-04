# SKILL: checkpoint-template-writer

**Purpose**
- Record a checkpoint summary using the repository template.

**Trigger**
- Context is large or a task needs a durable summary.

**Inputs**
- Background, key issues, SSOT links, decisions, open questions, next actions

**Outputs**
- Updated `checkpoint.md` or a new checkpoint doc based on the template

**Procedure**
1. Use the template structure from `agents/templates/checkpoint-template.md`.
2. Fill each section with concise, factual summaries.
3. Ensure `Next Actions` include file paths and verification steps.

**Do Not**
- Omit SSOT references or leave sections blank.

**References**
- `agents/templates/checkpoint-template.md`
- `scripts/context-reset.sh` (template refresh)
