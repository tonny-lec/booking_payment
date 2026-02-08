---
doc_type: "codex_intel"
id: "codex-strategy-org"
version: "0.1"
last_updated: "2026-02-08"
status: "draft"
---

# 01. Strategy and Organization

## Focus
- AI-native engineering team の運用モデル
- 役割分担（Builder/Reviewer/Approver）
- 権限境界と責任分界

## Key Decisions
- AGENTS.md を組織標準の永続コンテキストとして採用する。
- PLANS/ExecPlans を長時間タスクの標準手順にする。
- Skills をワークフローの部品として再利用し、Evalsで品質保証する。

## Auto Snapshot
<!-- AUTO-GENERATED:START -->
| Date | Type | Claim | Source | Confidence |
|---|---|---|---|---|
| 2026-02-08 | official | Custom instructions with AGENTS.md | [agents-md-guide](https://developers.openai.com/codex/guides/agents-md/) | high |
| 2026-02-08 | official | Building an AI-Native Engineering Team | [ai-native-engineering-team](https://developers.openai.com/codex/guides/build-ai-native-engineering-team/) | high |
| 2026-02-08 | official | How Codex ran OpenAI DevDay 2025 | [blog-codex-devday](https://developers.openai.com/blog/codex-at-devday/) | high |
| 2026-02-08 | official | Supercharging Codex with JetBrains MCP at Skyscanner | [blog-skyscanner-mcp](https://developers.openai.com/blog/skyscanner-codex-jetbrains-mcp/) | high |
| 2026-02-08 | official | Use Codex with the Agents SDK | [codex-agents-sdk](https://developers.openai.com/codex/guides/agents-sdk/) | high |
| 2026-02-08 | official | Agent Skills | [codex-create-skill](https://developers.openai.com/codex/skills/#create-a-skill) | high |
| 2026-02-08 | official | Using PLANS.md for multi-hour problem solving | [codex-exec-plans](https://developers.openai.com/cookbook/articles/codex_exec_plans/) | high |
| 2026-02-08 | official | Explore | [codex-explore](https://developers.openai.com/codex/explore/) | high |
| 2026-02-08 | official | Codex Prompting Guide | [codex-prompting-guide](https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide/) | high |
| 2026-02-08 | official | Agent Skills | [codex-skills](https://developers.openai.com/codex/skills/) | high |
| 2026-02-08 | official | How OpenAI uses Codex | [how-openai-uses-codex](https://cdn.openai.com/pdf/6a2631dc-783e-479b-b1a4-af0cfbd38630/how-openai-uses-codex.pdf) | high |
| 2026-02-05 | official | Introducing GPT-5.3-Codex (fetch degraded: http_403) | [introducing-gpt-5-3-codex](https://openai.com/index/introducing-gpt-5-3-codex/) | high |
| 2026-02-02 | official | Introducing the Codex app (fetch degraded: http_403) | [introducing-codex-app](https://openai.com/index/introducing-the-codex-app/) | high |
| 2025-10-06 | official | Codex now generally available (fetch degraded: http_403) | [codex-ga-announcement](https://openai.com/index/codex-now-generally-available/) | high |
| 2025-05-17 | external | ChatGPT Codex: The Missing Manual - Latent.Space | [latent-space-missing-manual](https://www.latent.space/p/codex) | medium |
### Source Matrix
| claim_id | status | url |
|---|---|---|
| `agents-md-guide#1` | ok | https://developers.openai.com/codex/guides/agents-md/ |
| `ai-native-engineering-team#2` | ok | https://developers.openai.com/codex/guides/build-ai-native-engineering-team/ |
| `blog-codex-devday#3` | ok | https://developers.openai.com/blog/codex-at-devday/ |
| `blog-skyscanner-mcp#6` | ok | https://developers.openai.com/blog/skyscanner-codex-jetbrains-mcp/ |
| `codex-agents-sdk#8` | ok | https://developers.openai.com/codex/guides/agents-sdk/ |
| `codex-create-skill#14` | ok | https://developers.openai.com/codex/skills/#create-a-skill |
| `codex-exec-plans#15` | ok | https://developers.openai.com/cookbook/articles/codex_exec_plans/ |
| `codex-explore#16` | ok | https://developers.openai.com/codex/explore/ |
| `codex-prompting-guide#19` | ok | https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide/ |
| `codex-skills#21` | ok | https://developers.openai.com/codex/skills/ |
| `how-openai-uses-codex#24` | ok | https://cdn.openai.com/pdf/6a2631dc-783e-479b-b1a4-af0cfbd38630/how-openai-uses-codex.pdf |
| `introducing-gpt-5-3-codex#27` | error | https://openai.com/index/introducing-gpt-5-3-codex/ |
| `introducing-codex-app#25` | error | https://openai.com/index/introducing-the-codex-app/ |
| `codex-ga-announcement#17` | error | https://openai.com/index/codex-now-generally-available/ |
| `latent-space-missing-manual#28` | ok | https://www.latent.space/p/codex |
<!-- AUTO-GENERATED:END -->

## Notes
- 推論を事実として扱わない。根拠URLを必須にする。
