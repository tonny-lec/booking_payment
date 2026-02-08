---
doc_type: "codex_intel"
id: "codex-operations-playbook"
version: "0.1"
last_updated: "2026-02-08"
status: "draft"
---

# 02. Operations Playbook

## Focus
- 日常運用（IDE/CLI/GitHub）
- 並列実行（worktrees, automations）
- レビュー運用（/review, PR review）

## Standard Loop
1. AGENTS.md とタスク入力を固定する。
2. 必要に応じて PLANS を作成する。
3. CLI/App で実行し、レビューを回す。
4. 変更をPRで検証し、証跡を残す。

## Auto Snapshot
<!-- AUTO-GENERATED:START -->
| Date | Type | Claim | Source | Confidence |
|---|---|---|---|---|
| 2026-02-08 | official | Custom instructions with AGENTS.md | [agents-md-guide](https://developers.openai.com/codex/guides/agents-md/) | high |
| 2026-02-08 | official | Testing Agent Skills Systematically with Evals | [blog-eval-skills](https://developers.openai.com/blog/eval-skills/) | high |
| 2026-02-08 | external | OpenAI’s Codex App for macOS: Setup, Features & First Project \| Codecademy | [codecademy-codex-app](https://www.codecademy.com/article/open-ai-codex-app-for-mac-os) | medium |
| 2026-02-08 | official | Use Codex with the Agents SDK | [codex-agents-sdk](https://developers.openai.com/codex/guides/agents-sdk/) | high |
| 2026-02-08 | official | Automations | [codex-app-automations](https://developers.openai.com/codex/app/automations/) | high |
| 2026-02-08 | official | Local environments | [codex-app-local-environments](https://developers.openai.com/codex/app/local-environments/) | high |
| 2026-02-08 | official | Worktrees | [codex-app-worktrees](https://developers.openai.com/codex/app/worktrees/) | high |
| 2026-02-08 | official | Codex CLI features | [codex-cli-features](https://developers.openai.com/codex/cli/features/) | high |
| 2026-02-08 | official | Config basics | [codex-config-basic](https://developers.openai.com/codex/config-basic/) | high |
| 2026-02-08 | official | Agent Skills | [codex-create-skill](https://developers.openai.com/codex/skills/#create-a-skill) | high |
| 2026-02-08 | official | Using PLANS.md for multi-hour problem solving | [codex-exec-plans](https://developers.openai.com/cookbook/articles/codex_exec_plans/) | high |
| 2026-02-08 | official | Explore | [codex-explore](https://developers.openai.com/codex/explore/) | high |
| 2026-02-08 | official | Use Codex in GitHub | [codex-github-integration](https://developers.openai.com/codex/integrations/github/) | high |
| 2026-02-08 | official | Codex Prompting Guide | [codex-prompting-guide](https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide/) | high |
| 2026-02-08 | official | Agent Skills | [codex-skills](https://developers.openai.com/codex/skills/) | high |
| 2026-02-08 | official | Workflows | [codex-workflows](https://developers.openai.com/codex/workflows/) | high |
| 2026-02-08 | official | How OpenAI uses Codex | [how-openai-uses-codex](https://cdn.openai.com/pdf/6a2631dc-783e-479b-b1a4-af0cfbd38630/how-openai-uses-codex.pdf) | high |
| 2026-02-08 | official | OpenAI Cookbook | [openai-cookbook-home](https://cookbook.openai.com/) | high |
| 2026-02-02 | official | Introducing the Codex app (fetch degraded: http_403) | [introducing-codex-app](https://openai.com/index/introducing-the-codex-app/) | high |
| 2026-02-02 | external | Best Practices for using Codex - Codex - OpenAI Developer Community | [openai-community-best-practices](https://community.openai.com/t/best-practices-for-using-codex/1373143) | medium |
| 2025-12-04 | external | OpenAI の Codex「Plans.md (唯一の生きた文書) 戦略」 をVSCode GitHub Copilot で実行する方法 #GitHubCopilot - Qiita | [qiita-plans-practice](https://qiita.com/masakinihirota/items/62367ca7ab1766cbd012) | medium |
| 2025-09-18 | external | Codex CLIなどで設計書ベースでAgenticな実装を支援するAGENTS.mdを作成しました | [zenn-agents-practice](https://zenn.dev/shinpr_p/articles/80cef7ed8421a8) | medium |
### Source Matrix
| claim_id | status | url |
|---|---|---|
| `agents-md-guide#1` | ok | https://developers.openai.com/codex/guides/agents-md/ |
| `blog-eval-skills#5` | ok | https://developers.openai.com/blog/eval-skills/ |
| `codecademy-codex-app#7` | ok | https://www.codecademy.com/article/open-ai-codex-app-for-mac-os |
| `codex-agents-sdk#8` | ok | https://developers.openai.com/codex/guides/agents-sdk/ |
| `codex-app-automations#9` | ok | https://developers.openai.com/codex/app/automations/ |
| `codex-app-local-environments#10` | ok | https://developers.openai.com/codex/app/local-environments/ |
| `codex-app-worktrees#11` | ok | https://developers.openai.com/codex/app/worktrees/ |
| `codex-cli-features#12` | ok | https://developers.openai.com/codex/cli/features/ |
| `codex-config-basic#13` | ok | https://developers.openai.com/codex/config-basic/ |
| `codex-create-skill#14` | ok | https://developers.openai.com/codex/skills/#create-a-skill |
| `codex-exec-plans#15` | ok | https://developers.openai.com/cookbook/articles/codex_exec_plans/ |
| `codex-explore#16` | ok | https://developers.openai.com/codex/explore/ |
| `codex-github-integration#18` | ok | https://developers.openai.com/codex/integrations/github/ |
| `codex-prompting-guide#19` | ok | https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide/ |
| `codex-skills#21` | ok | https://developers.openai.com/codex/skills/ |
| `codex-workflows#23` | ok | https://developers.openai.com/codex/workflows/ |
| `how-openai-uses-codex#24` | ok | https://cdn.openai.com/pdf/6a2631dc-783e-479b-b1a4-af0cfbd38630/how-openai-uses-codex.pdf |
| `openai-cookbook-home#30` | ok | https://cookbook.openai.com/ |
| `introducing-codex-app#25` | error | https://openai.com/index/introducing-the-codex-app/ |
| `openai-community-best-practices#29` | ok | https://community.openai.com/t/best-practices-for-using-codex/1373143 |
| `qiita-plans-practice#31` | ok | https://qiita.com/masakinihirota/items/62367ca7ab1766cbd012 |
| `zenn-agents-practice#32` | ok | https://zenn.dev/shinpr_p/articles/80cef7ed8421a8 |
<!-- AUTO-GENERATED:END -->

## Notes
- 並列化は worktree 境界で行い、衝突を避ける。
