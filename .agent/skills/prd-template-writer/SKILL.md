# SKILL: prd-template-writer

**Purpose**
- Create or update PRDs using the repository template.

**Trigger**
- A new feature or change requires a PRD.

**Inputs**
- PRD id
- Status (proposed or approved)
- User story
- In-scope / out-of-scope items
- Tech stack and constraints
- Architecture and data flow
- Acceptance criteria and test strategy

**Outputs**
- `docs/prd-<id>.md` following the template

**Procedure**
1. Copy `agents/templates/prd-template.md` into `docs/prd-<id>.md`.
2. Set `doc_type`, `id`, and `status` in the front matter.
3. Fill all sections with concrete requirements and constraints.
4. Keep `status: proposed` until explicit approval is granted.

**Do Not**
- Leave required sections empty.
- Mark a PRD as approved without approval.

**References**
- `agents/templates/prd-template.md`
- `scripts/prd-gate.sh`
