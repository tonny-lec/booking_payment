---
doc_type: "agent_policy"
id: "context-policy"
version: "0.4"
last_updated: "2026-03-07"
status: "stable"
---

# コンテキスト運用ポリシー

## 基本原則
- 必要最小限の情報だけを読む
- まず探索し、探索で解けることは質問しない
- タスク種別ごとに読むべき文書を変える

---

## SSOT参照順
1. `agents/rules.md`
2. `agents/workflow.md`
3. `agents/self-check.md`
4. PRD（`docs/prd-*.md`、コード/インフラ変更時のみ優先必須）
5. `docs/domain/glossary.md`
6. `docs/api/openapi/*.yaml`
7. `docs/design/usecases/*.md`
8. `docs/design/contexts/*.md`
9. `docs/plan/*.md`
10. `docs/test/*.md`
11. `checkpoint.md`

---

## Explore Before Asking
- まず `rg` などで対象を探す
- 次に、入口ファイル、設定、型、契約を読む
- それでも解けない仕様判断だけを質問する

質問してよい例:
- 複数の妥当な仕様案がある
- 優先順位やスコープの選択が必要
- レビュー基準や期待成果物が明示されていない

質問しない例:
- 対象ファイルの場所
- 既存フローの把握
- テストや起動方法の確認

---

## Minimal Load Set by Task Type

| タスク種別 | 最初に読むもの |
|---------|-----------------|
| **Implement** | `agents/rules.md`, 関連PRD, 対象コード/仕様 |
| **Review** | `agents/rules.md`, 対象差分, 関連テスト/仕様 |
| **Debug** | `agents/rules.md`, エラーログ, 対象コード, 再現手順 |
| **Explain** | `agents/rules.md`, 対象コード/文書 |
| **Research** | `agents/rules.md`, 関連文書, 対象設定 |

---

## Modular Rules

| 作業領域 | 優先参照 |
|---------|---------|
| **API** | `docs/api/openapi/*.yaml`, `docs/design/usecases/*.md` |
| **認証・認可** | `docs/design/contexts/iam.md`, `docs/security/*.md` |
| **予約機能** | `docs/design/contexts/booking.md`, `docs/design/usecases/booking-*.md` |
| **決済機能** | `docs/design/contexts/payment.md`, `docs/design/usecases/payment-*.md` |
| **セキュリティ** | `docs/security/*.md`, `agents/rules.md` |
| **テスト設計** | `docs/test/*.md`, `docs/design/usecases/*.md` |
| **観測性** | `docs/design/observability.md` |

---

## Context Reset
- 長時間タスクに切り替わるとき
- 会話が長文化したとき
- 無関係なタスクが混ざり始めたとき

手順:
1. `checkpoint.md` に要点をまとめる
2. `Plan` と必要最小限の参照文書だけを残す
3. 以後はその要約をSSOTとして続行する
