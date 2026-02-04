---
doc_type: "agent_policy"
id: "agent-incident-policy"
version: "0.3"
last_updated: "2026-01-22"
status: "stable"
---

# AIエージェント起因インシデント運用

## 基本原則

バグやエラーは単なる修正対象ではなく、「システム（ルール）」の欠陥である。
修正後は必ずルールを更新し、再発防止策をシステムに組み込む。

---

## 成果物
- Postmortem：`docs/postmortem/postmortem-agent-YYYYMMDD-*.md`
- RCA（Root Cause Analysis）：`docs/rca/RCA_issue_date.md`

---

## 重要ルール
- コード修正の前に **System Fix**（`docs/agent/rules.md` の更新）を入れる
- 「人の注意力」に依存する対策で終わらせない
- ルール or テンプレ or 検証手順のどれかが更新されること

---

## RCA（Root Cause Analysis）作成条件

以下の条件に該当する場合、RCAを必ず作成する：

- 同種のミスが2回目
- テスト不足で見逃した
- 仕様の読み違い
- 依存関係/環境差分でハマった

---

## RCAの最低要素

| 項目 | 内容 |
|------|------|
| **現象（Symptoms）** | 何が起きたか |
| **期待結果 vs 実結果** | 何が期待され、何が起きたか |
| **原因** | 直接原因 / 背後要因 |
| **検知** | なぜ早く見つからなかったか |
| **対策** | コード / テスト / ルール / テンプレ |
| **再発防止の更新内容** | どのファイルをどう変えるか |

---

## セルフチェック（RCA）

- [ ] "人の注意力"に依存する対策で終わっていない
- [ ] ルール or テンプレ or 検証手順のどれかが更新される
- [ ] 対策が「観測可能」（自動テスト/CI/静的解析で検出できる）

---

## Evolve（システム進化）

再発防止のために、以下のいずれかを更新する提案を行う：

| 更新対象 | 例 |
|---------|-----|
| **Global Rules** | 全体に適用されるルール（命名規則、ログ戦略など）→ `docs/agent/rules.md` |
| **Reference Docs** | 特定領域のガイドライン → `docs/agent/*.md`, `docs/security/*.md` |
| **Templates** | テンプレートの改善 → `docs/templates/*.md` |
| **Workflow Commands** | プロセス自体の改善 → `scripts/`, `.agent/workflows/` |
