# DDD Agent Pack v0.5

生成日: 2026-01-16

このリポジトリは「予約・決済」基盤を題材に、**DDD + 契約ファースト + Observability/SLO + エージェント型開発**を行うためのテンプレです。

## 入口（まず読む）
1. 仕様書: `spec_slo_booking_payment_ddd_v0.6.md`
2. エージェント運用: `docs/agent/workflow.md`
3. ルールSSOT: `docs/agent/rules.md`
4. PRD（承認ゲートあり）:
   - Platform: `docs/prd-platform.md`
   - DevEx AI: `docs/prd-devex-ai.md`
5. 長文化したら: `checkpoint.md`

## 標準コマンド（Commandify）
- PRD承認ゲート: `bash scripts/prd-gate.sh`
- Evidence lint: `bash scripts/evidence-lint.sh`
- テスト一括: `bash scripts/test-all.sh`
- ADR作成: `bash scripts/new-adr.sh 0001 decision-slug`
- Context Reset（checkpoint再生成）: `bash scripts/context-reset.sh`
