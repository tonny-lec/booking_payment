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
6. **Graphite Flow**：Graphite stacked PR運用ルールを遵守する（下記参照）
7. **One Task, One PR**：細分化されたタスク単位でブランチを作成しPRを出す（下記参照）

## Must Not（禁止）
- SSOTを無視してコードだけ変更
- 推論を事実として断定（不確かなら推論と明記）
- 外部送信設定（webhook等）を追加（明示許可なし）
- mainブランチへの直接push
- mainブランチ上での直接作業
- ルールに反する依頼を黙って破る（抵触するルールと代替案を提示すること）

---

## Graphite Flow（stacked PR運用ルール）

### 作業開始時（必須手順）
```bash
# 1. trunk(main)に切り替え
gt checkout main

# 2. trunkとopen stackを同期
gt sync
```

### ブランチ・コミット作成ルール
- 変更を実装した後に `git add` でステージする
- `gt create --message "<type>: <summary>"` で branch + commit を作成する
- type: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

### コミットルール
- コミットメッセージは意図を明確に記述
- 1コミット = 1つの論理的変更
- コミットメッセージ形式：
  ```
  <type>: <summary>

  <body（任意）>

  Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
  ```
- Graphite運用では `git commit` より `gt create` / `gt modify` を優先する

### プッシュ・マージルール
1. PRスタックを提出：`gt submit --no-interactive`
2. Pull Request を作成/更新（Graphite経由）
3. レビュー後にmainへマージ（人間が実行）
4. **mainへの直接pushは禁止**

### 作業中断・再開時
```bash
# 中断前：変更をコミット or スタッシュ
git stash  # または gt create / gt modify

# 再開時：trunkとstackを同期
gt checkout main
gt sync
gt checkout <作業ブランチ>
# 必要に応じて: gt restack
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
1. gt checkout main / gt sync
2. タスクを完了（ドキュメント編集）
3. git add -> gt create
4. gt submit --no-interactive
5. （必要に応じて）レビュー・修正
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
- `docs: add POST /auth/login endpoint to OpenAPI`

### タスク参照
- PRの説明にタスクID（例：`IAM-D-06`）を記載
- 完了後は `docs/tasks/implementation-slice-a.md` のステータスを更新

---

## PR作成ルール

### 必須セクション
PRを作成する際は、以下のセクションを必ず含めること：

1. **Summary**：変更内容の要約（1-3箇条書き）
2. **Changes**：変更ファイル一覧と実装詳細
3. **Test Coverage**：テスト内容
4. **Test plan**：検証チェックリスト
5. **Related**：関連タスクID・仕様へのリンク

### 詳細度ガイドライン

| 変更規模 | Summary | Changes | Design Decisions |
|----------|---------|---------|------------------|
| 小（1-2ファイル） | 1-2行 | ファイル一覧のみ | 省略可 |
| 中（3-5ファイル） | 2-3行 | ファイル一覧＋概要 | 重要な判断のみ |
| 大（6ファイル以上） | 3行＋背景 | 詳細な実装説明＋コード例 | 全ての設計判断 |

### コード実装PRの必須項目
コード（Java等）を含むPRでは、以下を必ず記載：
- **Key Implementation Details**：主要クラス/インターフェースの説明
- **コードスニペット**：重要な構造を示す（インターフェース定義、主要メソッド等）
- **入出力例**：該当する場合（マスキング処理、変換処理等）

### テンプレート参照
- `.github/pull_request_template.md` - GitHubテンプレート（自動適用）
- `docs/templates/pr-template.md` - 詳細ガイドライン

---

## System Fix（更新先）
- 重要失敗は Postmortem を作り、**この rules.md を更新**してから修正する。
- 「人の注意力」に依存する対策で終わらせない
- ルール or テンプレ or 検証手順のどれかが更新されること
