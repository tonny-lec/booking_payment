---
title: "DDD版：SLO駆動 予約・決済マイクロサービス基盤 仕様書"
version: "0.6"
last_updated: "2026-01-16"
language: "ja"
audience: ["human", "agent"]
design_methodology: ["DDD", "Hexagonal Architecture", "Contract-first API", "SLO/Observability-first", "Event-driven (optional)"]
ssot:
  rules: "docs/agent/rules.md"
  workflow: "docs/agent/workflow.md"
  platform_prd: "docs/prd-platform.md"
  devex_prd: "docs/prd-devex-ai.md"
  tech_stack: "docs/tech-stack.md"
  glossary: "docs/domain/glossary.md"
  openapi: "docs/api/openapi/"
  usecases: "docs/design/usecases/"
  contexts: "docs/design/contexts/"
  plan: "docs/plan/structured-plan.md"
  file_map: "docs/plan/file-map.md"
  test_plan: "docs/test/test-plan.md"
  checkpoint: "checkpoint.md"
---

# 0. 目的
(1) 予約・決済マイクロサービスを土台に、(2) インシデント訓練場、(3) 契約ファースト、(4) DevEx自動化（AI）、(5) イベント駆動を段階的に追加し、**2026年に求められるスキル50項目を最終的に網羅**する。

---

# 1. 運用プロトコル（Initialization準拠）
- PRD-First（承認ゲート）：`docs/agent/workflow.md`
- Context Efficiency（参照領域宣言 + 長文化時のcheckpoint）：`docs/agent/context-policy.md` / `checkpoint.md`
- Structured Planning（Context Independent計画）：`docs/plan/structured-plan.md`
- Commandify（反復作業のコマンド化）：`scripts/`
- Reflexion（System Fixの更新先）：`docs/agent/rules.md`

---

# 2. SSOT（参照優先順位）
1. `docs/agent/rules.md`
2. `docs/agent/workflow.md`
3. PRD: `docs/prd-platform.md`, `docs/prd-devex-ai.md`
4. 技術制約: `docs/tech-stack.md`
5. 用語: `docs/domain/glossary.md`
6. API契約: `docs/api/openapi/*.yaml`
7. ユースケース設計: `docs/design/usecases/*.md`
8. コンテキスト設計: `docs/design/contexts/*.md`
9. 計画: `docs/plan/*.md`
10. テスト: `docs/test/*.md`
11. 運用: `docs/runbook/*.md`, `docs/postmortem/*.md`
12. `checkpoint.md`

---

# 3. ドキュメント出力仕様（各機能の設計書はMarkdownで出力）
## 3.1 テンプレ（固定）
- `docs/templates/usecase-template.md`
- `docs/templates/context-template.md`
- `docs/templates/adr-template.md`
- `docs/templates/postmortem-template.md`
- `docs/templates/checkpoint-template.md`

## 3.2 機能→設計書→OpenAPI 対応表（網羅チェック）
| 機能（ユースケース） | Bounded Context | 設計書（Markdown） | OpenAPI |
|---|---|---|---|
| ログイン/リフレッシュ | IAM | `docs/design/usecases/iam-login.md` | `docs/api/openapi/iam.yaml` |
| 予約作成 | Booking | `docs/design/usecases/booking-create.md` | `docs/api/openapi/booking.yaml` |
| 予約変更 | Booking | `docs/design/usecases/booking-update.md` | `docs/api/openapi/booking.yaml` |
| 予約取消 | Booking | `docs/design/usecases/booking-cancel.md` | `docs/api/openapi/booking.yaml` |
| 支払い作成（冪等） | Payment | `docs/design/usecases/payment-create.md` | `docs/api/openapi/payment.yaml` |
| 支払い確定（capture） | Payment | `docs/design/usecases/payment-capture.md` | `docs/api/openapi/payment.yaml` |
| 返金 | Payment | `docs/design/usecases/payment-refund.md` | `docs/api/openapi/payment.yaml` |
| 通知送信（イベント受信） | Notification | `docs/design/usecases/notification-send.md` | `docs/api/openapi/notification.yaml` |
| 監査記録 | Audit | `docs/design/usecases/audit-record.md` | `docs/api/openapi/audit.yaml` |
| 台帳投影（projection） | Ledger | `docs/design/usecases/ledger-project.md` | （任意） |

---

# 4. CIゲート（雛形）
- `.github/workflows/agent-qa.yml` が `scripts/` を呼び出す。
