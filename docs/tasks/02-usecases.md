# 未完了タスク: ユースケース設計

## 概要
- 対象フォルダ: `docs/design/usecases/`
- 状態: 9完了 / 1未完了（Ledger）
- 優先度: **高〜中**
- 関連: [by-feature.md](./by-feature.md), [implementation-slice-a.md](./implementation-slice-a.md)

---

## タスク一覧

<a id="UC-0-IAM"></a>
### UC-0-IAM: IAMログインユースケース
- ファイル: `docs/design/usecases/iam-login.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-IAM), [implementation-slice-a](./implementation-slice-a.md#IMPL-IAM)

<a id="UC-0-BK-CREATE"></a>
### UC-0-BK-CREATE: 予約作成ユースケース
- ファイル: `docs/design/usecases/booking-create.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-BOOKING), [implementation-slice-a](./implementation-slice-a.md#IMPL-BOOKING)

<a id="UC-0-PAY-CREATE"></a>
### UC-0-PAY-CREATE: 支払い作成ユースケース
- ファイル: `docs/design/usecases/payment-create.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-PAYMENT), [implementation-slice-a](./implementation-slice-a.md#IMPL-PAYMENT)

<a id="UC-1"></a>
### UC-1: 予約変更ユースケース
- ファイル: `docs/design/usecases/booking-update.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-BOOKING), [implementation-slice-a](./implementation-slice-a.md#IMPL-BOOKING)

<a id="UC-2"></a>
### UC-2: 予約キャンセルユースケース
- ファイル: `docs/design/usecases/booking-cancel.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-BOOKING), [implementation-slice-a](./implementation-slice-a.md#IMPL-BOOKING)

<a id="UC-3"></a>
### UC-3: 支払いキャプチャユースケース
- ファイル: `docs/design/usecases/payment-capture.md`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-PAYMENT)

<a id="UC-4"></a>
### UC-4: 返金ユースケース
- ファイル: `docs/design/usecases/payment-refund.md`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-PAYMENT)

<a id="UC-5"></a>
### UC-5: 通知送信ユースケース
- ファイル: `docs/design/usecases/notification-send.md`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-NOTIFICATION)

<a id="UC-6"></a>
### UC-6: 監査記録ユースケース
- ファイル: `docs/design/usecases/audit-record.md`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-AUDIT)

<a id="UC-7"></a>
### UC-7: 台帳投影ユースケース
- ファイル: `docs/design/usecases/ledger-project.md`
- Slice: D
- 状態: ⬜ 未着手
- 関連: [by-feature](./by-feature.md#BF-LEDGER)

未完了セクション:
- [ ] 1. 目的/背景
- [ ] 3. 入出力（イベント受信 → Projection更新）
- [ ] 4. ドメインモデル
- [ ] 6. 失敗モード（再構築戦略）
- [ ] 7. 観測性
- [ ] 8. セキュリティ
- [ ] 10. ADRリンク
- [ ] 11. Evidence
- [ ] 12. 未決事項

---

## 完了条件（DoD）
- 各ファイルでテンプレ準拠（目的/入出力/集約/失敗モード/観測性/セキュリティ/テスト/Evidence）
- `scripts/evidence-lint.sh` がPASS
- LedgerユースケースのTODOが解消されている
