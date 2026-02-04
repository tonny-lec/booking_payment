# 未完了タスク: OpenAPI仕様

## 概要
- 対象フォルダ: `docs/api/openapi/`
- 状態: 5完了 / 2未完了（gateway, ledger）
- 優先度: **中**
- 関連: [by-feature.md](./by-feature.md), [implementation-slice-a.md](./implementation-slice-a.md)

---

## タスク一覧

<a id="API-IAM"></a>
### API-IAM: IAM API
- ファイル: `docs/api/openapi/iam.yaml`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-IAM), [implementation-slice-a](./implementation-slice-a.md#IMPL-IAM)

<a id="API-BOOKING"></a>
### API-BOOKING: Booking API
- ファイル: `docs/api/openapi/booking.yaml`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-BOOKING), [implementation-slice-a](./implementation-slice-a.md#IMPL-BOOKING)

<a id="API-PAYMENT"></a>
### API-PAYMENT: Payment API
- ファイル: `docs/api/openapi/payment.yaml`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-PAYMENT), [implementation-slice-a](./implementation-slice-a.md#IMPL-PAYMENT)

<a id="API-AUDIT"></a>
### API-AUDIT: Audit API
- ファイル: `docs/api/openapi/audit.yaml`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-AUDIT)

<a id="API-NOTIFICATION"></a>
### API-NOTIFICATION: Notification API
- ファイル: `docs/api/openapi/notification.yaml`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-NOTIFICATION)

<a id="API-GATEWAY"></a>
### API-GATEWAY: Gateway API
- ファイル: `docs/api/openapi/gateway.yaml`
- Slice: 共通
- 状態: ⬜ 未着手
- 現状: `paths: {}`
- 関連: [by-feature](./by-feature.md)

未完了項目:
- [ ] ルーティング/認可/バージョニング方針
- [ ] パス定義（必要であれば）

<a id="API-LEDGER"></a>
### API-LEDGER: Ledger API
- ファイル: `docs/api/openapi/ledger.yaml`（新規）
- Slice: D
- 状態: ⬜ 未着手
- 関連: [by-feature](./by-feature.md#BF-LEDGER)

未完了項目:
- [ ] GET /ledger/entries
- [ ] GET /ledger/balance
- [ ] スキーマ定義（LedgerEntry/Balance）

---

## 完了条件（DoD）
- paths が空でない
- request/response schema が定義されている
- エラー設計（RFC 7807 ProblemDetail）
- Security Scheme（Bearer JWT）
- 互換性の方針がコメントに記載
