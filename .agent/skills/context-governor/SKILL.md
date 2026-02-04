# SKILL: context-governor

**Purpose**
- Enforce SSOT and reference order when making decisions.

**Trigger**
- Any decision that depends on policy, rules, or design sources.

**Inputs**
- Task scope and relevant domain area

**Outputs**
- Ordered list of references to consult
- Decision grounded in SSOT

**Procedure**
1. Identify the task domain (security, API, context design, etc.).
2. Load the highest-priority SSOT references for that domain.
3. Confirm any conflicts and state which source wins.
4. Proceed only with information from the chosen references.

**Do Not**
- Skip SSOT references or assume facts without checking.

**References**
- `agents/context-policy.md`
