# 未完了タスク: PRD承認

## 概要
- 対象ファイル: `docs/prd-platform.md`, `docs/prd-devex-ai.md`
- 状態: Platformは承認済み / DevEx AIは提案中
- 優先度: **高**（実装開始のゲート）
- 関連: [by-feature.md](./by-feature.md)

---

## タスク一覧

<a id="PRD-1"></a>
### PRD-1: Platform PRD承認
- ファイル: `docs/prd-platform.md`
- 現状: `status: approved`
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-PRD)

<a id="PRD-2"></a>
### PRD-2: DevEx AI PRD承認
- ファイル: `docs/prd-devex-ai.md`
- 現状: `status: proposed`
- 状態: ⬜ 未着手
- 関連: [by-feature](./by-feature.md#BF-PRD)

未完了アクション:
- [ ] 人間によるレビュー
- [ ] 必要に応じて内容修正
- [ ] `status: approved` に変更

---

## PRD-First ルール（rules.md準拠）

```
Must（絶対）:
1. PRD-First：承認済みPRD（status: approved）なしに実装（コード）変更しない
```

- **実装開始前に必ずPRD承認が必要**
- Slice A の Docs が完成しても、PRD承認なしには実装に進めない（DevEx AIは別ゲート）

---

## 承認フロー

```
1. PRD内容のレビュー
2. 不明点・懸念点の解消
3. status: proposed → status: approved に変更
4. 実装開始可能
```

---

## 完了条件（DoD）
- `docs/prd-platform.md` の `status: approved`
- `docs/prd-devex-ai.md` の `status: approved`
- `scripts/prd-gate.sh` がPASS
