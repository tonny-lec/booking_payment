# Git Push Failure Knowledge

Date: 2026-01-16
Context: Attempted to push branch `docs/add-agents-guidelines`.
Error: `ssh: Could not resolve hostname github.com: Temporary failure in name resolution`
Impact: Remote push failed; PR creation via `gh` was blocked.
Next steps: Retry after network/SSH resolution or switch remote to HTTPS if approved.

# WSL から SSH で GitHub / Git を使うときの `ssh-agent` トラブルシューティング知見

## まとめ（要点）
- `ssh -T git@github.com` で **「You've successfully authenticated, but GitHub does not provide shell access.」** が出るのは **正常**（認証成功の合図）。
- Git の `fetch` / `push` が通っているなら、**SSH 接続自体は問題なし**。
- `ssh-add -l` が **`Could not open a connection to your authentication agent.`** になる場合は、**ssh-agent が起動していない / 環境変数が設定されていない**可能性が高い。
- VS Code Remote（WSL）のターミナルは起動方式が特殊なことがあり、ログインシェルと違って **ssh-agent が自動で立ち上がらない**ことがある。

---

## 症状
- GitHub への SSH テストは成功：
  - `ssh -T git@github.com` → 認証成功メッセージ
- Git の疎通も成功：
  - `git fetch -v` → up to date
- ただし agent 関連が失敗：
  - `ssh-add -l` → `Could not open a connection to your authentication agent.`

---

## 調査で確認したこと（環境変数とプロセス）
以下のワンライナーで、ssh-agent の状態を確認した：

```bash
( echo "--- env"; env | egrep '^(SSH_|GIT_SSH|GIT_SSH_COMMAND)=' || true
  echo "--- agent vars"; echo "SSH_AUTH_SOCK=$SSH_AUTH_SOCK"; echo "SSH_AGENT_PID=$SSH_AGENT_PID"
  echo "--- shell"; echo "SHELL=$SHELL"; ps -p $$ -o pid,ppid,cmd
  echo "--- agent proc"; pgrep -a ssh-agent || echo "ssh-agent process not found"
)
```

確認結果（重要点）
- `SSH_AUTH_SOCK=`（空）
- `SSH_AGENT_PID=`（空）
- `ssh-agent process not found`

→ **ssh-agent が起動しておらず、agent 用の環境変数も未設定**であることが確定。

補足：シェルのコマンド行に `.vscode-server` が見えていたため、VS Code Remote（WSL）経由のシェルであることも分かった。

---

## 原因（Root Cause）
- GitHub への SSH 認証や `git fetch` は、**鍵ファイル（例：`~/.ssh/id_ed25519`）を直接使えば**通る。
- 一方 `ssh-add` は **ssh-agent に接続**する必要がある。
- 今回は VS Code Remote（WSL）で起動したシェルにおいて、
  - ssh-agent が起動していない
  - `SSH_AUTH_SOCK` / `SSH_AGENT_PID` が設定されていない
  ため、`ssh-add` が接続できず失敗していた。

---

## 解決方法（今回実施した手順）
### 1) ssh-agent を起動して環境変数を設定
```bash
eval "$(ssh-agent -s)"
```

### 2) 鍵を agent に登録
```bash
ssh-add ~/.ssh/id_ed25519
```

### 3) 登録確認
```bash
ssh-add -l
```

---

## 動作確認（検証）
- agent の環境変数が入っていること：
```bash
echo "SSH_AUTH_SOCK=$SSH_AUTH_SOCK"
echo "SSH_AGENT_PID=$SSH_AGENT_PID"
```

- ssh-agent プロセスが存在すること：
```bash
pgrep -a ssh-agent
```

- GitHub の鍵認証が成功すること（参考）：
```bash
ssh -vT git@github.com
```

---

## 注意点 / 補足
- `ssh -T git@github.com` の成功メッセージにある **「GitHub does not provide shell access」** はエラーではなく仕様。
- `~/.ssh/config` は `~/.ssh/` 直下の `config` が正しい配置。
- ssh-agent を毎回手動で起動するのが面倒なら、起動方法（bashrc で起動、systemd user、keychain 等）を環境に合わせて整備するのが次のステップ。
