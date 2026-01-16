---
doc_type: "agent_policy"
id: "context-policy"
version: "0.2"
last_updated: "2026-01-16"
status: "stable"
---

# コンテキスト運用ポリシー（SSOT参照の最小化）

## SSOT参照順
1. `docs/agent/rules.md`
2. `docs/agent/workflow.md`
3. PRD（`docs/prd-*.md`）
4. `docs/domain/glossary.md`
5. `docs/api/openapi/*.yaml`
6. `docs/design/usecases/*.md`
7. `docs/design/contexts/*.md`
8. `docs/plan/*.md`
9. `docs/test/*.md`
10. `checkpoint.md`

## タスク開始時の質問（必須）
- **Frontend / Backend / Auth / DB / Messaging / Observability / Security** のどれを参照する？
- 変更は **機能追加 / バグ修正 / 性能改善 / 互換性 / セキュリティ** のどれ？
- 検証は **unit / integration / contract / e2e / load** のどれ？

## Context Reset
- 長文化したら `checkpoint.md` を更新してから続行する。
