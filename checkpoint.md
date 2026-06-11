---
doc_type: "checkpoint"
version: "0.2"
last_updated: "2026-06-11"
status: "active"
---

# checkpoint.md（Context Reset：会話が長くなったらここへ要約）

## 現在のゴール
- Slice A の縦スライス完成: IAM / Booking に続き Payment の最小フロー（作成・取得）を実装済み。次は capture/refund エンドポイントと idempotency_records 方式への移行

## 決定事項（SSOTに反映済み）
- Payment 縦スライス（POST /payments / GET /payments/{id}）を実装（詳細: `docs/tasks/implementation-slice-a.md` 更新履歴 2026-06-11）
- 冪等性は Slice A では `payments.idempotency_key` ユニーク制約＋リクエスト内容比較（userId/bookingId/amount/currency）で実現。同一キー再送は 200 で既存結果、内容不一致は 409
- 外部決済ゲートウェイは `PaymentGatewayPort`（ACL）で抽象化し、bootstrap に `StubPaymentGateway`（常に与信成功）を配置
- `app.openapi.validation.path-to-spec` のマップキーはブラケット記法必須（relaxed binding がキーの `/` `*` を破壊し全 API が 400 になるバグを修正済み）

## 未決事項 / 質問
- idempotency_records テーブル（24h TTL・レスポンスキャッシュ）の導入時期
- Payment → Booking 連携の最終形（現状はモジュラーモノリス内で BookingRepository ポートを直接参照。ACLポート化 or イベント連携が候補）
- ドメインイベント（PaymentCreated 等）の発行基盤（Outbox 等）

## 次のタスク（ファイルパス + 検証）
- `POST /payments/{id}/capture` / `POST /payments/{id}/refund` の実装
  - 追加: `application/.../payment/application/usecase/CapturePaymentUseCase.java` ほか、`adapter-web/.../payment/adapter/web/PaymentController.java` 拡張
  - 検証: `./gradlew :application:test :adapter-web:test :bootstrap:test`（E2E は `PaymentFlowE2ETest` を拡張）
- Booking の未完テスト補完（BK-T-03/04/05/06）と DELETE /bookings/{id}（BK-W-04）
- 本物のゲートウェイ Adapter（タイムアウト・リトライ・502/504変換、`docs/design/usecases/payment-create.md` §8 準拠）

## 直近の検証コマンド
```bash
./gradlew build          # 全モジュールのビルド＋テスト（E2E は Docker 必須）
./gradlew :bootstrap:test --tests "com.booking.payment.PaymentFlowE2ETest"
bash scripts/test-all.sh # ドキュメント検証 + Gradle テスト
```
