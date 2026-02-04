# SKILL: tool-usage-guard

**Purpose**
- Enforce tool usage rules and write-scope constraints.

**Trigger**
- Before running commands or editing files.

**Inputs**
- Intended tool actions

**Outputs**
- Approved tool usage plan
- Warnings for unsafe operations

**Procedure**
1. Identify whether the action is read-only or mutating.
2. Confirm the target files are within write scope.
3. Prefer safe commands and avoid destructive operations.
4. Request approval when required by policy.

**Do Not**
- Run destructive commands or edit outside allowed roots.

**References**
- `agents/tool-contract.md`
