# 未完了タスク: テスト計画・観測性

## 概要
- 対象ファイル: `docs/test/test-plan.md`, `docs/design/observability.md`
- 状態: 2完了 / 0未完了
- 優先度: **高**（Slice A検証基準）
- 関連: [by-feature.md](./by-feature.md), [implementation-slice-a.md](./implementation-slice-a.md)

---

## タスク一覧

<a id="TEST-1"></a>
### TEST-1: テスト計画の具体化
- ファイル: `docs/test/test-plan.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-CROSS-TEST), [implementation-slice-a](./implementation-slice-a.md#IMPL-TEST)

<a id="OBS-1"></a>
### OBS-1: 観測性設計の具体化
- ファイル: `docs/design/observability.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-CROSS-OBS), [implementation-slice-a](./implementation-slice-a.md#IMPL-OBS)

---

## 完了条件（DoD）
- test-plan.md: 境界/冪等/権限/互換性テストが含まれる
- observability.md: 必須属性/メトリクス/トレースが具体化されている
- PIIポリシーと整合している
