# 未完了タスク: PRD承認

## 概要
- 対象ファイル: `docs/prd-platform.md`, `docs/prd-devex-ai.md`
- 状態: `status: proposed`（承認待ち）
- 優先度: **高**（実装開始のゲート）

---

## タスク一覧

### PRD-1: Platform PRD承認
- ファイル: `docs/prd-platform.md`
- 現状: `status: proposed`
- 必要なアクション:
  - [ ] 人間によるレビュー
  - [ ] 必要に応じて内容修正
  - [ ] `status: approved` に変更
- 承認後に可能になること:
  - 実装コード（src/）の変更
  - `scripts/prd-gate.sh` のPASS

### PRD-2: DevEx AI PRD承認
- ファイル: `docs/prd-devex-ai.md`
- 現状: `status: proposed`
- 必要なアクション:
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
- Slice A の Docs が完成しても、PRD承認なしには実装に進めない

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
