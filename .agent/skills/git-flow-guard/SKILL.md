# SKILL: git-flow-guard

**Purpose**
- Enforce repository git flow, branch naming, and PR rules.

**Trigger**
- Any request to start work, create a branch, or open a PR.

**Inputs**
- Task scope and type (docs, feature, fix, refactor, chore)

**Outputs**
- Correct branch name
- Correct commit message format
- PR checklist with required sections

**Procedure**
1. Ensure work starts from `main` and `git pull origin main` is run.
2. Create a feature branch with the correct prefix.
3. Use commit message format `<type>: <summary>`.
4. Verify main is never pushed directly.
5. Ensure PR includes required sections and task IDs when applicable.

**Do Not**
- Push directly to `main` or work on `main`.

**References**
- `agents/rules.md`
