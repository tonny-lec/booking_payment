---
description: Git commands auto-run policy
---
// turbo-all

# Git Commands Workflow

このワークフローは、Gitコマンドの自動実行ポリシーを定義します。
プロジェクトのGitフロールール（`docs/agent/rules.md`）に準拠しています。

---

## Git Flow（ブランチ運用ルール）


### 作業開始時（必須手順）
```bash
# 1. mainブランチに切り替え
git checkout main

# 2. mainブランチを最新化
git pull origin main

# 3. 作業用ブランチを作成
git checkout -b <branch-type>/<description>
```

### ブランチ命名規則
| タイプ | 用途 | 例 |
|--------|------|-----|
| `feature/` | 新機能・ドキュメント追加 | `feature/slice-a-docs` |
| `fix/` | バグ修正 | `fix/login-validation` |
| `refactor/` | リファクタリング | `refactor/payment-service` |
| `docs/` | ドキュメントのみの変更 | `docs/update-glossary` |
| `chore/` | 設定・ツール変更 | `chore/ci-config` |

### コミットメッセージ形式
```
<type>: <summary>

<body（任意）>

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```
- type: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

---

## 自動実行ポリシー

### ✅ 自動実行可能なコマンド（SafeToAutoRun: true）

#### 読み取り系
- `git status`
- `git log`
- `git diff`
- `git show`
- `git branch`
- `git remote -v`
- `git fetch origin`
- `git stash list`

#### ローカル操作
- `git add <files>`
- `git commit -m "<message>"`
- `git checkout <branch>`
- `git checkout -b <new-branch>`
- `git switch <branch>`
- `git merge <branch>` (ローカル)
- `git rebase <branch>` (ローカル)
- `git stash`
- `git stash pop`
- `git reset --soft`
- `git reset --mixed`
- `git pull origin main` (main最新化)
- `git pull origin <branch>` (作業ブランチ)

#### リモート操作（main以外）
- `git push -u origin <feature-branch>`
- `git push origin <docs-branch>`
- `git push origin <fix-branch>`
- `git push origin <refactor-branch>`
- `git push origin <chore-branch>`

#### GitHub CLI（gh コマンド）
- `gh pr create` - プルリクエスト作成
- `gh pr create --title "<title>" --body "<body>"`
- `gh pr list` - PR一覧表示
- `gh pr view <number>` - PR詳細表示
- `gh pr status` - PRステータス確認
- `gh pr checkout <number>` - PRをローカルにチェックアウト
- `gh pr comment <number>` - PRにコメント
- `gh pr edit <number>` - PR編集
- `gh issue list` - Issue一覧
- `gh issue view <number>` - Issue詳細
- `gh repo view` - リポジトリ情報
- `gh auth status` - 認証状態確認

### ❌ ユーザー承認必須（SafeToAutoRun: false）

#### mainブランチへの操作
- `git push origin main` - **禁止（PRを通じてマージ）**
- `git checkout main && git merge <branch>` - mainへのローカルマージ

#### 破壊的操作
- `git push --force` / `git push -f`
- `git push --force-with-lease`
- `git reset --hard`
- `git clean -fd`
- `git branch -D <branch>` (強制削除)
- `git rebase -i` (インタラクティブリベース)

---

## Must Not（禁止事項）

プロジェクトルール（`docs/agent/rules.md`）より：

- mainブランチへの直接push
- mainブランチ上での直接作業
- 1つのPRに複数の無関係なタスクを混ぜる

---

## 運用フロー

```
1. mainブランチから新規ブランチ作成 (自動実行可)
2. タスクを完了（ドキュメント/コード編集）
3. コミット＆プッシュ (自動実行可: feature/docs/fix等ブランチ)
4. PR作成
5. レビュー・修正
6. マージ後、次のタスクへ
```

### PRタイトル形式
```
<type>(<scope>): <summary>
```

| type | 用途 |
|------|------|
| `feat` | 新機能 |
| `fix` | バグ修正 |
| `docs` | ドキュメントのみ |
| `refactor` | リファクタリング |
| `test` | テスト追加・修正 |
| `chore` | 設定・ツール変更 |

例：
- `feat(iam): add RefreshToken entity`
- `fix(booking): resolve time range overlap detection`
- `docs: add IAM context design`

---

## PR作成ワークフロー

### 1. PR作成前の確認
```bash
# 変更内容の確認
git status
git diff --stat
git log main..HEAD --oneline
```

### 2. PR作成コマンド
```bash
gh pr create --title "<type>(<scope>): <summary>" --body "$(cat <<'EOF'
## Summary

- <変更内容1>
- <変更内容2>

## Changes

### New Files

| File | Description |
|------|-------------|
| `path/to/file` | Description |

### Key Implementation Details

<実装の詳細説明>

## Design Decisions

1. **決定事項** - 理由

## Test Coverage

- <テスト内容>

## Test plan

- [x] テスト項目1
- [x] テスト項目2

## Related

- Task: <タスクID>
- Spec: `docs/design/contexts/<context>.md` section X.X

---
Generated with [Claude Code](https://claude.ai/code)
EOF
)"
```

### 3. PR更新コマンド
```bash
# PR本文の更新
gh pr edit <PR番号> --body "$(cat <<'EOF'
<更新された本文>
EOF
)"

# PRタイトルの更新
gh pr edit <PR番号> --title "<新しいタイトル>"
```

### 4. 詳細度ガイドライン

| 変更規模 | Summary | Changes | Design Decisions |
|----------|---------|---------|------------------|
| 小（1-2ファイル） | 1-2行 | ファイル一覧のみ | 省略可 |
| 中（3-5ファイル） | 2-3行 | ファイル一覧＋概要 | 重要な判断のみ |
| 大（6ファイル以上） | 3行＋背景 | 詳細な実装説明＋コード例 | 全ての設計判断 |

### 5. コード実装PRの必須項目

コード（Java等）を含むPRでは、以下を必ず記載：

- **Key Implementation Details**
  - 主要クラス/インターフェースの説明
  - コードスニペット（インターフェース定義、主要メソッド等）

- **入出力例**（該当する場合）
  ```markdown
  | Type | Input | Output |
  |------|-------|--------|
  | IPv4 | `192.168.1.100` | `192.168.1.***` |
  ```

- **テスト詳細**
  - テストケース数
  - カバー範囲（正常系、異常系、境界値等）

---

## 参照ドキュメント

- `docs/agent/rules.md` - システムルール（Must/Must Not、Git Flow、PR作成ルール）
- `docs/templates/pr-template.md` - PRテンプレート詳細ガイド
- `.github/pull_request_template.md` - GitHubテンプレート
- `docs/tasks/implementation-slice-a.md` - タスク一覧・進捗管理
- `AGENTS.md` - エージェント向けガイドライン