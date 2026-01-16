---
doc_type: "agent_rules"
id: "rules"
version: "0.3"
last_updated: "2026-01-16"
status: "stable"
---

# ルール（System FixのSSOT）

## Must（絶対）
1. **PRD-First**：承認済みPRD（`status: approved`）なしに実装（コード）変更しない
2. **Evidence-First**：提案/変更/レビューには必ず根拠（差分/ログ/計測/仕様）を付ける
3. **Small Changes**：変更は最小。分割して各分割ごとに検証
4. **Tests Gate**：テストが落ちる変更はReject（例外なし）
5. **No Secrets / No PII**：Secrets/PIIを出力・埋め込みしない（SSOT：`docs/security/pii-policy.md`）
6. **Git Flow**：ブランチ運用ルールを遵守する（下記参照）

## Must Not（禁止）
- SSOTを無視してコードだけ変更
- 推論を事実として断定（不確かなら推論と明記）
- 外部送信設定（webhook等）を追加（明示許可なし）
- mainブランチへの直接push
- mainブランチ上での直接作業

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

### コミットルール
- コミットメッセージは意図を明確に記述
- 1コミット = 1つの論理的変更
- コミットメッセージ形式：
  ```
  <type>: <summary>

  <body（任意）>

  Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
  ```
- type: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

### プッシュ・マージルール
1. 作業ブランチにプッシュ：`git push -u origin <branch-name>`
2. Pull Request を作成
3. レビュー後にmainへマージ（人間が実行）
4. **mainへの直接pushは禁止**

### 作業中断・再開時
```bash
# 中断前：変更をコミット or スタッシュ
git stash  # または git commit

# 再開時：mainを最新化してからリベース（必要に応じて）
git checkout main
git pull origin main
git checkout <作業ブランチ>
git rebase main  # コンフリクトがあれば解決
```

---

## System Fix（更新先）
- 重要失敗は Postmortem を作り、**この rules.md を更新**してから修正する。
