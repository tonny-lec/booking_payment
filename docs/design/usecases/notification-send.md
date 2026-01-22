---
doc_type: "usecase"
id: "notification-send"
bounded_context: "Notification"
related_features: ["contract-first", "observability", "event-driven"]
related_skills: ["messaging", "retry", "template-rendering"]
status: "draft"
---

# 1. 目的 / 背景

## 目的
ドメインイベントに応じて、ユーザーに適切なチャネル（EMAIL/PUSH/SMS）で通知を配信する。

## 背景
- 予約・決済システムでは、重要なイベント（予約確定、支払い完了、アカウントロック等）をユーザーに通知する必要がある
- 複数チャネルをサポートし、ユーザーの設定に応じた配信を実現
- At-Least-Once配信を保証し、配信失敗時のリトライ戦略を明確化

## ユースケース概要
1. 他BCからドメインイベントを受信（または内部コマンドを受付）
2. 通知テンプレートを選択し、コンテンツを生成
3. ユーザーの通知設定に基づき、配信チャネルを決定
4. 外部プロバイダーを通じて通知を配信
5. 配信結果を記録し、失敗時はリトライをスケジュール

---

# 2. ユビキタス言語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **Notification**：ユーザーへの通知メッセージ（集約ルート）
  - **NotificationChannel**：配信チャネル（EMAIL | PUSH | SMS）
  - **NotificationStatus**：通知の状態（PENDING | SENT | DELIVERED | FAILED）
  - **DeliveryAttempt**：配信試行記録
  - **NotificationTemplate**：通知テンプレート（言語・チャネル別）
  - **NotificationPreference**：ユーザーの通知設定

---

# 3. 依存関係（Context Map）

```
                    ┌─────────────────────────────────────────┐
                    │              Notification               │
                    │            (Event Consumer)             │
                    └───────────────────┬─────────────────────┘
                                        │
        ┌───────────────┬───────────────┼───────────────┬───────────────┐
        │               │               │               │               │
        ▼               ▼               ▼               ▼               ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
   │   IAM   │    │ Booking │    │ Payment │    │  Audit  │    │ External│
   │         │    │         │    │         │    │         │    │ Provider│
   └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
   UserLoggedIn   BookingCreated PaymentCaptured AuditLogRecorded  EMAIL/
   AccountLocked  BookingCancelled PaymentFailed               PUSH/SMS
   LoginFailed
```

## 関係性

| 関係 | 種別 | 説明 |
|------|------|------|
| IAM → Notification | Publisher-Subscriber | 認証イベント（AccountLocked、LoginFailed等）を受信 |
| Booking → Notification | Publisher-Subscriber | 予約イベント（BookingCreated/Cancelled）を受信 |
| Payment → Notification | Publisher-Subscriber | 決済イベント（PaymentCaptured/Failed）を受信 |
| Audit → Notification | Publisher-Subscriber | 監査イベント（AuditLogRecorded）を受信 |
| Notification → External Provider | ACL (Anti-Corruption Layer) | 外部プロバイダー（SendGrid/FCM/Twilio等）を抽象化 |

## 統合パターン

- **イベント駆動アーキテクチャ**：
  - 各BCからのドメインイベントをKafkaトピック経由で購読
  - イベントハンドラーが各イベントタイプに応じた通知テンプレートを選択
  - 非同期処理により、イベント発行元BCへの影響を最小化

- **外部プロバイダーとの統合（ACL）**：
  - 各プロバイダーの差異を抽象化（SendGrid/SES、FCM/APNs、Twilio等）
  - 統一されたインターフェース（`NotificationProvider`）でドメイン層から利用
  - プロバイダー固有のエラーハンドリングとリトライ戦略を適用

---

# 4. 入出力（Command/Query/Event）

## Event Trigger: ドメインイベント受信

```
# 受信するイベント例
BookingCreated {
  bookingId: UUID
  userId: UUID
  resourceId: UUID
  timeRange: { startAt: DateTime, endAt: DateTime }
  occurredAt: DateTime
}

PaymentCaptured {
  paymentId: UUID
  bookingId: UUID
  userId: UUID
  amount: { value: Decimal, currency: String }
  occurredAt: DateTime
}

AccountLocked {
  userId: UUID
  reason: Enum
  lockedUntil: DateTime
  occurredAt: DateTime
}
```

## Command: SendNotificationCommand（内部用）

```
SendNotificationCommand {
  notificationId: UUID (optional, for idempotency)
  userId: UUID (required)
  templateId: String (required, e.g., "booking.created", "payment.captured")
  channel: NotificationChannel? (optional, use preference if not specified)
  parameters: Map<String, Any> (required, template variables)
  priority: Priority (HIGH | NORMAL | LOW, default: NORMAL)
  scheduledAt: DateTime? (optional, for delayed delivery)
}
```

## Response: NotificationCreated

```
NotificationCreated {
  notificationId: UUID
  status: NotificationStatus
  channel: NotificationChannel
  scheduledAt: DateTime
}
```

## Domain Event: NotificationSent

```
NotificationSent {
  eventId: UUID
  notificationId: UUID
  userId: UUID
  channel: NotificationChannel
  templateId: String
  sentAt: DateTime
  deliveryAttempt: Integer
}
```

## Domain Event: NotificationDelivered

```
NotificationDelivered {
  eventId: UUID
  notificationId: UUID
  channel: NotificationChannel
  deliveredAt: DateTime
  providerMessageId: String
}
```

## Domain Event: NotificationFailed

```
NotificationFailed {
  eventId: UUID
  notificationId: UUID
  channel: NotificationChannel
  reason: Enum (PROVIDER_ERROR | INVALID_RECIPIENT | RATE_LIMITED | TIMEOUT)
  failedAt: DateTime
  attemptNumber: Integer
  willRetry: Boolean
  nextRetryAt: DateTime?
}
```

---

# 5. ドメインモデル（集約/不変条件）

## 集約：Notification

```
Notification (Aggregate Root) {
  id: NotificationId (UUID)
  userId: UUID
  templateId: String
  channel: NotificationChannel
  parameters: Map<String, Any>
  status: NotificationStatus
  priority: Priority
  content: NotificationContent? (rendered content)
  scheduledAt: DateTime
  sentAt: DateTime?
  deliveredAt: DateTime?
  failedAt: DateTime?
  deliveryAttempts: List<DeliveryAttempt>
  createdAt: DateTime
  updatedAt: DateTime
}
```

## 値オブジェクト：DeliveryAttempt

```
DeliveryAttempt {
  attemptNumber: Integer (1-based)
  attemptedAt: DateTime
  status: AttemptStatus (SUCCESS | FAILURE)
  providerResponse: String?
  providerMessageId: String?
  errorCode: String?
  errorMessage: String?
  durationMs: Integer
}
```

## 値オブジェクト：NotificationContent

```
NotificationContent {
  subject: String? (for EMAIL)
  body: String (required)
  htmlBody: String? (for EMAIL)
  metadata: Map<String, String>? (channel-specific)
}
```

## エンティティ：NotificationTemplate

```
NotificationTemplate {
  id: String (e.g., "booking.created")
  name: String
  channel: NotificationChannel
  locale: String (e.g., "ja", "en")
  subjectTemplate: String? (for EMAIL)
  bodyTemplate: String (Mustache/Handlebars形式)
  htmlBodyTemplate: String? (for EMAIL)
  version: Integer
  isActive: Boolean
  createdAt: DateTime
  updatedAt: DateTime
}
```

## エンティティ：NotificationPreference

```
NotificationPreference {
  userId: UUID
  channel: NotificationChannel
  eventType: String (e.g., "booking.*", "payment.*")
  enabled: Boolean
  createdAt: DateTime
  updatedAt: DateTime
}
```

## 不変条件

1. **status遷移は一方向**：PENDING → SENT → DELIVERED または PENDING → SENT → FAILED
2. **DELIVERED/FAILED後の再送不可**：最終状態に達した通知は変更不可
3. **deliveryAttempts.attemptNumber は連続**：1, 2, 3, ... と連続する
4. **リトライ上限**：deliveryAttempts.size() ≤ MAX_RETRY_COUNT (5)
5. **scheduledAt ≥ createdAt**：過去への遅延配信は不可

## 状態遷移

```
                  配信試行
PENDING ──────────────────────→ SENT
   │                              │
   │ キャンセル                    │ プロバイダー確認
   ▼                              ▼
CANCELLED                    DELIVERED
                                  │
                                  │ 配信失敗（リトライ可）
                                  ▼
                           SENT（リトライ）
                                  │
                                  │ 最終失敗（リトライ上限）
                                  ▼
                               FAILED
```

---

# 6. API（OpenAPI参照）

- SSOT：`docs/api/openapi/notification.yaml`
- エンドポイント：
  - `POST /notifications` - 通知作成（内部用）
  - `GET /notifications` - 通知一覧取得
  - `GET /notifications/{id}` - 通知詳細取得

---

# 7. 永続化

## テーブル設計（推論、実装時に検証が必要）

### notifications テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| user_id | UUID | NOT NULL, INDEX |
| template_id | VARCHAR(100) | NOT NULL |
| channel | VARCHAR(20) | NOT NULL |
| parameters | JSONB | NOT NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' |
| priority | VARCHAR(10) | NOT NULL, DEFAULT 'NORMAL' |
| subject | TEXT | NULL |
| body | TEXT | NULL |
| html_body | TEXT | NULL |
| scheduled_at | TIMESTAMP | NOT NULL |
| sent_at | TIMESTAMP | NULL |
| delivered_at | TIMESTAMP | NULL |
| failed_at | TIMESTAMP | NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### delivery_attempts テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| notification_id | UUID | FK(notifications), NOT NULL |
| attempt_number | INTEGER | NOT NULL |
| attempted_at | TIMESTAMP | NOT NULL |
| status | VARCHAR(20) | NOT NULL |
| provider_response | TEXT | NULL |
| provider_message_id | VARCHAR(255) | NULL |
| error_code | VARCHAR(50) | NULL |
| error_message | TEXT | NULL |
| duration_ms | INTEGER | NULL |

### notification_templates テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | VARCHAR(100) | PK |
| name | VARCHAR(255) | NOT NULL |
| channel | VARCHAR(20) | NOT NULL |
| locale | VARCHAR(10) | NOT NULL, DEFAULT 'ja' |
| subject_template | TEXT | NULL |
| body_template | TEXT | NOT NULL |
| html_body_template | TEXT | NULL |
| version | INTEGER | NOT NULL, DEFAULT 1 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### notification_preferences テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| user_id | UUID | NOT NULL |
| channel | VARCHAR(20) | NOT NULL |
| event_type | VARCHAR(100) | NOT NULL |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

**インデックス：**
- `notifications(user_id, created_at DESC)` - ユーザーの通知履歴取得
- `notifications(status, scheduled_at)` - 配信待ち通知の取得
- `delivery_attempts(notification_id, attempt_number)` - UNIQUE
- `notification_preferences(user_id, channel, event_type)` - UNIQUE

---

# 8. 失敗モードとリカバリ（timeout/retry/idempotency）

## 失敗モード一覧

| 失敗モード | 原因 | 対応 | リトライ |
|------------|------|------|----------|
| PROVIDER_ERROR | 外部プロバイダーのエラー | Exponential Backoff + Jitter でリトライ | ○ |
| INVALID_RECIPIENT | 無効な宛先（メールアドレス不正等） | 通知をFAILED状態に、ユーザーに連絡先更新を促す | × |
| RATE_LIMITED | プロバイダーのレート制限 | 429のRetry-Afterに従う | ○ |
| TIMEOUT | プロバイダー応答タイムアウト | リトライ、状態確認API呼び出し | ○ |
| TEMPLATE_ERROR | テンプレートレンダリング失敗 | エラーログ出力、デフォルトテンプレート使用 | × |
| PREFERENCE_DISABLED | ユーザーが通知を無効化 | 通知をスキップ、ログ記録 | × |

## Timeout設計

- **API全体のタイムアウト**：10秒
- **外部プロバイダー呼び出し**：5秒
- **テンプレートレンダリング**：1秒
- **DB接続タイムアウト**：1秒

## Retry戦略（Exponential Backoff + Jitter）

```
リトライ間隔 = min(baseDelay * 2^attemptNumber + random(0, jitter), maxDelay)

baseDelay = 30秒
jitter = 10秒
maxDelay = 1時間
maxRetries = 5

例：
- 1回目リトライ: 30 * 2^1 + random(0,10) = 60〜70秒後
- 2回目リトライ: 30 * 2^2 + random(0,10) = 120〜130秒後
- 3回目リトライ: 30 * 2^3 + random(0,10) = 240〜250秒後
- 4回目リトライ: 30 * 2^4 + random(0,10) = 480〜490秒後
- 5回目リトライ: 30 * 2^5 + random(0,10) = 960〜970秒後
- 5回失敗後: FAILED状態に遷移
```

## Dead Letter Queue (DLQ)

- 最終失敗した通知はDLQに移動
- 運用チームがDLQを監視し、手動対応を判断
- DLQ内の通知は30日間保持後、アーカイブ

## Idempotency

- `notificationId` をクライアントが指定可能
- 同一 `notificationId` での再送は、既存の通知状態を返却
- イベントハンドラーでは `eventId` を使用して重複処理を防止

---

# 9. 観測性（logs/metrics/traces）

## ログ

| イベント | ログレベル | 必須フィールド | PIIポリシー |
|----------|------------|----------------|-------------|
| NotificationCreated | INFO | traceId, notificationId, userId, templateId, channel | - |
| NotificationSending | DEBUG | traceId, notificationId, attemptNumber | - |
| NotificationSent | INFO | traceId, notificationId, channel, durationMs | - |
| NotificationDelivered | INFO | traceId, notificationId, providerMessageId | - |
| NotificationFailed | WARN | traceId, notificationId, errorCode, willRetry | - |
| NotificationRetryScheduled | INFO | traceId, notificationId, nextRetryAt | - |
| ProviderError | ERROR | traceId, provider, errorCode, errorMessage | レスポンス本文にPIIがあればマスク |

## メトリクス

| メトリクス名 | 型 | ラベル | 説明 |
|--------------|-----|--------|------|
| `notification_sent_total` | Counter | channel, template_id, status=[success\|failure] | 送信試行総数 |
| `notification_delivered_total` | Counter | channel, template_id | 配信確認総数 |
| `notification_failed_total` | Counter | channel, reason | 最終失敗総数 |
| `notification_send_duration_seconds` | Histogram | channel, provider | 送信処理時間 |
| `notification_retry_total` | Counter | channel, attempt_number | リトライ発生数 |
| `notification_queue_size` | Gauge | channel, priority | 配信待ちキューサイズ |
| `notification_dlq_size` | Gauge | channel | DLQサイズ |

## トレース

- **SpanName**：`Notification.send`
- **必須属性**：
  - `notification.id`
  - `notification.channel`
  - `notification.template_id`
  - `notification.attempt_number`
  - `notification.status`
- **子Span**：
  - `Notification.renderTemplate` - テンプレートレンダリング
  - `Notification.checkPreference` - 通知設定確認
  - `Notification.callProvider` - 外部プロバイダー呼び出し

## SLI/SLO

| SLI | 計算式 | SLO |
|-----|--------|-----|
| 配信成功率 | delivered_total / (sent_total - preference_disabled) | ≥ 99.0% |
| 配信レイテンシ（p50） | notification_send_duration_seconds | ≤ 2秒 |
| 配信レイテンシ（p99） | notification_send_duration_seconds | ≤ 10秒 |
| DLQサイズ | notification_dlq_size | ≤ 100件 |

---

# 10. セキュリティ（authn/authz/audit/PII）

## 認証（AuthN）

- `POST /notifications` は内部サービス間通信のみ（mTLS or サービストークン）
- `GET /notifications` はユーザー認証必須（AccessToken）

## 認可（AuthZ）

| エンドポイント | 認可ルール |
|----------------|------------|
| POST /notifications | 内部サービスのみ |
| GET /notifications | 自分の通知のみ（user_id = token.sub） |
| GET /notifications/{id} | 自分の通知のみ |

## 監査

- 通知送信はすべてAuditコンテキストに記録
- 記録内容：
  - 通知ID
  - ユーザーID
  - チャネル
  - テンプレートID
  - 送信結果
  - タイムスタンプ

## PII保護

### ログ出力禁止
- メール本文
- Push通知本文
- SMS本文
- 宛先（メールアドレス、電話番号、デバイストークン）

### マスク必須
- ユーザーのメールアドレス（u***@example.com形式）
- 電話番号（最後4桁のみ表示：***-****-1234）

### テンプレートパラメータの取り扱い
- パラメータにPIIが含まれる可能性あり
- ログ出力時はパラメータをマスクまたは省略
- 例外：bookingId, paymentId等の業務IDはログ出力可

---

# 11. テスト戦略（Unit/Integration/Contract/E2E）

## Unit Tests

| テスト対象 | テストケース |
|------------|-------------|
| Notification集約 | 状態遷移、リトライ判定、不変条件検証 |
| DeliveryAttempt | 試行記録の作成、エラー情報保持 |
| NotificationTemplate | テンプレートレンダリング、変数置換 |
| RetryStrategy | Exponential Backoff計算、Jitter範囲 |

## Integration Tests

| テスト対象 | テストケース |
|------------|-------------|
| NotificationRepository | 通知の保存、検索、状態更新 |
| TemplateRepository | テンプレートの取得、バージョン管理 |
| PreferenceRepository | 設定の取得、デフォルト値適用 |
| EmailProvider (Mock) | メール送信、エラーハンドリング |
| PushProvider (Mock) | Push送信、デバイストークン無効時 |

## Contract Tests

- OpenAPI `notification.yaml` に対する契約テスト
- リクエスト/レスポンスの形式検証
- エラーレスポンスのProblemDetail形式検証

## E2E Tests

| シナリオ | 検証内容 |
|----------|----------|
| 正常配信フロー | イベント受信 → 通知作成 → 配信 → DELIVERED |
| リトライフロー | プロバイダーエラー → リトライ → 成功 |
| 最終失敗フロー | 5回リトライ失敗 → FAILED → DLQ |
| 通知無効化フロー | 設定無効 → 配信スキップ |
| 複数チャネルフロー | 同一イベントで EMAIL + PUSH 配信 |

## 境界値テスト

- リトライ上限（5回）の境界
- Exponential Backoff の最大遅延（1時間）
- 通知本文の最大長（EMAIL: 10MB, SMS: 160文字, PUSH: 4KB）

---

# 12. ADRリンク

- ADR-011: 通知リトライ戦略（作成予定）
- ADR-012: 外部通知プロバイダーの抽象化（作成予定）
- ADR-013: 通知テンプレートエンジンの選定（作成予定）

---

# 13. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| Exponential Backoff | AWS Architecture Best Practices | 外部サービス呼び出しの標準パターン |
| Jitter追加 | AWS re:Invent 2019 - Thundering Herd対策 | 同時リトライによる負荷集中を防止 |
| At-Least-Once配信 | メッセージングシステムの一般的保証 | 重複は受信側で処理（Idempotency） |
| 5回リトライ | 業界標準（推論） | 運用データで調整が必要 |
| DLQ | AWS SQS Dead Letter Queue パターン | 最終失敗の可視化と手動対応 |

---

# 14. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| 通知プロバイダー選定 | EMAIL: SendGrid/SES, PUSH: FCM/APNs, SMS: Twilio | 高 |
| テンプレートエンジン | Mustache vs Handlebars vs Thymeleaf | 中 |
| 多言語対応 | ユーザーのlocale設定に基づくテンプレート選択 | 中 |
| バッチ通知 | 複数ユーザーへの一斉通知（マーケティング等） | 低（Slice B対象外） |
| 通知履歴の保持期間 | 法的要件、ストレージコストとのバランス | 中 |
| Webhookコールバック | 外部システムへの配信状態通知 | 低 |
