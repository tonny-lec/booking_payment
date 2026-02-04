# 未完了タスク: コンテキスト設計

## 概要
- 対象フォルダ: `docs/design/contexts/`
- 状態: 5完了 / 1未完了（Ledger）
- 優先度: **高**（Slice A基盤）
- 関連: [by-feature.md](./by-feature.md), [implementation-slice-a.md](./implementation-slice-a.md)

---

## タスク一覧

<a id="CTX-1"></a>
### CTX-1: IAMコンテキスト設計
- ファイル: `docs/design/contexts/iam.md`
- Slice: A
- 状態: ✅ 完了
- 参照: `docs/design/usecases/iam-login.md`
- 関連: [by-feature](./by-feature.md#BF-IAM), [implementation-slice-a](./implementation-slice-a.md#IMPL-IAM)

<a id="CTX-2"></a>
### CTX-2: Bookingコンテキスト設計
- ファイル: `docs/design/contexts/booking.md`
- Slice: A
- 状態: ✅ 完了
- 参照: `docs/design/usecases/booking-create.md`
- 関連: [by-feature](./by-feature.md#BF-BOOKING), [implementation-slice-a](./implementation-slice-a.md#IMPL-BOOKING)

<a id="CTX-3"></a>
### CTX-3: Paymentコンテキスト設計
- ファイル: `docs/design/contexts/payment.md`
- Slice: A
- 状態: ✅ 完了
- 参照: `docs/design/usecases/payment-create.md`
- 関連: [by-feature](./by-feature.md#BF-PAYMENT), [implementation-slice-a](./implementation-slice-a.md#IMPL-PAYMENT)

<a id="CTX-4"></a>
### CTX-4: Auditコンテキスト設計
- ファイル: `docs/design/contexts/audit.md`
- Slice: B
- 状態: ✅ 完了
- 参照: `docs/design/usecases/audit-record.md`
- 関連: [by-feature](./by-feature.md#BF-AUDIT)

<a id="CTX-5"></a>
### CTX-5: Notificationコンテキスト設計
- ファイル: `docs/design/contexts/notification.md`
- Slice: B
- 状態: ✅ 完了
- 参照: `docs/design/usecases/notification-send.md`
- 関連: [by-feature](./by-feature.md#BF-NOTIFICATION)

<a id="CTX-6"></a>
### CTX-6: Ledgerコンテキスト設計
- ファイル: `docs/design/contexts/ledger.md`
- Slice: D
- 状態: ⬜ 未着手
- 関連: [by-feature](./by-feature.md#BF-LEDGER)

未完了セクション:
- [ ] 1. 目的
- [ ] 3. 集約一覧
- [ ] 4. Context Map
- [ ] 5. 永続化
- [ ] 6. ドメインイベント
- [ ] 8. ADRリンク
- [ ] 9. Evidence
- [ ] 10. 未決事項

---

## 完了条件（DoD）
- 各ファイルでテンプレ見出し（集約/イベント/非機能）が埋まっている
- usecaseドキュメントと整合性がある
- LedgerコンテキストのTODOが解消されている
