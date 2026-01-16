---
doc_type: "plan"
id: "structured-plan"
version: "0.2"
last_updated: "2026-01-16"
status: "draft"
---

# Structured Plan（タスク単位：Context Independent）

## Constraint（Initialization準拠）
- 各タスクは必出：
  - 手順
  - 編集するファイルパス
  - 検証（テスト/再現/観測）
  - **Context Independent: YES**（会話履歴なしで実行可能）

---

## Gate 0：PRD承認（必須）
### Task G0-1：Platform PRDを埋める
- Context Independent: YES
- 編集：`docs/prd-platform.md`
- 検証：scope/acceptance/approval gate がある

### Task G0-2：人間承認→approvedへ
- Context Independent: YES
- 編集：`docs/prd-platform.md` の `status: approved`
- 検証：`scripts/prd-gate.sh` がPASS（実装変更を入れた場合）

---

## Phase 0：設計（DDD + 契約）
### Task P0-1：用語集をSSOT化
- Context Independent: YES
- 編集：`docs/domain/glossary.md`
- 検証：Booking/Payment/IAMの主要用語が定義済み

### Task P0-2：コンテキスト設計（6本）
- Context Independent: YES
- 編集：`docs/design/contexts/*.md`
- 検証：テンプレ見出し（集約/イベント/非機能）がある

### Task P0-3：ユースケース設計（10本）
- Context Independent: YES
- 編集：`docs/design/usecases/*.md`
- 検証：Evidence見出しがある（`scripts/evidence-lint.sh`）

### Task P0-4：OpenAPI（契約）を埋める
- Context Independent: YES
- 編集：`docs/api/openapi/iam.yaml`, `docs/api/openapi/booking.yaml`, `docs/api/openapi/payment.yaml`
- 検証：paths が空でない

### Task P0-5：テスト計画の具体化
- Context Independent: YES
- 編集：`docs/test/test-plan.md`
- 検証：境界/冪等/権限/互換性が含まれる

---

## Slice A：最小MVP（Docs完成）

### 参照領域
- Backend / Auth / DB / Observability

### Task SA-1：用語集（glossary.md）を作成
- Context Independent: YES
- 編集：`docs/domain/glossary.md`
- 内容：
  - TimeRange、Idempotency Key、Payment状態（PENDING/AUTHORIZED/CAPTURED/REFUNDED）
  - Booking状態（PENDING/CONFIRMED/CANCELLED）
  - 認証トークン（AccessToken/RefreshToken）
- 検証：IAM/Booking/Paymentの主要用語が定義済み
- DoD：各用語に定義・使用例・関連コンテキストが記載されている

### Task SA-2：IAM OpenAPI（iam.yaml）を具体化
- Context Independent: YES
- 編集：`docs/api/openapi/iam.yaml`
- 内容：
  - POST /auth/login（ログイン）
  - POST /auth/refresh（トークンリフレッシュ）
  - POST /auth/logout（ログアウト）
  - Security Scheme（Bearer JWT）
  - エラーフォーマット（RFC 7807準拠）
- 検証：paths が空でない、スキーマ定義がある
- DoD：認証フローに必要な全エンドポイントが定義済み

### Task SA-3：Booking OpenAPI（booking.yaml）を具体化
- Context Independent: YES
- 編集：`docs/api/openapi/booking.yaml`
- 内容：
  - POST /bookings（予約作成）
  - GET /bookings/{id}（予約取得）
  - リクエスト/レスポンススキーマ（Booking、TimeRange、Error）
  - 409 Conflict（衝突時）の設計
  - Security（Bearer認証）
- 検証：paths が空でない、スキーマ定義がある
- DoD：予約作成に必要な全エンドポイントが定義済み

### Task SA-4：Payment OpenAPI（payment.yaml）を具体化
- Context Independent: YES
- 編集：`docs/api/openapi/payment.yaml`
- 内容：
  - POST /payments（支払い作成：冪等キーヘッダー必須）
  - GET /payments/{id}（支払い取得）
  - リクエスト/レスポンススキーマ（Payment、PaymentStatus）
  - 冪等性設計（Idempotency-Key）
  - Security（Bearer認証）
- 検証：paths が空でない、冪等キー設計がある
- DoD：支払い作成に必要な全エンドポイントが定義済み

### Task SA-5：iam-login.md ユースケース具体化
- Context Independent: YES
- 編集：`docs/design/usecases/iam-login.md`
- 内容：
  - 目的/背景（認証フローの意義）
  - 入出力（Command: LoginRequest → Event: TokenIssued）
  - 集約/不変条件（User集約、認証情報の検証）
  - 失敗モード（invalid_credentials、account_locked、rate_limit）
  - 観測性（login_success/login_failure メトリクス）
  - セキュリティ（brute-force対策、PII非出力）
- 検証：Evidence見出しがある
- DoD：テンプレ準拠で全セクション記載

### Task SA-6：booking-create.md ユースケース具体化
- Context Independent: YES
- 編集：`docs/design/usecases/booking-create.md`
- 内容：
  - 目的/背景（予約作成フローの意義）
  - 入出力（Command: CreateBookingRequest → Event: BookingCreated）
  - 集約/不変条件（Booking集約、TimeRange重複禁止）
  - 失敗モード（conflict_409、validation_error、timeout）
  - 観測性（booking_created メトリクス、traceId）
  - セキュリティ（認可：user_id一致）
- 検証：Evidence見出しがある
- DoD：テンプレ準拠で全セクション記載

### Task SA-7：payment-create.md ユースケース具体化
- Context Independent: YES
- 編集：`docs/design/usecases/payment-create.md`
- 内容：
  - 目的/背景（支払い作成フローの意義）
  - 入出力（Command: CreatePaymentRequest → Event: PaymentCreated）
  - 集約/不変条件（Payment集約、金額正値、冪等キー一意）
  - 失敗モード（idempotency_conflict、gateway_timeout、retry戦略）
  - 観測性（payment_created メトリクス、traceId）
  - セキュリティ（認可：booking所有者一致、PII非出力）
- 検証：Evidence見出しがある
- DoD：テンプレ準拠で全セクション記載

### Task SA-8：Gitコミット（Slice A Docs）
- Context Independent: YES
- 実行：`git add . && git commit -m "docs: add Slice A documentation (glossary, OpenAPI, usecases)"`
- 検証：コミット履歴に変更が残る
