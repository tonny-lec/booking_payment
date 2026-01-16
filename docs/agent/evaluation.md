---
doc_type: "agent_policy"
id: "evaluation"
version: "0.2"
last_updated: "2026-01-16"
status: "stable"
---

# エージェント評価（回帰）

## 指標（MVP）
- Evidence添付率：100%
- テスト合格率：提案変更後にCIが通る割合
- 変更失敗率：CIを落とす提案の割合
- 指摘採用率：採用された提案/指摘の割合

## 出力例の置き場
- `docs/agent/output/`
