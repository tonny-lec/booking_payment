---
doc_type: "plan"
id: "structured-plan"
version: "0.2"
last_updated: "2026-01-16"
status: "draft"
---

# Structured Plan（タスク単位：Context Independent）

## Constraint（Initialization準拠）
- 各タスクは必出：
  - 手順
  - 編集するファイルパス
  - 検証（テスト/再現/観測）
  - **Context Independent: YES**（会話履歴なしで実行可能）

---

## Gate 0：PRD承認（必須）
### Task G0-1：Platform PRDを埋める
- Context Independent: YES
- 編集：`docs/prd-platform.md`
- 検証：scope/acceptance/approval gate がある

### Task G0-2：人間承認→approvedへ
- Context Independent: YES
- 編集：`docs/prd-platform.md` の `status: approved`
- 検証：`scripts/prd-gate.sh` がPASS（実装変更を入れた場合）

---

## Phase 0：設計（DDD + 契約）
### Task P0-1：用語集をSSOT化
- Context Independent: YES
- 編集：`docs/domain/glossary.md`
- 検証：Booking/Payment/IAMの主要用語が定義済み

### Task P0-2：コンテキスト設計（6本）
- Context Independent: YES
- 編集：`docs/design/contexts/*.md`
- 検証：テンプレ見出し（集約/イベント/非機能）がある

### Task P0-3：ユースケース設計（10本）
- Context Independent: YES
- 編集：`docs/design/usecases/*.md`
- 検証：Evidence見出しがある（`scripts/evidence-lint.sh`）

### Task P0-4：OpenAPI（契約）を埋める
- Context Independent: YES
- 編集：`docs/api/openapi/iam.yaml`, `docs/api/openapi/booking.yaml`, `docs/api/openapi/payment.yaml`
- 検証：paths が空でない

### Task P0-5：テスト計画の具体化
- Context Independent: YES
- 編集：`docs/test/test-plan.md`
- 検証：境界/冪等/権限/互換性が含まれる
