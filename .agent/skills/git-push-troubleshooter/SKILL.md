# SKILL: git-push-troubleshooter

**Purpose**
- Diagnose and resolve Git push failures, especially SSH and WSL issues.

**Trigger**
- `git push` fails with SSH or network errors.

**Inputs**
- Error message
- Remote URL (`git remote -v`)
- Environment (WSL/VS Code Remote)

**Outputs**
- Steps to restore push capability

**Procedure**
1. Identify the error type:
   - DNS resolution failure (`could not resolve hostname`).
   - SSH authentication agent failure.
2. Verify SSH connectivity:
   - `ssh -T git@github.com` for auth check.
3. If `ssh-add` fails, start agent and add key:
   - `eval "$(ssh-agent -s)"`
   - `ssh-add ~/.ssh/id_ed25519`
4. Confirm agent variables and process exist.
5. If DNS/network issues persist, retry later or switch to HTTPS if approved.

**Do Not**
- Regenerate keys or change remotes without approval.

**References**
- `agents/knowledge/git-push-failure.md`
