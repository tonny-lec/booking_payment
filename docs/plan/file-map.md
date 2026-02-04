---
doc_type: "plan"
id: "file-map"
version: "0.3"
last_updated: "2026-02-04"
status: "draft"
---

# File Map（DDD + Hexagonal）

## ディレクトリ構成（モジュール別）

```text
domain/src/main/java/com/booking/<context>/domain/...          # ドメインモデル
application/src/main/java/com/booking/<context>/application/... # ユースケース
adapter-web/src/main/java/com/booking/<context>/adapter/web/... # 入力アダプタ
adapter-persistence/src/main/java/com/booking/<context>/adapter/persistence/... # 出力アダプタ
bootstrap/src/main/java/com/booking/...                         # Spring Boot エントリポイント/設定

*/src/main/java/com/booking/shared/...                          # 横断関心（共通例外/共通Adapter等）
```

## 禁止依存

- `domain → adapter`（ドメイン層はアダプタ層に依存しない）
- `domain → spring`（ドメイン層はSpringに依存しない）

---

## 機能→設計書→OpenAPI 対応表

| ユースケース | Bounded Context | 設計書 | OpenAPI |
|-------------|----------------|--------|---------|
| ログイン/リフレッシュ | IAM | [iam-login.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/iam-login.md) | [iam.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/iam.yaml) |
| 予約作成 | Booking | [booking-create.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/booking-create.md) | [booking.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/booking.yaml) |
| 予約変更 | Booking | [booking-update.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/booking-update.md) | [booking.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/booking.yaml) |
| 予約キャンセル | Booking | [booking-cancel.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/booking-cancel.md) | [booking.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/booking.yaml) |
| 支払い作成 | Payment | [payment-create.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/payment-create.md) | [payment.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/payment.yaml) |
| 支払いキャプチャ | Payment | [payment-capture.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/payment-capture.md) | [payment.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/payment.yaml) |
| 返金 | Payment | [payment-refund.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/payment-refund.md) | [payment.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/payment.yaml) |
| 通知送信 | Notification | [notification-send.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/notification-send.md) | [notification.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/notification.yaml) |
| 監査記録 | Audit | [audit-record.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/audit-record.md) | [audit.yaml](file:///home/tonny/workspace/booking_payment/docs/api/openapi/audit.yaml) |
| 台帳投影 | Ledger | [ledger-project.md](file:///home/tonny/workspace/booking_payment/docs/design/usecases/ledger-project.md) | - |

---

## Bounded Context 設計書

| Context | 設計書 |
|---------|--------|
| IAM | [iam.md](file:///home/tonny/workspace/booking_payment/docs/design/contexts/iam.md) |
| Booking | [booking.md](file:///home/tonny/workspace/booking_payment/docs/design/contexts/booking.md) |
| Payment | [payment.md](file:///home/tonny/workspace/booking_payment/docs/design/contexts/payment.md) |
| Notification | [notification.md](file:///home/tonny/workspace/booking_payment/docs/design/contexts/notification.md) |
| Audit | [audit.md](file:///home/tonny/workspace/booking_payment/docs/design/contexts/audit.md) |
| Ledger | [ledger.md](file:///home/tonny/workspace/booking_payment/docs/design/contexts/ledger.md) |
