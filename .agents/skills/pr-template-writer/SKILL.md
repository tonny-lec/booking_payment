---
name: pr-template-writer
description: Draft PR descriptions using the repository PR template. Use when creating or updating PRs.
---

# PR Template Writer

## Scope
- Produce PR bodies aligned to the repository template.

## Inputs
- Provide summary bullets.
- Provide new/modified files list.
- Provide key implementation details.
- Provide design decisions if any.
- Provide test coverage and test plan.
- Provide related tasks/specs.

## Outputs
- Produce a completed PR body matching the template structure.

## Procedure
1. Open `agents/templates/pr-template.md` and mirror its sections.
2. Fill Summary with 1-3 verb-led bullets (what + why).
3. Fill Changes with New/Modified Files tables and key details.
4. Add Design Decisions when applicable.
5. Complete Test Coverage and Test plan with commands and results.
6. Add Related links to tasks/specs/PRDs.

## Evaluation
- Define must-pass sections (Summary, Test Coverage, Test plan, Related).
- Verify the PR body follows the template structure.
- Score missing required sections as failures.

## Do Not
- Omit Summary, Test Coverage, Test plan, or Related.

### Allowed command shapes
- OK: `bash -lc "cat <<'EOF' > /tmp/pr-body.md ... EOF"`
- OK: `cat <<'EOF' > /tmp/pr-body.md ... EOF`
- NOT OK: `<<EOF` (without quotes)
- NOT OK: redirecting to any path other than `/tmp/pr-body.md`
- NOT OK: mixing `>>`, `|`, `;`, `&&`, or `||` into the PR body generation step

## References
- `agents/templates/pr-template.md`
- `.github/pull_request_template.md`
- `.agent/workflows/git-commands.md`
