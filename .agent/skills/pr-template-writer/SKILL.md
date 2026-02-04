# SKILL: pr-template-writer

**Purpose**
- Draft PR descriptions using the repository template and guidelines.

**Trigger**
- Creating or updating a PR.

**Inputs**
- Summary bullets
- New/modified files list
- Key implementation details
- Design decisions (if any)
- Test coverage and test plan
- Related tasks/specs

**Outputs**
- Completed PR body matching the template structure

**Procedure**
1. Open `agents/templates/pr-template.md` and mirror its sections.
2. Fill Summary with 1-3 verb-led bullets (what + why).
3. Fill Changes with New/Modified Files tables and key implementation details.
4. Add Design Decisions when applicable.
5. Complete Test Coverage and Test plan with commands and results.
6. Add Related links to tasks/specs/PRDs.

**Do Not**
- Omit Summary, Test Coverage, Test plan, or Related.

**References**
- `agents/templates/pr-template.md`
- `.github/pull_request_template.md`
- `.agent/workflows/git-commands.md`
