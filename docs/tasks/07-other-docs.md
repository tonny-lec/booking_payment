# 未完了タスク: その他ドキュメント

## 概要
- 対象: マイグレーション、Runbook、設計概要、ADR、用語/技術スタック
- 状態: 4完了 / 2未完了
- 優先度: **低〜中**
- 関連: [by-feature.md](./by-feature.md), [implementation-slice-a.md](./implementation-slice-a.md)

---

## タスク一覧

<a id="DOC-0"></a>
### DOC-0: 用語集
- ファイル: `docs/domain/glossary.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-GLOSSARY)

<a id="DOC-1"></a>
### DOC-1: API マイグレーションガイド
- ファイル: `docs/api/migration/v1-to-v2.md`
- Slice: C
- 状態: ⬜ 未着手

未完了セクション:
- [ ] v1/v2 併走の方針
- [ ] 破壊的変更の一覧
- [ ] 移行手順
- [ ] 廃止スケジュール

<a id="DOC-2"></a>
### DOC-2: Runbook
- ファイル: `docs/runbook/README.md`
- Slice: C
- 状態: ⬜ 未着手

未完了セクション:
- [ ] インシデント対応フロー概要
- [ ] 共通手順（ログ確認、再起動、ロールバック）
- [ ] サービス別Runbookへのリンク
- [ ] `runbook/incident-db-connection.md`
- [ ] `runbook/incident-payment-gateway.md`
- [ ] `runbook/incident-high-latency.md`

<a id="DOC-3"></a>
### DOC-3: 設計概要
- ファイル: `docs/design/overview.md`
- Slice: A
- 状態: ✅ 完了

<a id="DOC-4"></a>
### DOC-4: ADR（Architecture Decision Records）
- フォルダ: `docs/adr/`
- Slice: A〜
- 状態: ✅ 完了
- 参照: `docs/adr/0001-jwt-auth.md` 〜 `docs/adr/0010-timeout-handling.md`

<a id="DOC-5"></a>
### DOC-5: 技術スタック / プロジェクト基盤
- ファイル: `docs/tech-stack.md`, `docs/plan/file-map.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [implementation-slice-a](./implementation-slice-a.md#IMPL-INFRA)

---

## 完了条件（DoD）
- 各ファイルでスケルトンが解消されている
- 実運用に必要な情報が含まれている
- 他のドキュメントと整合性がある
