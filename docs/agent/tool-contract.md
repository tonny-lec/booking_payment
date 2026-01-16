---
doc_type: "agent_policy"
id: "tool-contract"
version: "0.2"
last_updated: "2026-01-16"
status: "stable"
---

# ツール契約（権限とガードレール）

## 前提（SSOT）
- `docs/agent/rules.md` が最上位

## 書き込み可能
- `docs/`（設計/PRD/運用）
- `scripts/`（標準化）
- 将来 `src/`（ただしPRDがapprovedになってから）

## 禁止
- Secrets/PIIの生成・出力・埋め込み
- 外部送信設定の追加（明示許可なし）

## Evidence要件
- 変更には必ず Evidence（根拠）と検証手順を添付する。
