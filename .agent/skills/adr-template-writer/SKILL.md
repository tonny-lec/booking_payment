# SKILL: adr-template-writer

**Purpose**
- Create ADRs using the repository template and conventions.

**Trigger**
- A design decision needs a recorded rationale.

**Inputs**
- ADR number
- Decision slug
- Context, decision, consequences, alternatives, evidence

**Outputs**
- New ADR markdown file following the template

**Procedure**
1. Create a new ADR using `bash scripts/new-adr.sh <number> <decision-slug>`.
2. Fill `Context`, `Decision`, `Consequences`, `Alternatives`, and `Evidence` sections.
3. Keep `status` as `proposed` until approved.

**Do Not**
- Skip the evidence section.

**References**
- `agents/templates/adr-template.md`
- `scripts/new-adr.sh`
