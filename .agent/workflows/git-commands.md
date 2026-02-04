---
description: Git commands auto-run policy
---
// turbo-all

# Git Commands Workflow

このワークフローは、Gitコマンドの自動実行ポリシーを定義します。
プロジェクトのGitフロールール（`agents/rules.md`）に準拠しています。

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

プロジェクトルール（`agents/rules.md`）より：

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
docs: <タスクの要約>
```
例：
- `docs: add IAM context design`
- `docs: complete booking-update usecase`
- `docs: add POST /auth/login endpoint to OpenAPI`

---

## 参照ドキュメント

- `agents/rules.md` - システムルール（Must/Must Not、Git Flow）
- `docs/tasks/by-feature.md` - タスク一覧・進捗管理
- `AGENTS.md` - エージェント向けガイドライン