---
doc_type: "agent_workflow"
id: "workflow"
version: "0.2"
last_updated: "2026-01-16"
status: "stable"
---

# Agentic Workflow（5 Commandments準拠）

## 0) Initial Command
- `docs/agent/initial-command.md` を使用する。

## 1) PRD-First（North Star）
1. PRDを作る/更新する（`docs/prd-platform.md` 等）
2. 人間が確認し、`status: approved` に変更
3. 承認後に計画（Structured Plan）へ進む

## 2) Context Efficiency（Modular Thinking）
- 不要な知識を読み込まない
- タスク開始時に必ず宣言：
  - **「このタスクで参照すべきドメインルール（Frontend / Backend / Auth / DB）はどれ？」**
- 会話が長くなったら `checkpoint.md` に要約し、以後はそれをSSOTとして進める

## 3) Structured Planning（Measure Twice）
- 実装前に `docs/plan/structured-plan.md` を更新する
- 計画には必ず：
  - 手順
  - 編集するファイルパス
  - 各ステップの検証
  - **Context Independent: YES**（会話履歴なしで実行可能）

## 4) Commandify
- 2回以上繰り返す作業は `scripts/` にコマンド化

## 5) Reflexion（System Evolution）
- 事故/失敗は Postmortem → **rules更新（System Fix）** → 修正
