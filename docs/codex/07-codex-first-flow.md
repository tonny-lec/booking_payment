---
doc_type: "codex_operating_model"
id: "codex-first-flow"
version: "0.1"
last_updated: "2026-03-07"
status: "draft"
---

# Codex-first Operating Flow

## Goal
- Codex が最も得意な `探索 -> 局所変更 -> 即検証 -> 簡潔な報告` を標準フローにする。
- 特定の Git ツールや重い儀式を必須にせず、どのプロジェクトでも移植できる運用にする。

## Default Flow
1. **Explore**: リポジトリと環境を見て、解ける不確定を潰す
2. **Confirm only if needed**: 仕様判断や優先順位だけ確認する
3. **Edit or analyze**: 小さく編集するか、レビュー/調査を返す
4. **Validate**: 変更に見合うテスト、lint、build、観測を行う
5. **Report**: 何を変えたか、どう確かめたか、残課題は何かを短く残す

## When To Use An Explicit Plan
- 複数ファイルにまたがる
- 長時間かかる
- 外部依存やデータ移行がある
- 仕様判断が多い
- 失敗コストが高い

それ以外は、明示的な Plan を毎回要求しない。

## Git And PR Policy
- 標準は `1 task = 1 branch = 1 PR`
- `main` で直接作業しない
- 通常 Git 以外の専用 PR ツールは前提にしない

## Task Classification
- `implement`
- `review`
- `debug`
- `explain`
- `research`

各タスクで、必要な文書だけを最初に読む。
