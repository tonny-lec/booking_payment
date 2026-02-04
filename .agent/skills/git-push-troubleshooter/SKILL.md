---
name: git-push-troubleshooter
description: Diagnose and resolve Git push failures, especially SSH and WSL issues. Use when git push fails.
---

# Git Push Troubleshooter

## Scope
- Restore git push capability by addressing SSH and network issues.

## Inputs
- Provide the error message.
- Provide remote URL and environment details.

## Outputs
- Produce steps to restore push capability.

## Procedure
1. Identify the error type (DNS, SSH auth, agent). 
2. Verify SSH connectivity with `ssh -T git@github.com`.
3. If `ssh-add` fails, start agent and add key.
4. Confirm agent variables and process exist.
5. If DNS/network issues persist, retry later or switch to HTTPS if approved.

## Evaluation
- Define must-pass checks for successful push.
- Capture command trace and outcomes.
- Score remaining push failures as unresolved.

## Do Not
- Regenerate keys or change remotes without approval.

## References
- `agents/knowledge/git-push-failure.md`
