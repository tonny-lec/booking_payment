---
doc_type: "prd"
id: "prd-platform"
version: "0.2"
last_updated: "2026-01-16"
status: "proposed"   # proposed | approved | archived
---

# PRD: 予約・決済基盤（Platform）

## 1. User Story
- 開発者として、予約と支払いを扱う基盤を作り、運用（SLO/観測性/障害対応）まで含めた実戦力を身につけたい。

## 2. Scope（In / Out）
### In
- IAM（認証/認可）・Booking・Payment・Notification・Audit・（任意で）Ledger
- 契約ファースト（OpenAPI）と互換性維持の方針
- Observability（logs/metrics/traces）と SLO/SLI
- timeout / retry / idempotency を設計に含める

### Out（初期）
- 実決済プロバイダ接続（スタブで代替）
- 自律デプロイ（人間承認なし）
- マルチリージョン本番運用

## 3. Tech Stack & Constraints（PRD内の制約）
- SSOT：`docs/tech-stack.md`
- 最低限の前提：
  - Java 21+ / Spring Boot / Spring Security
  - OpenTelemetry（相関IDで追えること）
  - PostgreSQL（推奨）
  - OpenAPI 3.0（契約ファースト）

## 4. Architecture & Data Flow（要旨）
- DDD（Bounded Context）+ Hexagonal（Ports/Adapters）
- 主なBC：IAM / Booking / Payment / Notification / Audit /（任意）Ledger
- API: OpenAPI → 実装 → 契約テスト（後続で強化）
- イベント（任意）：at-least-once + dedup（重複排除）を設計

## 5. 実装スライス（粒度：MVP → 拡張）
### Slice A（最小MVP：まず動く価値）
- `iam-login`（ログイン/リフレッシュの最小）
- `booking-create`（予約作成：衝突防止の入口）
- `payment-create`（支払い作成：冪等性の入口）
- Observability最低限（traceId相関、REDメトリクスの雛形）

### Slice B（E2E成立）
- `payment-capture` / `payment-refund`
- `notification-send`（イベントor同期でまず通知記録）
- `audit-record`（重要操作の追記）

### Slice C（互換性と運用の深掘り）
- v1/v2 併走と段階的廃止（`docs/api/migration/`）
- SLO/SLIとRunbook（`docs/runbook/`）
- Incident Lab（障害注入→postmortem）

### Slice D（イベント駆動 + Ledger）
- `ledger-project`（再構築可能なprojection）
- ordering/duplicate/再処理戦略

## 6. Acceptance Criteria（MVP）
- E2E（予約→支払い→通知→監査）がCIで緑（Slice B完了時）
- traceId で原因追跡できる（ログ/トレースがつながる）
- ユースケース設計書がテンプレ準拠で揃う（`docs/design/usecases/`）

## 7. リスク / 前提（推論は推論と明記）
- イベント基盤（Kafka等）は初期はスタブ/インメモリで代替してもよい（後続で差し替え）
- SLOの数値は初期は仮置きでよい（計測→見直し）

## 8. Approval Gate（重要）
- `status: approved` にしてから **実装（コード）変更**へ進む
- 変更ゲートは `scripts/prd-gate.sh` を使用
