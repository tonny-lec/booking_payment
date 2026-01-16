# 未完了タスク: ユースケース設計

## 概要
- 対象フォルダ: `docs/design/usecases/`
- 状態: 7ファイルがTODO状態（3ファイルは完了済み）
- 優先度: **高〜中**

---

## 完了済み
- [x] `iam-login.md` - Slice A
- [x] `booking-create.md` - Slice A
- [x] `payment-create.md` - Slice A

---

## タスク一覧

### UC-1: 予約変更ユースケース
- ファイル: `docs/design/usecases/booking-update.md`
- Slice: A（追加）
- 優先度: 高
- 未完了セクション:
  - [ ] 1. 目的/背景
  - [ ] 3. 入出力（UpdateBookingCommand → BookingUpdated）
  - [ ] 4. ドメインモデル（楽観的ロック、TimeRange再検証）
  - [ ] 6. 失敗モード（conflict_409、version_mismatch）
  - [ ] 7. 観測性
  - [ ] 8. セキュリティ（所有者のみ変更可）
  - [ ] 10. ADRリンク
  - [ ] 11. Evidence
  - [ ] 12. 未決事項

### UC-2: 予約キャンセルユースケース
- ファイル: `docs/design/usecases/booking-cancel.md`
- Slice: A（追加）
- 優先度: 高
- 未完了セクション:
  - [ ] 1. 目的/背景
  - [ ] 3. 入出力（CancelBookingCommand → BookingCancelled）
  - [ ] 4. ドメインモデル（状態遷移、CONFIRMED→CANCELLED）
  - [ ] 6. 失敗モード（already_cancelled）
  - [ ] 7. 観測性
  - [ ] 8. セキュリティ（所有者のみキャンセル可）
  - [ ] 10. ADRリンク
  - [ ] 11. Evidence
  - [ ] 12. 未決事項
- 備考: キャンセル時の返金トリガーを含む

### UC-3: 支払いキャプチャユースケース
- ファイル: `docs/design/usecases/payment-capture.md`
- Slice: B
- 優先度: 中
- 未完了セクション:
  - [ ] 1. 目的/背景
  - [ ] 3. 入出力（CapturePaymentCommand → PaymentCaptured）
  - [ ] 4. ドメインモデル（AUTHORIZED→CAPTURED遷移）
  - [ ] 6. 失敗モード（gateway_error、invalid_state）
  - [ ] 7. 観測性
  - [ ] 8. セキュリティ
  - [ ] 10. ADRリンク
  - [ ] 11. Evidence
  - [ ] 12. 未決事項

### UC-4: 返金ユースケース
- ファイル: `docs/design/usecases/payment-refund.md`
- Slice: B
- 優先度: 中
- 未完了セクション:
  - [ ] 1. 目的/背景
  - [ ] 3. 入出力（RefundPaymentCommand → PaymentRefunded）
  - [ ] 4. ドメインモデル（全額/部分返金、返金額制約）
  - [ ] 6. 失敗モード（refund_exceeded、gateway_error）
  - [ ] 7. 観測性
  - [ ] 8. セキュリティ
  - [ ] 10. ADRリンク
  - [ ] 11. Evidence
  - [ ] 12. 未決事項

### UC-5: 通知送信ユースケース
- ファイル: `docs/design/usecases/notification-send.md`
- Slice: B
- 優先度: 中
- 未完了セクション:
  - [ ] 1. 目的/背景
  - [ ] 3. 入出力（SendNotificationCommand / イベント受信）
  - [ ] 4. ドメインモデル
  - [ ] 6. 失敗モード（delivery_failed、retry戦略）
  - [ ] 7. 観測性
  - [ ] 8. セキュリティ
  - [ ] 10. ADRリンク
  - [ ] 11. Evidence
  - [ ] 12. 未決事項

### UC-6: 監査記録ユースケース
- ファイル: `docs/design/usecases/audit-record.md`
- Slice: B
- 優先度: 中
- 未完了セクション:
  - [ ] 1. 目的/背景
  - [ ] 3. 入出力（RecordAuditCommand / イベント受信）
  - [ ] 4. ドメインモデル
  - [ ] 6. 失敗モード
  - [ ] 7. 観測性
  - [ ] 8. セキュリティ（改ざん防止）
  - [ ] 10. ADRリンク
  - [ ] 11. Evidence
  - [ ] 12. 未決事項

### UC-7: 台帳投影ユースケース
- ファイル: `docs/design/usecases/ledger-project.md`
- Slice: D
- 優先度: 低
- 未完了セクション:
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
- 各ファイルでTODOが解消されている
- テンプレ準拠（目的/入出力/集約/失敗モード/観測性/セキュリティ/テスト/Evidence）
- `scripts/evidence-lint.sh` がPASS
