---
doc_type: "agent_policy"
id: "context-policy"
version: "0.3"
last_updated: "2026-01-22"
status: "stable"
---

# コンテキスト運用ポリシー（SSOT参照の最小化）

## 基本原則

**Context is Currency**: コンテキストウィンドウは貴重な資源である。
常に消費量を最小限に抑え、必要な情報のみをロードせよ。

---

## SSOT参照順

情報を探す際は、以下の順序で確認する：

1. `docs/agent/rules.md` - システムルール
2. `docs/agent/workflow.md` - ワークフロー手順
3. `docs/agent/self-check.md` - セルフチェックリスト
4. PRD（`docs/prd-*.md`） - 製品要件
5. `docs/domain/glossary.md` - ドメイン用語
6. `docs/api/openapi/*.yaml` - API契約
7. `docs/design/usecases/*.md` - ユースケース設計
8. `docs/design/contexts/*.md` - コンテキスト設計
9. `docs/plan/*.md` - 計画
10. `docs/test/*.md` - テスト
11. `checkpoint.md` - セッション状態

---

## タスク開始時の質問（必須）

タスクを開始する前に、以下を明確にすること：

### 参照領域の特定
- **Frontend / Backend / Auth / DB / Messaging / Observability / Security** のどれを参照する？

### 変更種別の特定
- **機能追加 / バグ修正 / 性能改善 / 互換性 / セキュリティ** のどれ？

### 検証方法の特定
- **unit / integration / contract / e2e / load** のどれ？

---

## モジュラールール（Overwhelm防止）

タスクの種類に応じて、特定の参照ルールのみを読み込む：

| 作業領域 | 参照ドキュメント |
|---------|-----------------|
| **API開発** | `docs/api/openapi/*.yaml`, `docs/design/usecases/*.md` |
| **認証・認可** | `docs/design/contexts/iam.md`, `docs/security/*.md` |
| **予約機能** | `docs/design/contexts/booking.md`, `docs/design/usecases/booking-*.md` |
| **決済機能** | `docs/design/contexts/payment.md`, `docs/design/usecases/payment-*.md` |
| **セキュリティ** | `docs/security/*.md`, `docs/agent/rules.md` |
| **テスト設計** | `docs/test/*.md`, `docs/design/usecases/*.md` |
| **観測性** | `docs/design/observability.md` |

---

## Context Reset

### いつリセットするか
- 計画フェーズから実装フェーズに移行するとき
- 長文化したとき（概ね会話が10往復を超えたら）
- 複数の無関係なタスクが混在し始めたとき

### リセット手順
1. `checkpoint.md` を更新して現在の状態を要約
2. 新しいセッションまたは新しい会話を開始
3. `checkpoint.md` と `Plan` のみを入力として続行

### セルフチェック（Reset）
- [ ] 今から参照するのは「PLAN」だけ、と宣言した
- [ ] 追加で参照するdocがあるなら列挙した
- [ ] `checkpoint.md` が最新状態に更新されている
