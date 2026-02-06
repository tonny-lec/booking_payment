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

## Decision Rules
- Fix immediately when runtime behavior can violate documented API responses.
- Prefer explicit contract updates over implicit assumptions.
- Keep status mapping semantically consistent:
  - `401`: invalid credentials / unauthenticated.
  - `403`: authenticated identity known but not allowed (e.g., `account_not_active`).
  - `423`: locked account/resource.
  - `429`: rate limited.

## Example (Applied Pattern)
- Review pointed out `account_not_active` could fall back to `403` while login OpenAPI only had `401/423/429`.
- Resolution:
  1. Add `403` to login OpenAPI responses.
  2. Add tests for `account_not_active -> 403`.
  3. Reply on review thread with mapping and commit reference.

## Evaluation
- Every blocking comment has explicit disposition (`fixed`/`not fixed` + reason).
- Contract and tests are aligned after fixes.
- Review thread contains a concrete reply with evidence.

## Do Not
- Reply “fixed” without commit/test evidence.
- Change API status behavior without updating OpenAPI and tests.
