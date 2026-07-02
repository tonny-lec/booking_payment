---
doc_type: "implementation_spec"
id: "cancellation-refund-policy-selection"
status: "approved"
related_prd: "docs/prd-platform.md"
related_usecases:
  - "docs/design/usecases/booking-cancel.md"
  - "docs/design/usecases/payment-refund.md"
---

# キャンセル時返金ポリシー選択 仕様

## 成果

予約キャンセル時に、呼び出し元が返金ポリシーを `FULL_REFUND`、`PARTIAL_REFUND`、`NO_REFUND` から選択できるようにする。最初のスライスでは既存の同期 Spring/DDD 実装内で、予約キャンセルと返金またはオーソリ取消を同一ユースケース境界から実行し、選択されたポリシーに応じて支払い状態を機械的に検証する。

呼び出し側が指定する入力は次の構造を想定する。

```text
CancelBookingRequest {
  refundPolicy: "FULL_REFUND" | "PARTIAL_REFUND" | "NO_REFUND" (required)
  partialRefundAmount: Integer? (PARTIAL_REFUND の場合のみ required)
  reason: String? (optional, max 500)
}
```

ポリシーごとの振る舞いは次の通り。

| ポリシー | 対象 | 振る舞い |
| --- | --- | --- |
| `FULL_REFUND` | 支払いが `AUTHORIZED` または `CAPTURED` の予約 | `AUTHORIZED` は gateway authorization を void し、支払い状態を `REFUNDED` にする。`CAPTURED` は未返金残額の全額を refund し、全額返金後に `REFUNDED` にする。 |
| `PARTIAL_REFUND` | 支払いが `CAPTURED` の予約 | `partialRefundAmount` を返金額として refund する。返金後も未返金残額があれば支払い状態は `CAPTURED` を維持し、残額が 0 なら `REFUNDED` にする。 |
| `NO_REFUND` | 返金しないキャンセル | 予約だけを `CANCELLED` にする。支払い状態は変更しない。 |

`PARTIAL_REFUND` の金額はリクエストで明示的に渡す。検証条件は `partialRefundAmount > 0`、かつ `partialRefundAmount <= capturedAmount - refundedAmount` とする。`capturedAmount` が null の場合は既存 `RefundPaymentUseCase` と同じく `payment.money().amount()` を最大返金額として扱う。

支払い状態ごとの扱いは次の通り。

| 支払い状態 | `FULL_REFUND` | `PARTIAL_REFUND` | `NO_REFUND` |
| --- | --- | --- | --- |
| 支払いなし | 422 `payment_required_for_refund_policy` | 422 `payment_required_for_refund_policy` | 予約キャンセル成功 |
| `PENDING` | 422 `payment_not_refundable` | 422 `payment_not_refundable` | 予約キャンセル成功 |
| `AUTHORIZED` | authorization を void して予約キャンセル成功 | 422 `payment_not_captured_for_partial_refund` | 予約キャンセル成功 |
| `CAPTURED` | 未返金残額を全額返金して予約キャンセル成功 | 指定額を返金して予約キャンセル成功 | 予約キャンセル成功 |
| `REFUNDED` | 既存結果として扱い、予約キャンセル成功 | 422 `payment_already_refunded` | 予約キャンセル成功 |

## スコープ境界

このスライスで扱う。

- `DELETE /bookings/{bookingId}` から返金ポリシーを受け取り、Booking と Payment の既存ユースケースを同期的に接続する。
- Booking 所有者と Payment 所有者が一致することを検証する。
- 予約が `PENDING` または `CONFIRMED` の場合だけキャンセルする。
- `CONFIRMED` 予約で返金対象の支払いを bookingId から取得する。
- `FULL_REFUND`、`PARTIAL_REFUND`、`NO_REFUND` の分岐とエラーコードをアプリケーション層でテストできる形にする。

このスライスでは扱わない。

- イベント駆動の返金オーケストレーション。
- 通知、監査、Ledger 連携の実装。
- 返金手数料、キャンセル期限、時間窓ルール、管理者承認。
- 複数支払いをまたぐ返金配分。
- 外部決済プロバイダの本番接続。
- migration の変更。この文書は次の tdd-loop の入力であり、DB スキーマ変更はこのスライスでは行わない。

## 制約

- 根拠 PRD は `docs/prd-platform.md` で、`status: approved` である。
- 既存の Spring/DDD/Hexagonal 構成に合わせ、Booking から Payment 永続化実装へ直接依存しない。
- 返金処理は既存 `RefundPaymentUseCase` と `PaymentGatewayPort` の責務に寄せ、重複する gateway 呼び出しロジックを Booking 側へ複製しない。
- `PaymentRepository` は現状 `findById`、`findByIdempotencyKey`、`existsActiveByBookingId`、`save` だけを提供しているため、返金対象 Payment を bookingId で取得する読み取り能力が必要になる。
- `BookingController` の既存キャンセルエンドポイントはリクエスト body を受け取らず、`CancelBookingCommand.reason` に null を渡しているため、ポリシー入力を受け取る controller DTO と command 拡張が必要になる。
- 部分返金の冪等性は既存 `payment-refund` 文書でも未決事項である。最初のスライスでは cancellation request ごとに idempotency key を生成または受け渡す設計を実装面で明確化し、同一キャンセルの二重実行で二重返金しないテストを必須にする。
- 予約キャンセルと返金の永続化は同期実装内で整合性を保つ。gateway 成功後の DB 保存失敗など分散トランザクション問題はこのスライスでは補償処理まで実装しないが、失敗時の例外伝播とテストで観測可能にする。

## 過去の決定

- `docs/prd-platform.md` は Slice B に `payment-capture` / `payment-refund` を含め、予約・決済基盤の DDD + Hexagonal 構成を前提にしている。
- `docs/design/usecases/booking-cancel.md` は `CONFIRMED` からのキャンセルで返金処理をトリガーし、`PENDING` からのキャンセルでは返金不要と定義している。
- `docs/design/usecases/payment-refund.md` は全額返金と部分返金を区別し、`CAPTURED` の部分返金では `CAPTURED` を維持し、全額返金後に `REFUNDED` とする方針を示している。
- 既存 `RefundPaymentUseCase` は `AUTHORIZED` を void し、`CAPTURED` を refund し、`REFUNDED` は既存結果として返す。
- 人間承認済みの最初のスライスのポリシー選択肢は `FULL_REFUND`、`PARTIAL_REFUND`、`NO_REFUND` の 3 つだけである。

## タスク分解

1. ドメイン入力を追加する。
   - `RefundPolicy` enum を `FULL_REFUND`、`PARTIAL_REFUND`、`NO_REFUND` で定義する。
   - `CancelBookingCommand` に `refundPolicy` と `partialRefundAmount` を追加する。
   - `PARTIAL_REFUND` 以外で `partialRefundAmount` が渡された場合は 400 または 422 に正規化する。

2. Payment 取得ポートを拡張する。
   - `PaymentRepository` に bookingId から返金対象 Payment を取得するメソッドを追加する。
   - 永続化アダプタに同等の Spring Data query を追加する。
   - 複数候補があり得る場合は、最初のスライスでは active または latest の 1 件だけを扱う条件を実装前に固定する。

3. キャンセルユースケースを接続する。
   - `CancelBookingUseCase` に Payment lookup と `RefundPaymentUseCase` 相当の処理を注入または委譲する。
   - `NO_REFUND` は Payment を変更せず予約キャンセルへ進む。
   - `FULL_REFUND` と `PARTIAL_REFUND` は Payment 検証と gateway 呼び出しが成功した場合だけ予約を保存する。
   - 既に `CANCELLED` の予約では既存の状態遷移拒否を維持し、二重返金を発生させない。

4. Web 入力を追加する。
   - `BookingController.cancelBooking` で request body を受け取る。
   - 未指定の `refundPolicy` は 400 とし、暗黙のデフォルトは置かない。
   - `reason` は既存上限 500 文字に合わせる。

5. エラー変換を明確にする。
   - 支払いなしで返金ポリシーが指定された場合は 422。
   - `PENDING`、`AUTHORIZED`、`CAPTURED`、`REFUNDED` の各状態とポリシー組み合わせをテストで固定する。
   - 所有者不一致は既存方針に合わせて 403 とする。

6. 仕様と契約を更新する。
   - tdd-loop でコードとテストを追加する際に、OpenAPI の request body とエラー例を更新する。
   - `booking-cancel.md` と `payment-refund.md` の未決事項から、このスライスで解決した項目を反映する。

## 検証基準

次の検証を tdd-loop の完了条件にする。

- Unit: `CancelBookingUseCase` が `FULL_REFUND` + `CAPTURED` で未返金残額を全額返金し、予約を `CANCELLED` にする。
- Unit: `CancelBookingUseCase` が `PARTIAL_REFUND` + `CAPTURED` で `partialRefundAmount` だけ返金し、残額がある場合は Payment を `CAPTURED` のままにする。
- Unit: `CancelBookingUseCase` が `NO_REFUND` で Payment を保存せず、予約だけを `CANCELLED` にする。
- Unit: `partialRefundAmount` が null、0 以下、未返金残額超過の場合に拒否する。
- Unit: 支払いなし、`PENDING`、`AUTHORIZED`、`CAPTURED`、`REFUNDED` の状態表に対応する成功または拒否を固定する。
- Unit: 既に `CANCELLED` の予約では gateway refund/void が呼ばれない。
- Integration: `PaymentRepository` の bookingId lookup が対象 Payment を取得し、存在しない場合は empty を返す。
- Web: `DELETE /bookings/{bookingId}` が `refundPolicy` と `partialRefundAmount` を command へ渡す。
- E2E: 予約作成、支払い作成、authorization、capture、キャンセル、返金状態確認までの同期フローが通る。

想定コマンドは次の通り。

```bash
./gradlew test
bash scripts/test-all.sh
bash scripts/evidence-lint.sh
```

OpenAPI 更新の検証として、booking contract test を実行し、`DELETE /bookings/{bookingId}` の request body と 422 エラー例が契約に含まれることを確認する。
