---
doc_type: "codex_intel"
id: "codex-security-governance"
version: "0.1"
last_updated: "2026-02-08"
status: "draft"
---

# 03. Security and Governance

## Focus
- sandbox / network / approval policy の設計
- prompt injection 対策
- 監査可能性（trace, evidence）

## Guardrails
- デフォルトは最小権限（sandbox + network制限）を採用する。
- 未検証情報を実行指示として扱わない。
- 自動化は承認ポイントを明示し、監査ログを残す。

## Auto Snapshot
<!-- AUTO-GENERATED:START -->
| Date | Type | Claim | Source | Confidence |
|---|---|---|---|---|
| 2026-02-08 | official | Building an AI-Native Engineering Team | [ai-native-engineering-team](https://developers.openai.com/codex/guides/build-ai-native-engineering-team/) | high |
| 2026-02-08 | official | Automations | [codex-app-automations](https://developers.openai.com/codex/app/automations/) | high |
| 2026-02-08 | official | Config basics | [codex-config-basic](https://developers.openai.com/codex/config-basic/) | high |
| 2026-02-08 | official | Security | [codex-security](https://developers.openai.com/codex/security/) | high |
| 2026-02-08 | official | How OpenAI uses Codex | [how-openai-uses-codex](https://cdn.openai.com/pdf/6a2631dc-783e-479b-b1a4-af0cfbd38630/how-openai-uses-codex.pdf) | high |
### Source Matrix
| claim_id | status | url |
|---|---|---|
| `ai-native-engineering-team#2` | ok | https://developers.openai.com/codex/guides/build-ai-native-engineering-team/ |
| `codex-app-automations#9` | ok | https://developers.openai.com/codex/app/automations/ |
| `codex-config-basic#13` | ok | https://developers.openai.com/codex/config-basic/ |
| `codex-security#20` | ok | https://developers.openai.com/codex/security/ |
| `how-openai-uses-codex#24` | ok | https://cdn.openai.com/pdf/6a2631dc-783e-479b-b1a4-af0cfbd38630/how-openai-uses-codex.pdf |
<!-- AUTO-GENERATED:END -->

## Notes
- 例外権限は期限付きで運用する。
