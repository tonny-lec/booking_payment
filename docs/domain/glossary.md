---
doc_type: "glossary"
id: "glossary"
version: "0.2"
last_updated: "2026-01-17"
status: "stable"
---

# 用語集（Glossary）SSOT

本ドキュメントは、予約・決済基盤で使用するユビキタス言語の定義です。
すべてのドキュメント・コード・会話で統一して使用してください。

---

## IAM（認証/認可）コンテキスト

### User（ユーザー）
- **定義**：システムに登録された利用者。認証情報（credentials）を持つ
- **使用例**：「Userがログインリクエストを送信する」
- **関連コンテキスト**：IAM、Booking、Payment

### AccessToken（アクセストークン）
- **定義**：認証済みユーザーを識別する短命のJWTトークン（推奨有効期限：15分〜1時間）
- **使用例**：「AccessTokenをAuthorizationヘッダーに付与してAPIを呼び出す」
- **関連コンテキスト**：IAM
- **制約**：PIIを含めない、署名検証必須

### RefreshToken（リフレッシュトークン）
- **定義**：AccessTokenを再発行するための長命トークン（推奨有効期限：7日〜30日）
- **使用例**：「AccessToken期限切れ時にRefreshTokenで再発行する」
- **関連コンテキスト**：IAM
- **制約**：サーバー側で失効管理、ローテーション推奨

### Credential（認証情報）
- **定義**：ユーザーを認証するための情報（例：email + password）
- **使用例**：「Credentialを検証してTokenを発行する」
- **関連コンテキスト**：IAM
- **制約**：パスワードはハッシュ化して保存、平文をログに出力しない

---

## Booking（予約）コンテキスト

### Booking（予約）
- **定義**：特定のリソース（部屋、席など）の特定時間帯の利用権を表す集約
- **使用例**：「UserがBookingを作成する」
- **関連コンテキスト**：Booking、Payment

### BookingStatus（予約ステータス）
- **定義**：予約のライフサイクル状態
- **状態一覧**：
  - `PENDING`：作成直後、支払い待ち
  - `CONFIRMED`：支払い完了、予約確定
  - `CANCELLED`：キャンセル済み
- **状態遷移**：
  ```
  PENDING → CONFIRMED（支払い成功時）
  PENDING → CANCELLED（タイムアウトまたはユーザーキャンセル）
  CONFIRMED → CANCELLED（キャンセルリクエスト）
  ```
- **関連コンテキスト**：Booking

### TimeRange（時間範囲）
- **定義**：開始時刻（startAt）と終了時刻（endAt）のペア。予約の占有期間を表す
- **使用例**：「Bookingは1つのTimeRangeを持つ」
- **関連コンテキスト**：Booking
- **不変条件**：
  - startAt < endAt（開始は終了より前）
  - startAt >= now（過去の予約は不可）
  - 同一リソースでTimeRangeが重複する予約は作成不可

### ResourceId（リソースID）
- **定義**：予約対象のリソース（部屋、席など）を一意に識別するID
- **使用例**：「BookingはResourceIdとTimeRangeで一意性を持つ」
- **関連コンテキスト**：Booking

---

## Payment（決済）コンテキスト

### Payment（支払い）
- **定義**：予約に紐づく決済を表す集約。外部決済ゲートウェイとの連携を含む
- **使用例**：「BookingのConfirm前にPaymentを作成する」
- **関連コンテキスト**：Payment、Booking

### PaymentStatus（支払いステータス）
- **定義**：支払いのライフサイクル状態
- **状態一覧**：
  - `PENDING`：支払い処理中
  - `AUTHORIZED`：与信枠確保済み（キャプチャ前）
  - `CAPTURED`：決済確定（売上計上）
  - `REFUNDED`：返金済み
  - `FAILED`：決済失敗
- **状態遷移**：
  ```
  PENDING → AUTHORIZED（与信成功）
  PENDING → FAILED（与信失敗）
  AUTHORIZED → CAPTURED（キャプチャ成功）
  AUTHORIZED → REFUNDED（キャプチャ前キャンセル）
  CAPTURED → REFUNDED（返金処理）
  ```
- **関連コンテキスト**：Payment

### IdempotencyKey（冪等キー）
- **定義**：同一リクエストの重複処理を防ぐためのクライアント生成キー（UUID推奨）
- **使用例**：「Payment作成リクエストにはIdempotencyKeyヘッダーが必須」
- **関連コンテキスト**：Payment
- **制約**：
  - 同一キーでの再リクエストは、既存結果を返す（再処理しない）
  - キーの有効期限：24時間（推奨）
  - サーバー側でキーと結果を永続化

### Amount（金額）
- **定義**：支払い金額を表す値オブジェクト（通貨単位の最小単位で表現）
- **使用例**：「Amount: 10000（= 100.00 JPY）」
- **関連コンテキスト**：Payment
- **不変条件**：正の整数、通貨コードとペアで管理

### Currency（通貨）
- **定義**：ISO 4217準拠の通貨コード
- **使用例**：「Currency: JPY」
- **関連コンテキスト**：Payment

---

## 共通用語

### AggregateId（集約ID）
- **定義**：集約を一意に識別するID（UUID推奨）
- **使用例**：「BookingId、PaymentIdはAggregateIdの一種」
- **関連コンテキスト**：全コンテキスト

### DomainEvent（ドメインイベント）
- **定義**：ドメインで発生した重要な事実を表す不変オブジェクト
- **使用例**：「BookingCreated、PaymentCapturedはDomainEvent」
- **関連コンテキスト**：全コンテキスト

### TraceId（トレースID）
- **定義**：分散システムでリクエストを追跡するための一意ID（OpenTelemetry準拠）
- **使用例**：「ログとトレースをTraceIdで相関させる」
- **関連コンテキスト**：Observability

### CorrelationId（相関ID）
- **定義**：関連する複数のリクエスト/イベントを紐付けるID
- **使用例**：「予約→支払い→通知をCorrelationIdで追跡」
- **関連コンテキスト**：全コンテキスト

---

## エラー関連

### ProblemDetail（問題詳細）
- **定義**：RFC 7807準拠のエラーレスポンス形式
- **使用例**：
  ```json
  {
    "type": "https://api.example.com/errors/booking-conflict",
    "title": "Booking Conflict",
    "status": 409,
    "detail": "The requested time range conflicts with an existing booking",
    "instance": "/bookings/123"
  }
  ```
- **関連コンテキスト**：全API

---

## Evidence（根拠）

- 用語定義は、DDD（Domain-Driven Design）のユビキタス言語の原則に基づく
- 状態遷移は、予約・決済システムの一般的なパターンを参考（推論を含む、実装時に検証が必要）
- RFC 7807：https://datatracker.ietf.org/doc/html/rfc7807
- ISO 4217：https://www.iso.org/iso-4217-currency-codes.html
