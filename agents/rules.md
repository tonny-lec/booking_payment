---
doc_type: "agent_rules"
id: "rules"
version: "0.4"
last_updated: "2026-01-22"
status: "stable"
---

# ルール（System FixのSSOT）

## 基本原則（Core Principles）

### Context is Currency
- コンテキストウィンドウは貴重な資源である
- 常に消費量を最小限に抑え、必要な情報のみをロードする
- 長文化したら `checkpoint.md` に要約し、以後はそれをSSOTとして進める

### Single Source of Truth
- すべての開発はPRD（製品要件ドキュメント）を「北極星（North Star）」として進める
- SSOTを無視してコードだけ変更しない

### System Evolution
- バグやエラーは単なる修正対象ではなく、「システム（ルール）」の欠陥である
- 修正後は必ずルールを更新し、再発防止策をシステムに組み込む

---

## Must（絶対）
1. **PRD-First**：承認済みPRD（`status: approved`）なしに実装（コード）変更しない
2. **Evidence-First**：提案/変更/レビューには必ず根拠（差分/ログ/計測/仕様）を付ける
3. **Small Changes**：変更は最小。分割して各分割ごとに検証
4. **Tests Gate**：テストが落ちる変更はReject（例外なし）
5. **No Secrets / No PII**：Secrets/PIIを出力・埋め込みしない（SSOT：`docs/security/pii-policy.md`）
6. **Git Flow**：ブランチ運用ルールを遵守する（下記参照）
7. **One Task, One PR**：細分化されたタスク単位でブランチを作成しPRを出す（下記参照）

## Must Not（禁止）
- SSOTを無視してコードだけ変更
- 推論を事実として断定（不確かなら推論と明記）
- 外部送信設定（webhook等）を追加（明示許可なし）
- mainブランチへの直接push
- mainブランチ上での直接作業
- ルールに反する依頼を黙って破る（抵触するルールと代替案を提示すること）

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

## One Task, One PR（タスク単位PR運用）

### 原則
- `docs/tasks/by-feature.md` で細分化されたタスク単位でPRを作成
- 1つのPRに複数の無関係なタスクを混ぜない
- 関連するタスクは同一PRにまとめてもよい（例：同一ファイルの複数セクション）

### OpenAPI仕様の粒度（例外ルール）
- **OpenAPI仕様はタスクID単位で1つのPRを作成**
- 例：`IAM-API-01`（POST /auth/login）で1PR、`IAM-API-02`（POST /auth/refresh）で別PR
- 理由：APIエンドポイントは個別にレビュー・検証が必要なため
- ブランチ命名：`docs/openapi-<context>-<endpoint>`
  - 例：`docs/openapi-iam-login`, `docs/openapi-booking-create`

### ブランチ命名
```
docs/<context>-<document>
```
例：
- `docs/iam-context` - IAMコンテキスト設計
- `docs/booking-update-usecase` - 予約変更ユースケース
- `docs/observability` - 観測性設計

### ワークフロー
```
1. mainブランチから新規ブランチ作成
2. タスクを完了（ドキュメント編集）
3. コミット＆プッシュ
4. PR作成
5. （必要に応じて）レビュー・修正
6. マージ後、次のタスクへ
```

### PRタイトル形式
```
docs: <タスクの要約>
```
例：
- `docs: add IAM context design`
- `docs: complete booking-update usecase`
- `docs: add POST /auth/login endpoint to OpenAPI` （OpenAPI）
- `docs: add POST /payments endpoint with idempotency` （OpenAPI）

### タスク参照
- PRの説明にタスクID（例：`IAM-CTX-01`）を記載
- 完了後は `docs/tasks/by-feature.md` のステータスを更新

---

## System Fix（更新先）
- 重要失敗は Postmortem を作り、**この rules.md を更新**してから修正する。
- 「人の注意力」に依存する対策で終わらせない
- ルール or テンプレ or 検証手順のどれかが更新されること
