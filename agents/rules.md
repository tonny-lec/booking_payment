---
doc_type: "agent_rules"
id: "rules"
version: "0.5"
last_updated: "2026-03-07"
status: "stable"
---

# ルール（Codex-first SSOT）

## Core Principles

### Context is Currency
- コンテキストウィンドウは貴重な資源である。
- 必要最小限の文書とコードだけを読む。
- 長時間タスクや長文化した会話は `checkpoint.md` に要約し、以後はそれをSSOTとして使う。

### Explore Before Asking
- まずリポジトリと環境を探索し、解ける不確定は自分で潰す。
- 質問は、探索で解けない仕様判断、優先順位、トレードオフに限る。
- 推論は事実と分離し、不確かなら明示する。

### Evidence-First
- 提案、変更、レビュー、調査には根拠を残す。
- 根拠は差分、ログ、テスト結果、仕様、計測、ファイル参照のいずれかで示す。

### System Evolution
- バグや手戻りは個別ミスではなく、運用ルール、テンプレート、検証手順の改善機会として扱う。
- 同種のミスを繰り返した場合は、必ずシステム側を更新する。

---

## Must
1. **PRD-First for code/infra changes**: コードまたはインフラ変更は、承認済みPRD（`status: approved`）なしに実装しない
2. **Small Changes**: 変更は小さく、論点ごとに分ける
3. **Validate Changes**: 変更後は必ず対象に応じた検証を行う
4. **No Secrets / No PII**: Secrets/PII を生成、出力、埋め込みしない
5. **Stay Off Main**: `main` で直接作業しない
6. **One Task, One Branch, One PR**: 標準運用は 1 タスク 1 ブランチ 1 PR とする
7. **Respect User Changes**: 自分が作っていない差分は、明示依頼なしに戻さない
8. **Prefer Non-Destructive Tools**: まず探索、次に局所編集、最後に検証の順で進める

## Must Not
- SSOTを無視してコードだけ変更する
- 推論を事実として断定する
- 外部送信設定を追加する（明示許可なし）
- `main` へ直接 push する
- ユーザーや他プロセスの変更を黙って巻き戻す
- 破壊的な Git 操作を明示承認なしで実行する

---

## Standard Git PR Flow

### Start
1. `main` は読むだけにする
2. 新しいタスクは専用ブランチで開始する
3. まず探索して、対象範囲と検証方法を確定する

### Implementation
- まず小さく編集する
- 各変更の後に、その変更に対応する検証を行う
- 無関係なリファクタは混ぜない

### Submission
- PR は 1 タスク 1 本を原則とする
- PR には要約、根拠、検証結果、関連仕様を含める
- マージは人間が行う

---

## Workflow Profiles

### Core Loop
小さい修正、レビュー、調査、説明では次を標準とする。

1. Explore
2. Confirm intent only if needed
3. Edit or analyze
4. Validate
5. Report

### Extended Plan Flow
次の条件では明示的な Plan を作る。
- 複数ファイルにまたがる
- 複数時間かかる
- 外部依存や移行がある
- 判断点が多い
- 失敗コストが高い

Extended Plan Flow は次を使う。

1. Prime
2. PRD
3. Plan
4. Reset
5. Implement
6. Validate
7. Report

## PR and Reporting
- PR の本文は簡潔に保つ
- 小さい変更では、要約、検証結果、関連仕様があれば十分
- 詳細な設計説明やコードスニペットは、本当に必要な変更だけに限定する
- 実装完了時は、何を変えたか、どう検証したか、残課題は何かを短く残す

---

## System Fix
- 重要失敗や再発は、`rules.md`、テンプレート、検証手順のいずれかを更新して閉じる
- 「次から気を付ける」で終わらせない
