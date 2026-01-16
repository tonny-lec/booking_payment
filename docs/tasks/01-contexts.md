# 未完了タスク: コンテキスト設計

## 概要
- 対象フォルダ: `docs/design/contexts/`
- 状態: 6ファイル全てTODO状態
- 優先度: **高**（Slice A基盤）

---

## タスク一覧

### CTX-1: IAMコンテキスト設計
- ファイル: `docs/design/contexts/iam.md`
- Slice: A
- 未完了セクション:
  - [ ] 1. 目的
  - [ ] 3. 集約一覧（User, RefreshToken）
  - [ ] 4. Context Map（Booking/Paymentへの認証提供）
  - [ ] 5. 永続化（users, refresh_tokens テーブル）
  - [ ] 6. ドメインイベント（UserLoggedIn, LoginFailed）
  - [ ] 8. ADRリンク
  - [ ] 9. Evidence
  - [ ] 10. 未決事項
- 参照: `docs/design/usecases/iam-login.md` の内容を整理

### CTX-2: Bookingコンテキスト設計
- ファイル: `docs/design/contexts/booking.md`
- Slice: A
- 未完了セクション:
  - [ ] 1. 目的
  - [ ] 3. 集約一覧（Booking, TimeRange）
  - [ ] 4. Context Map（IAMから認証、Paymentへ予約情報提供）
  - [ ] 5. 永続化（bookings テーブル）
  - [ ] 6. ドメインイベント（BookingCreated, BookingCancelled）
  - [ ] 8. ADRリンク
  - [ ] 9. Evidence
  - [ ] 10. 未決事項
- 参照: `docs/design/usecases/booking-create.md` の内容を整理

### CTX-3: Paymentコンテキスト設計
- ファイル: `docs/design/contexts/payment.md`
- Slice: A
- 未完了セクション:
  - [ ] 1. 目的
  - [ ] 3. 集約一覧（Payment, Money, IdempotencyKey）
  - [ ] 4. Context Map（Bookingから予約参照、外部Gateway連携）
  - [ ] 5. 永続化（payments, idempotency_records テーブル）
  - [ ] 6. ドメインイベント（PaymentCreated, PaymentAuthorized, PaymentFailed）
  - [ ] 8. ADRリンク
  - [ ] 9. Evidence
  - [ ] 10. 未決事項
- 参照: `docs/design/usecases/payment-create.md` の内容を整理

### CTX-4: Auditコンテキスト設計
- ファイル: `docs/design/contexts/audit.md`
- Slice: B
- 未完了セクション:
  - [ ] 1. 目的
  - [ ] 3. 集約一覧
  - [ ] 4. Context Map
  - [ ] 5. 永続化
  - [ ] 6. ドメインイベント
  - [ ] 8. ADRリンク
  - [ ] 9. Evidence
  - [ ] 10. 未決事項

### CTX-5: Notificationコンテキスト設計
- ファイル: `docs/design/contexts/notification.md`
- Slice: B
- 未完了セクション:
  - [ ] 1. 目的
  - [ ] 3. 集約一覧
  - [ ] 4. Context Map
  - [ ] 5. 永続化
  - [ ] 6. ドメインイベント
  - [ ] 8. ADRリンク
  - [ ] 9. Evidence
  - [ ] 10. 未決事項

### CTX-6: Ledgerコンテキスト設計
- ファイル: `docs/design/contexts/ledger.md`
- Slice: D
- 未完了セクション:
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
- 各ファイルでTODOが解消されている
- テンプレ見出し（集約/イベント/非機能）がある
- usecaseドキュメントと整合性がある
