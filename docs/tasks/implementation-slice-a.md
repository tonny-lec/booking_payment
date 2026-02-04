# Slice A 実装タスク一覧

PRD承認済み、設計ドキュメント完了に基づき、Slice A（最小MVP）の実装タスクを機能別に細分化する。

---

## 凡例

- **状態**: ⬜未着手 / 🔄進行中 / ✅完了
- **優先度**: 🔴高 / 🟡中 / 🟢低
- **依存**: 先行タスクのID

---

<a id="IMPL-INFRA"></a>
## 0. プロジェクト基盤

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| INFRA-01 | Spring Boot プロジェクト初期化 | Java 25, Gradle 9.2.1, Spring Boot 4.0.2 | 🔴 | ✅ | - | [INFRA-01](./07-other-docs.md#DOC-5) |
| INFRA-02 | マルチモジュール構成 | domain/application/adapter-web/adapter-persistence/bootstrap | 🔴 | ✅ | INFRA-01 | [INFRA-02](./07-other-docs.md#DOC-5) |
| INFRA-03 | 共通依存関係設定 | OpenTelemetry, Spring Security, PostgreSQL ドライバ | 🔴 | ✅ | INFRA-01 | [INFRA-03](./07-other-docs.md#DOC-5) |
| INFRA-04 | PostgreSQL Docker設定 | docker-compose.yml, 初期スキーマ | 🔴 | ✅ | INFRA-01 | [INFRA-04](./07-other-docs.md#DOC-5) |
| INFRA-05 | OpenTelemetry基盤設定 | TraceId伝播, JSON構造化ログ | 🔴 | ✅ | INFRA-03 | [INFRA-05](./07-other-docs.md#DOC-5) |
| INFRA-06 | 共通エラーハンドリング | RFC 7807 Problem Details, GlobalExceptionHandler | 🔴 | ✅ | INFRA-02 | [INFRA-06](./07-other-docs.md#DOC-5) |
| INFRA-07 | Observability共通メトリクス | REDメトリクス基盤 (Rate, Errors, Duration) | 🟡 | ✅ | INFRA-05 | [INFRA-07](./07-other-docs.md#DOC-5) |

---

<a id="IMPL-IAM"></a>
## 1. IAM（認証）

### 1.1 ドメイン層

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| IAM-D-01 | UserId 値オブジェクト | UUID基盤のID | 🔴 | ✅ | INFRA-02 | [IAM-D-01](./01-contexts.md#CTX-1) |
| IAM-D-02 | Email 値オブジェクト | 形式バリデーション付き | 🔴 | ✅ | INFRA-02 | [IAM-D-02](./01-contexts.md#CTX-1) |
| IAM-D-03 | HashedPassword 値オブジェクト | BCryptハッシュ値保持 | 🔴 | ✅ | INFRA-02 | [IAM-D-03](./01-contexts.md#CTX-1) |
| IAM-D-04 | UserStatus 列挙型 | ACTIVE, LOCKED, SUSPENDED | 🔴 | ✅ | INFRA-02 | [IAM-D-04](./01-contexts.md#CTX-1) |
| IAM-D-05 | User 集約ルート | iam.md セクション3.1 に準拠 | 🔴 | ✅ | IAM-D-01〜04 | [IAM-D-05](./01-contexts.md#CTX-1) |
| IAM-D-06 | RefreshToken エンティティ | iam.md セクション3.2 に準拠 | 🔴 | ✅ | IAM-D-01 | [IAM-D-06](./01-contexts.md#CTX-1) |
| IAM-D-07 | UserLoggedIn イベント | ドメインイベント | 🟡 | ✅ | IAM-D-05 | [IAM-D-07](./01-contexts.md#CTX-1) |
| IAM-D-08 | LoginFailed イベント | ドメインイベント | 🟡 | ✅ | IAM-D-05 | [IAM-D-08](./01-contexts.md#CTX-1) |
| IAM-D-09 | AccountLocked イベント | ドメインイベント | 🟡 | ✅ | IAM-D-05 | [IAM-D-09](./01-contexts.md#CTX-1) |

### 1.2 アプリケーション層

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| IAM-A-01 | UserRepository ポート | 永続化インターフェース | 🔴 | ✅ | IAM-D-05 | [IAM-A-01](./02-usecases.md#UC-0-IAM) |
| IAM-A-02 | RefreshTokenRepository ポート | 永続化インターフェース | 🔴 | ✅ | IAM-D-06 | [IAM-A-02](./02-usecases.md#UC-0-IAM) |
| IAM-A-03 | TokenGenerator ポート | JWT生成インターフェース | 🔴 | ✅ | INFRA-02 | [IAM-A-03](./02-usecases.md#UC-0-IAM) |
| IAM-A-04 | PasswordEncoder ポート | BCryptインターフェース | 🔴 | ✅ | INFRA-02 | [IAM-A-04](./02-usecases.md#UC-0-IAM) |
| IAM-A-05 | LoginUseCase | ログイン処理ユースケース | 🔴 | ✅ | IAM-A-01〜04 | [IAM-A-05](./02-usecases.md#UC-0-IAM) |
| IAM-A-06 | RefreshTokenUseCase | トークン更新ユースケース | 🔴 | ✅ | IAM-A-02, IAM-A-03 | [IAM-A-06](./02-usecases.md#UC-0-IAM) |
| IAM-A-07 | LogoutUseCase | ログアウト処理ユースケース | 🔴 | ✅ | IAM-A-02 | [IAM-A-07](./02-usecases.md#UC-0-IAM) |

### 1.3 アダプター層（インフラ）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| IAM-I-01 | users テーブル DDL | Flywayマイグレーション | 🔴 | ✅ | INFRA-04 | [IAM-I-01](./01-contexts.md#CTX-1) |
| IAM-I-02 | refresh_tokens テーブル DDL | Flywayマイグレーション | 🔴 | ✅ | INFRA-04 | [IAM-I-02](./01-contexts.md#CTX-1) |
| IAM-I-03 | JpaUserRepository | User永続化実装 | 🔴 | ✅ | IAM-A-01, IAM-I-01 | [IAM-I-03](./01-contexts.md#CTX-1) |
| IAM-I-04 | JpaRefreshTokenRepository | RefreshToken永続化実装 | 🔴 | ⬜ | IAM-A-02, IAM-I-02 | [IAM-I-04](./01-contexts.md#CTX-1) |
| IAM-I-05 | JwtTokenGenerator | JWT生成実装 (RS256) | 🔴 | ⬜ | IAM-A-03 | [IAM-I-05](./01-contexts.md#CTX-1) |
| IAM-I-06 | BCryptPasswordEncoder | BCrypt実装 (cost=12) | 🔴 | ⬜ | IAM-A-04 | [IAM-I-06](./01-contexts.md#CTX-1) |

### 1.4 アダプター層（Web API）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| IAM-W-01 | POST /auth/login エンドポイント | OpenAPI iam.yaml 準拠 | 🔴 | ⬜ | IAM-A-05 | [IAM-W-01](./03-openapi.md#API-IAM) |
| IAM-W-02 | POST /auth/refresh エンドポイント | OpenAPI iam.yaml 準拠 | 🔴 | ⬜ | IAM-A-06 | [IAM-W-02](./03-openapi.md#API-IAM) |
| IAM-W-03 | POST /auth/logout エンドポイント | OpenAPI iam.yaml 準拠 | 🔴 | ⬜ | IAM-A-07 | [IAM-W-03](./03-openapi.md#API-IAM) |
| IAM-W-04 | JwtAuthenticationFilter | Bearer Token検証フィルタ | 🔴 | ⬜ | IAM-I-05 | [IAM-W-04](./03-openapi.md#API-IAM) |
| IAM-W-05 | SecurityConfig | Spring Security設定 | 🔴 | ⬜ | IAM-W-04 | [IAM-W-05](./03-openapi.md#API-IAM) |

<a id="IMPL-TEST"></a>
### 1.5 テスト

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| IAM-T-01 | User集約ユニットテスト | 不変条件、振る舞いテスト | 🔴 | ⬜ | IAM-D-05 | [IAM-T-01](./04-test-observability.md#TEST-1) |
| IAM-T-02 | Email値オブジェクトテスト | バリデーションテスト | 🔴 | ⬜ | IAM-D-02 | [IAM-T-02](./04-test-observability.md#TEST-1) |
| IAM-T-03 | LoginUseCase ユニットテスト | 正常系/異常系 | 🔴 | ⬜ | IAM-A-05 | [IAM-T-03](./04-test-observability.md#TEST-1) |
| IAM-T-04 | UserRepository 統合テスト | DB連携テスト | 🟡 | ⬜ | IAM-I-03 | [IAM-T-04](./04-test-observability.md#TEST-1) |
| IAM-T-05 | E2E: login→refresh→logout | 認証フロー完全テスト | 🔴 | ⬜ | IAM-W-01〜03 | [IAM-T-05](./04-test-observability.md#TEST-1) |
| IAM-T-06 | Brute-force対策テスト | アカウントロック確認 | 🟡 | ⬜ | IAM-A-05 | [IAM-T-06](./04-test-observability.md#TEST-1) |

---

<a id="IMPL-BOOKING"></a>
## 2. Booking（予約）

### 2.1 ドメイン層

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| BK-D-01 | BookingId 値オブジェクト | UUID基盤のID | 🔴 | ⬜ | INFRA-02 | [BK-D-01](./01-contexts.md#CTX-2) |
| BK-D-02 | ResourceId 値オブジェクト | リソース識別ID | 🔴 | ⬜ | INFRA-02 | [BK-D-02](./01-contexts.md#CTX-2) |
| BK-D-03 | TimeRange 値オブジェクト | booking.md セクション3.2 準拠、overlapsメソッド | 🔴 | ⬜ | INFRA-02 | [BK-D-03](./01-contexts.md#CTX-2) |
| BK-D-04 | BookingStatus 列挙型 | PENDING, CONFIRMED, CANCELLED | 🔴 | ⬜ | INFRA-02 | [BK-D-04](./01-contexts.md#CTX-2) |
| BK-D-05 | Booking 集約ルート | booking.md セクション3.1 準拠、楽観的ロック | 🔴 | ⬜ | BK-D-01〜04 | [BK-D-05](./01-contexts.md#CTX-2) |
| BK-D-06 | BookingCreated イベント | ドメインイベント | 🟡 | ⬜ | BK-D-05 | [BK-D-06](./01-contexts.md#CTX-2) |
| BK-D-07 | BookingCancelled イベント | ドメインイベント | 🟡 | ⬜ | BK-D-05 | [BK-D-07](./01-contexts.md#CTX-2) |

### 2.2 アプリケーション層

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| BK-A-01 | BookingRepository ポート | 永続化インターフェース | 🔴 | ⬜ | BK-D-05 | [BK-A-01](./02-usecases.md#UC-0-BK-CREATE) |
| BK-A-02 | ConflictDetector サービス | 衝突検出ロジック | 🔴 | ⬜ | BK-D-03 | [BK-A-02](./02-usecases.md#UC-0-BK-CREATE) |
| BK-A-03 | CreateBookingUseCase | 予約作成ユースケース | 🔴 | ⬜ | BK-A-01, BK-A-02 | [BK-A-03](./02-usecases.md#UC-0-BK-CREATE) |
| BK-A-04 | UpdateBookingUseCase | 予約更新ユースケース | 🔴 | ⬜ | BK-A-01, BK-A-02 | [BK-A-04](./02-usecases.md#UC-1) |
| BK-A-05 | CancelBookingUseCase | 予約キャンセルユースケース | 🔴 | ⬜ | BK-A-01 | [BK-A-05](./02-usecases.md#UC-2) |
| BK-A-06 | GetBookingUseCase | 予約取得ユースケース | 🟡 | ⬜ | BK-A-01 | [BK-A-06](./03-openapi.md#API-BOOKING) |

### 2.3 アダプター層（インフラ）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| BK-I-01 | bookings テーブル DDL | Flywayマイグレーション | 🔴 | ⬜ | INFRA-04 | [BK-I-01](./01-contexts.md#CTX-2) |
| BK-I-02 | JpaBookingRepository | Booking永続化実装、衝突検出クエリ含む | 🔴 | ⬜ | BK-A-01, BK-I-01 | [BK-I-02](./01-contexts.md#CTX-2) |

### 2.4 アダプター層（Web API）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| BK-W-01 | POST /bookings エンドポイント | OpenAPI booking.yaml 準拠 | 🔴 | ⬜ | BK-A-03 | [BK-W-01](./03-openapi.md#API-BOOKING) |
| BK-W-02 | GET /bookings/{id} エンドポイント | OpenAPI booking.yaml 準拠 | 🔴 | ⬜ | BK-A-06 | [BK-W-02](./03-openapi.md#API-BOOKING) |
| BK-W-03 | PUT /bookings/{id} エンドポイント | OpenAPI booking.yaml 準拠 | 🔴 | ⬜ | BK-A-04 | [BK-W-03](./03-openapi.md#API-BOOKING) |
| BK-W-04 | DELETE /bookings/{id} エンドポイント | OpenAPI booking.yaml 準拠 | 🔴 | ⬜ | BK-A-05 | [BK-W-04](./03-openapi.md#API-BOOKING) |

### 2.5 テスト

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| BK-T-01 | TimeRange値オブジェクトテスト | overlaps境界値テスト | 🔴 | ⬜ | BK-D-03 | [BK-T-01](./04-test-observability.md#TEST-1) |
| BK-T-02 | Booking集約ユニットテスト | 不変条件、状態遷移テスト | 🔴 | ⬜ | BK-D-05 | [BK-T-02](./04-test-observability.md#TEST-1) |
| BK-T-03 | ConflictDetector ユニットテスト | 衝突検出ロジックテスト | 🔴 | ⬜ | BK-A-02 | [BK-T-03](./04-test-observability.md#TEST-1) |
| BK-T-04 | CreateBookingUseCase ユニットテスト | 正常系/衝突系 | 🔴 | ⬜ | BK-A-03 | [BK-T-04](./04-test-observability.md#TEST-1) |
| BK-T-05 | BookingRepository 統合テスト | DB連携、衝突クエリテスト | 🟡 | ⬜ | BK-I-02 | [BK-T-05](./04-test-observability.md#TEST-1) |
| BK-T-06 | E2E: create→update→cancel | 予約ライフサイクルテスト | 🔴 | ⬜ | BK-W-01〜04 | [BK-T-06](./04-test-observability.md#TEST-1) |

---

<a id="IMPL-PAYMENT"></a>
## 3. Payment（決済）

### 3.1 ドメイン層

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| PAY-D-01 | PaymentId 値オブジェクト | UUID基盤のID | 🔴 | ⬜ | INFRA-02 | [PAY-D-01](./01-contexts.md#CTX-3) |
| PAY-D-02 | Money 値オブジェクト | payment.md セクション3.2 準拠、amount + currency | 🔴 | ⬜ | INFRA-02 | [PAY-D-02](./01-contexts.md#CTX-3) |
| PAY-D-03 | IdempotencyKey 値オブジェクト | payment.md セクション3.3 準拠 | 🔴 | ⬜ | INFRA-02 | [PAY-D-03](./01-contexts.md#CTX-3) |
| PAY-D-04 | PaymentStatus 列挙型 | PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED | 🔴 | ⬜ | INFRA-02 | [PAY-D-04](./01-contexts.md#CTX-3) |
| PAY-D-05 | Payment 集約ルート | payment.md セクション3.1 準拠 | 🔴 | ⬜ | PAY-D-01〜04 | [PAY-D-05](./01-contexts.md#CTX-3) |
| PAY-D-06 | PaymentCreated イベント | ドメインイベント | 🟡 | ⬜ | PAY-D-05 | [PAY-D-06](./01-contexts.md#CTX-3) |
| PAY-D-07 | PaymentFailed イベント | ドメインイベント | 🟡 | ⬜ | PAY-D-05 | [PAY-D-07](./01-contexts.md#CTX-3) |

### 3.2 アプリケーション層

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| PAY-A-01 | PaymentRepository ポート | 永続化インターフェース | 🔴 | ⬜ | PAY-D-05 | [PAY-A-01](./01-contexts.md#CTX-3) |
| PAY-A-02 | IdempotencyStore ポート | 冪等性管理インターフェース | 🔴 | ⬜ | PAY-D-03 | [PAY-A-02](./01-contexts.md#CTX-3) |
| PAY-A-03 | PaymentGatewayPort | 外部ゲートウェイACL（スタブ実装） | 🔴 | ⬜ | INFRA-02 | [PAY-A-03](./01-contexts.md#CTX-3) |
| PAY-A-04 | CreatePaymentUseCase | 支払い作成ユースケース（冪等性込み） | 🔴 | ⬜ | PAY-A-01〜03 | [PAY-A-04](./02-usecases.md#UC-0-PAY-CREATE) |
| PAY-A-05 | GetPaymentUseCase | 支払い取得ユースケース | 🟡 | ⬜ | PAY-A-01 | [PAY-A-05](./03-openapi.md#API-PAYMENT) |

### 3.3 アダプター層（インフラ）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| PAY-I-01 | payments テーブル DDL | Flywayマイグレーション | 🔴 | ⬜ | INFRA-04 | [PAY-I-01](./01-contexts.md#CTX-3) |
| PAY-I-02 | idempotency_records テーブル DDL | Flywayマイグレーション | 🔴 | ⬜ | INFRA-04 | [PAY-I-02](./01-contexts.md#CTX-3) |
| PAY-I-03 | JpaPaymentRepository | Payment永続化実装 | 🔴 | ⬜ | PAY-A-01, PAY-I-01 | [PAY-I-03](./01-contexts.md#CTX-3) |
| PAY-I-04 | JpaIdempotencyStore | 冪等性管理実装 | 🔴 | ⬜ | PAY-A-02, PAY-I-02 | [PAY-I-04](./01-contexts.md#CTX-3) |
| PAY-I-05 | StubPaymentGateway | スタブゲートウェイ実装 | 🔴 | ⬜ | PAY-A-03 | [PAY-I-05](./01-contexts.md#CTX-3) |

### 3.4 アダプター層（Web API）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| PAY-W-01 | POST /payments エンドポイント | OpenAPI payment.yaml 準拠、Idempotency-Key必須 | 🔴 | ⬜ | PAY-A-04 | [PAY-W-01](./03-openapi.md#API-PAYMENT) |
| PAY-W-02 | GET /payments/{id} エンドポイント | OpenAPI payment.yaml 準拠 | 🔴 | ⬜ | PAY-A-05 | [PAY-W-02](./03-openapi.md#API-PAYMENT) |

### 3.5 テスト

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| PAY-T-01 | Money値オブジェクトテスト | 境界値、通貨テスト | 🔴 | ⬜ | PAY-D-02 | [PAY-T-01](./04-test-observability.md#TEST-1) |
| PAY-T-02 | Payment集約ユニットテスト | 不変条件、状態遷移テスト | 🔴 | ⬜ | PAY-D-05 | [PAY-T-02](./04-test-observability.md#TEST-1) |
| PAY-T-03 | CreatePaymentUseCase ユニットテスト | 正常系/冪等性テスト | 🔴 | ⬜ | PAY-A-04 | [PAY-T-03](./04-test-observability.md#TEST-1) |
| PAY-T-04 | 冪等性テスト: 同一Key再送 | 同一レスポンス確認 | 🔴 | ⬜ | PAY-A-04 | [PAY-T-04](./04-test-observability.md#TEST-1) |
| PAY-T-05 | PaymentRepository 統合テスト | DB連携テスト | 🟡 | ⬜ | PAY-I-03 | [PAY-T-05](./04-test-observability.md#TEST-1) |

---

<a id="IMPL-OBS"></a>
## 4. Observability（観測性）

| ID | タスク | 詳細 | 優先度 | 状態 | 依存 | 参照 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| OBS-I-01 | ログ設定 | JSON構造化ログ、traceId/spanId/timestamp/level | 🔴 | ⬜ | INFRA-05 | [OBS-I-01](./04-test-observability.md#OBS-1) |
| OBS-I-02 | PIIマスキングフィルター | Email, IPアドレス等のマスキング | 🔴 | ⬜ | OBS-I-01 | [OBS-I-02](./04-test-observability.md#OBS-1) |
| OBS-I-03 | REDメトリクス実装 | Rate/Errors/Duration カウンター | 🟡 | ⬜ | INFRA-07 | [OBS-I-03](./04-test-observability.md#OBS-1) |
| OBS-I-04 | サービスメトリクス | iam_login_total, booking_create_total, payment_create_total | 🟡 | ⬜ | OBS-I-03 | [OBS-I-04](./04-test-observability.md#OBS-1) |
| OBS-I-05 | トレース設定 | W3C Trace Context伝播 | 🔴 | ⬜ | INFRA-05 | [OBS-I-05](./04-test-observability.md#OBS-1) |

---

## 推奨実装順序

1. **INFRA-01〜07**: プロジェクト基盤
2. **IAM-D-01〜09 → IAM-A-01〜07 → IAM-I-01〜06 → IAM-W-01〜05**: IAM完成
3. **BK-D-01〜07 → BK-A-01〜06 → BK-I-01〜02 → BK-W-01〜04**: Booking完成
4. **PAY-D-01〜07 → PAY-A-01〜05 → PAY-I-01〜05 → PAY-W-01〜02**: Payment完成
5. **OBS-I-01〜05**: 観測性仕上げ
6. **全E2Eテスト**: IAM-T-05, BK-T-06, PAY-T-04

---

## タスク数サマリー

| カテゴリ | ドメイン | アプリ | インフラ | Web API | テスト | 合計 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| プロジェクト基盤 | - | - | 7 | - | - | 7 |
| IAM | 9 | 7 | 6 | 5 | 6 | 33 |
| Booking | 7 | 6 | 2 | 4 | 6 | 25 |
| Payment | 7 | 5 | 5 | 2 | 5 | 24 |
| Observability | - | - | 5 | - | - | 5 |
| **合計** | 23 | 18 | 25 | 11 | 17 | **94** |
