# SKILL: usecase-template-writer

**Purpose**
- Create use-case documents using the repository template.

**Trigger**
- A new use case is defined or an existing one needs updating.

**Inputs**
- Use case id
- Bounded context
- Related features and skills
- Commands/queries/events
- Domain model and invariants
- API and persistence details
- Failure modes and recovery
- Observability and security needs
- Test strategy and evidence

**Outputs**
- `docs/design/usecases/<usecase-id>.md` following the template

**Procedure**
1. Copy `agents/templates/usecase-template.md` to the target file.
2. Fill front matter (`id`, `bounded_context`, `related_features`, `related_skills`, `status`).
3. Complete all numbered sections with concrete details.
4. Ensure the Evidence section includes rationale or references.

**Do Not**
- Leave Evidence or Failure Modes empty.

**References**
- `agents/templates/usecase-template.md`
- `scripts/evidence-lint.sh`
