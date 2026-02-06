---
name: pr-review-feedback-handler
description: Triage PR review comments, decide fix/no-fix with evidence, apply minimal updates (spec/tests/code), and post a precise reply on the review thread.
---

# PR Review Feedback Handler

## Scope
- Handle review feedback on an open PR with clear fix criteria and thread replies.

## Inputs
- PR number.
- Review comments (inline + review body).
- Relevant contract/SSOT docs (PRD, OpenAPI, task doc).

## Outputs
- Decision per comment: `fix`, `won't fix`, or `needs clarification`.
- Minimal patch set (spec/tests/code as needed).
- Reply comment on the original review thread with evidence.

## Procedure
1. Collect review evidence:
   - `gh pr view <PR> --comments`
   - `gh api repos/<owner>/<repo>/pulls/<PR>/comments`
2. Classify each comment:
   - Contract mismatch (OpenAPI/PRD/task definition).
   - Behavior bug/regression.
   - Non-blocking style suggestion.
3. Verify against source lines and SSOT before editing.
4. Apply minimal fix:
   - If contract mismatch: update contract first (e.g., OpenAPI), then tests, then code if needed.
   - If behavior bug: update code and tests; keep API contract consistent.
5. Re-run targeted tests and repository gates.
6. Reply on the exact review thread with:
   - What changed.
   - Why the chosen status/behavior is correct.
   - Commit hash.
7. When requesting re-review after the reply, append `[@codex rebiew]` in the comment body.

## Decision Rules
- Fix immediately when at least one is true:
  - Runtime behavior can violate documented contract (API/schema/spec/ADR/PRD).
  - Existing tests become misleading because expected behavior is unclear or inconsistent.
  - A client-integration assumption can break due to undocumented behavior.
- Choose `won't fix` only when:
  - The current behavior is explicitly documented in SSOT and tested.
  - The review comment is preference-only and does not affect correctness, safety, or contract.
- Choose `needs clarification` when:
  - Multiple SSOT sources conflict.
  - Product/security policy implications are unclear.
- Update order for contract-related comments:
  1. Source-of-truth doc (spec/PRD/ADR/task contract)
  2. Tests
  3. Implementation (only if needed to match the decided contract)
- Always include decision rationale in the review-thread reply (one short paragraph + evidence).

## Example (Applied Pattern)
- Pattern A: Contract Mismatch
  1. Review flags that runtime behavior includes an outcome not listed in the public contract.
  2. Confirm behavior from source lines and compare with spec.
  3. Decide canonical behavior with SSOT.
  4. Update contract and tests first; then implementation if still mismatched.
  5. Reply with final mapping/behavior and commit hash.
- Pattern B: Behavior Bug/Regression
  1. Review flags an edge case where logic diverges from intended use-case.
  2. Reproduce with a focused test.
  3. Apply minimal code fix and keep surrounding behavior unchanged.
  4. Include regression test and reply with before/after behavior summary.

## Evaluation
- Every blocking comment has explicit disposition (`fixed`/`not fixed` + reason).
- Contract and tests are aligned after fixes.
- Review thread contains a concrete reply with evidence.

## Do Not
- Reply “fixed” without commit/test evidence.
- Change API status behavior without updating OpenAPI and tests.
