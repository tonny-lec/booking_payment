---
doc_type: "codex_template_evaluation"
id: "codex-template-evaluation-shiteki-a"
version: "0.1"
last_updated: "2026-03-07"
status: "draft"
trigger: "指摘A"
---

# Codex Template Evaluation: 指摘A

## Purpose
- この文書は、このリポジトリのテンプレートが Codex をどこまで活用できているかを評価した記録である。
- 今後この評価について相談する際の会話トリガーは `指摘A` とする。

## Conclusion
- このテンプレートは、ドキュメント駆動で事故を減らす設計としては強い。
- 一方で、汎用的な Codex 活用テンプレートとしては、Codex の標準的な探索・実装フローよりも、このリポジトリ固有の運用前提が強い。
- 評価としては、`この repo 専用テンプレート` としては高品質、`どんなプロジェクトでも使える Codex テンプレート` としては改善余地が大きい。

## Codex と相性が良いルール
- `Context is Currency` は、読み過ぎを防ぎ、必要最小限の文脈に絞る方針として有効。
- `Evidence-First` は、提案や変更に根拠を要求するため、Codex の作業を検証可能にする。
- `Small Changes` と `Tests Gate` は、差分を小さく保ち、失敗を早期に見つける運用として再現性が高い。
- SSOT 参照順とモジュラールールは、探索範囲を限定しやすく、ドキュメント量が多いリポジトリで有効。
- `docs/tasks/by-feature.md` のような細かなタスク分解は、エージェントが作業単位を認識しやすい。

## Codex の強みを活かし切れていないルール
- `Graphite Flow` が必須ルールに入っており、Codex 活用そのものではなく、特定の Git 運用への依存が強い。
- `タスク開始時の質問（必須）` は、Codex が先に探索して事実を集める挙動と衝突しやすい。
- `Plan` と `Context Reset` が常に強く要求されており、短い修正やレビューでも初動コストが高くなりやすい。
- 初動コマンドが `PRD を起点にする` 形に固定されており、レビュー、調査、説明、デバッグの入口としては狭い。
- PR ルールの記載粒度が高く、Codex の簡潔な出力方針よりも、手作業の詳細記述を強く求めている。
- コミットメッセージ例に特定製品名が含まれており、汎用テンプレートとしては不適切。

## 再現性が高いルール
- `PRD-First` を `コード/インフラ変更` に限定して適用すること。
- `Evidence-First` を全変更の共通原則にすること。
- `Small Changes` を維持し、1回の変更で扱う論点を絞ること。
- `No Secrets / No PII` を常に明文化すること。
- `必要最小限の参照だけ読む` という原則を維持すること。
- タスクを ID 付きで分解し、作業単位を固定すること。
- 失敗時に、ルール、テンプレート、検証手順のいずれかへ必ず還元すること。

## 改善の方向
- ルールを `Core` と `Project Profile` に分離する。
- `Core` には、Codex 共通の原則だけを残す。
- `Project Profile` には、Graphite、PRD 厳格運用、OpenAPI 粒度のようなプロジェクト依存ルールを移す。
- 初動は `PRD 作成` ではなく `task classification` を基本にし、実装、レビュー、調査、説明の分岐を持たせる。
- `質問必須` をやめ、`まず探索し、探索で解けない不確定だけ質問する` に変える。
- `Plan` と `Context Reset` は、長時間または高複雑度のタスク向けに限定する。
- PR やコミットの詳細フォーマットは、SSOT ではなくテンプレートや補助コマンド側へ寄せる。
- `review`, `debug`, `explain`, `research` の軽量ワークフローを追加する。
- `commandify` を Codex Skills の追加判断まで接続し、繰り返し手順を skill 化する基準を明文化する。

## References
- `agents/rules.md`
- `agents/context-policy.md`
- `agents/workflow.md`
- `docs/tasks/by-feature.md`
