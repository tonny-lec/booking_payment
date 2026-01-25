---
doc_type: "usecase"
id: "audit-record"
bounded_context: "Audit"
related_features: ["contract-first", "observability", "event-driven"]
related_skills: ["ddd", "event-sourcing", "security"]
status: "draft"
---

# 1. 目的 / 背景

## 目的
システム全体のドメインイベントを購読し、改ざん不可能な監査ログとして記録する。

## 背景
- コンプライアンス・法規制対応のため、全操作の監査証跡が必要
- セキュリティインシデント発生時の調査に監査ログが不可欠
- 監査ログは追記専用（Append-Only）で改ざん防止が必須
- 各Bounded Contextからのドメインイベントを統一形式で記録

## ユースケース概要
1. 他BCからドメインイベントを受信（Kafka経由）
2. イベントを標準化された監査ログ形式に変換
3. 重複チェック（eventIdで判定）
4. チェックサム計算（改ざん検知用）
5. 追記専用テーブルに永続化
6. AuditLogRecordedイベントを発行

---

# 2. ユビキタス言語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **AuditLog**：監査ログエントリを表す集約（追記専用）
  - **Actor**：操作を実行した主体（USER | SYSTEM | ADMIN）
  - **Action**：実行された操作（CREATE | UPDATE | DELETE | READ | LOGIN | LOGOUT等）
  - **Resource**：操作対象のリソース（Booking, Payment, User等）
  - **Checksum**：改ざん検知用のSHA-256ハッシュ
  - **ChainHash**：連鎖ハッシュ（前ログのchecksumを含む）

---

# 3. 依存関係（Context Map）

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│       IAM       │     │     Booking     │     │     Payment     │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │ UserLoggedIn          │ BookingCreated        │ PaymentCreated
         │ LoginFailed           │ BookingUpdated        │ PaymentCaptured
         │ AccountLocked         │ BookingCancelled      │ PaymentRefunded
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────────────────┐
                    │              Audit                   │
                    │                                      │
                    │  ・イベント購読                       │
                    │  ・監査ログ記録                       │
                    │  ・改ざん検知                        │
                    └────────┬────────────────────────────┘
                             │
                             │ AuditLogRecorded
                             ▼
                    ┌─────────────────┐
                    │   Monitoring    │
                    │  (異常検知)      │
                    └─────────────────┘
```

## 関係性

| 関係 | 種別 | 説明 |
|------|------|------|
| IAM → Audit | Publisher-Subscriber | 認証イベントを購読して記録 |
| Booking → Audit | Publisher-Subscriber | 予約イベントを購読して記録 |
| Payment → Audit | Publisher-Subscriber | 決済イベントを購読して記録 |
| Notification → Audit | Publisher-Subscriber | 通知イベントを購読して記録 |
| Audit → Monitoring | Publisher-Subscriber | 異常検知用にAuditLogRecordedを発行 |

---

# 4. 入出力（Command/Query/Event）

## Event Trigger: ドメインイベント受信

```
# 受信するイベント例
BookingCreated {
  eventId: UUID
  aggregateId: BookingId
  occurredAt: DateTime
  payload: {
    bookingId: UUID
    userId: UUID
    resourceId: UUID
    timeRange: { startAt: DateTime, endAt: DateTime }
  }
}

PaymentCaptured {
  eventId: UUID
  aggregateId: PaymentId
  occurredAt: DateTime
  payload: {
    paymentId: UUID
    bookingId: UUID
    userId: UUID
    capturedAmount: Integer
    currency: String
  }
}

UserLoggedIn {
  eventId: UUID
  aggregateId: UserId
  occurredAt: DateTime
  payload: {
    userId: UUID
    ipAddress: String (マスク必須)
    userAgent: String
  }
}
```

## Command: RecordAuditLogCommand（内部用）

```
RecordAuditLogCommand {
  eventId: UUID (required, 元イベントのID)
  eventType: String (required, e.g., "BookingCreated")
  occurredAt: DateTime (required)

  // Actor情報
  actorId: UUID? (ユーザーID、システムの場合はnull)
  actorType: ActorType (USER | SYSTEM | ADMIN)
  actorIp: String? (マスク済みIPアドレス)

  // Action情報
  action: Action (CREATE | UPDATE | DELETE | READ | LOGIN | LOGOUT)
  actionCategory: ActionCategory (DATA | AUTH | CONFIG | ADMIN)

  // Resource情報
  resourceType: String (required, e.g., "Booking")
  resourceId: UUID (required)
  resourceContext: String (required, e.g., "Booking")

  // 変更詳細
  changes: Map<String, ChangeDetail>? (変更前後の値)
  metadata: Map<String, String>? (追加情報)
}
```

## Response: AuditLog

```
AuditLog {
  id: UUID
  eventId: UUID
  eventType: String
  occurredAt: DateTime
  recordedAt: DateTime
  actorId: UUID?
  actorType: String
  actorIp: String?
  action: String
  actionCategory: String
  resourceType: String
  resourceId: UUID
  resourceContext: String
  changes: Map<String, ChangeDetail>?
  metadata: Map<String, String>?
  checksum: String
  previousLogId: UUID?
}
```

## Domain Event: AuditLogRecorded

```
AuditLogRecorded {
  eventId: UUID
  aggregateId: AuditLogId
  occurredAt: DateTime
  payload: {
    auditLogId: UUID
    eventType: String
    actorId: UUID?
    actorType: String
    action: String
    resourceType: String
    resourceId: UUID
    recordedAt: DateTime
  }
}
```

**購読者：** 監視システム（異常検知）

---

# 5. ドメインモデル（集約/不変条件）

## 集約：AuditLog（追記専用）

```
AuditLog (Aggregate Root, Append-Only) {
  id: AuditLogId (UUID)
  eventId: UUID
  eventType: String
  occurredAt: DateTime
  recordedAt: DateTime

  // Actor
  actorId: UUID?
  actorType: ActorType
  actorIp: String?

  // Action
  action: Action
  actionCategory: ActionCategory

  // Resource
  resourceType: String
  resourceId: UUID
  resourceContext: String

  // Details
  changes: Map<String, ChangeDetail>?
  metadata: Map<String, String>?

  // Integrity
  checksum: String
  previousLogId: AuditLogId?

  // 振る舞い
  record(command: RecordAuditLogCommand): Result<AuditLog, Error>
  verify(): Boolean
}
```

## 値オブジェクト：ChangeDetail

```
ChangeDetail {
  field: String
  oldValue: String? (マスク済み)
  newValue: String? (マスク済み)
  changeType: ChangeType (ADDED | MODIFIED | REMOVED)
}
```

## 不変条件

1. **追記専用**：作成後の更新・削除は不可
2. **eventId一意**：同一eventIdの重複記録は不可（冪等性）
3. **checksum必須**：内容から計算されたSHA-256ハッシュ
4. **時系列整合性**：occurredAt <= recordedAt
5. **PIIマスク済み**：changes内のPIIは必ずマスク済み

## 記録処理フロー

```
1. ドメインイベントを受信
2. 重複チェック（eventIdで検索）
   ├─ 重複あり → 200 OK (冪等：既存結果を返却)
   └─ 重複なし
       ├─ イベントを監査ログ形式に変換
       ├─ PIIフィールドをマスク
       ├─ 前のログIDを取得（連鎖ハッシュ用）
       ├─ Checksum計算
       ├─ 追記専用テーブルに挿入
       └─ AuditLogRecordedイベントを発行
```

---

# 6. API（OpenAPI参照）

- SSOT：`docs/api/openapi/audit.yaml`
- エンドポイント：
  - `GET /audit-logs` - 監査ログ一覧取得（管理者のみ）
  - `GET /audit-logs/{id}` - 監査ログ詳細取得（管理者のみ）

**注意：** 監査ログの記録はイベント駆動で行われ、REST APIでの作成は提供しない。

---

# 7. 永続化

## audit_logs テーブル（追記専用）

```sql
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  event_id UUID UNIQUE NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  recorded_at TIMESTAMP NOT NULL DEFAULT NOW(),
  actor_id UUID,
  actor_type VARCHAR(20) NOT NULL,
  actor_ip VARCHAR(45),
  action VARCHAR(50) NOT NULL,
  action_category VARCHAR(20) NOT NULL,
  resource_type VARCHAR(50) NOT NULL,
  resource_id UUID NOT NULL,
  resource_context VARCHAR(50) NOT NULL,
  changes JSONB,
  metadata JSONB,
  checksum VARCHAR(64) NOT NULL,
  previous_log_id UUID,

  CONSTRAINT chk_actor_type CHECK (actor_type IN ('USER', 'SYSTEM', 'ADMIN')),
  CONSTRAINT chk_action_category CHECK (action_category IN ('DATA', 'AUTH', 'CONFIG', 'ADMIN'))
);
```

**インデックス：**
- `idx_audit_logs_event_id` ON audit_logs(event_id) - 重複チェック
- `idx_audit_logs_occurred_at` ON audit_logs(occurred_at DESC) - 時系列検索
- `idx_audit_logs_actor_id` ON audit_logs(actor_id) WHERE actor_id IS NOT NULL
- `idx_audit_logs_resource` ON audit_logs(resource_type, resource_id)
- `idx_audit_logs_action` ON audit_logs(action, occurred_at DESC)

## 追記専用の強制

```sql
-- 更新禁止トリガー
CREATE OR REPLACE FUNCTION prevent_audit_update()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'Audit logs are immutable. Updates are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_update();

-- 削除禁止トリガー
CREATE OR REPLACE FUNCTION prevent_audit_delete()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'Audit logs are immutable. Deletions are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_delete
BEFORE DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_delete();
```

---

# 8. 失敗モードとリカバリ（timeout/retry/idempotency）

## 失敗モード一覧

| 失敗モード | 原因 | 対応 | リトライ |
|------------|------|------|----------|
| DUPLICATE_EVENT | 同一eventIdが既に記録済み | 既存レコードを返却（冪等） | - |
| DB_CONNECTION_ERROR | DB接続障害 | DLQに退避、後続リトライ | ○ |
| CHECKSUM_CALCULATION_ERROR | ハッシュ計算失敗 | エラーログ、DLQに退避 | × |
| INVALID_EVENT_FORMAT | イベント形式不正 | エラーログ、DLQに退避 | × |
| STORAGE_FULL | ストレージ容量不足 | 緊急アラート、古いログのアーカイブ | - |

## Timeout設計

- **イベント処理タイムアウト**：5秒
- **DB接続タイムアウト**：1秒
- **Checksum計算タイムアウト**：500ms

## Retry戦略

### イベント処理
- DB接続エラー：最大3回リトライ（指数バックオフ）
- 3回失敗後：Dead Letter Queue (DLQ)に退避

### DLQ処理
- DLQ内のイベントは手動または定期ジョブで再処理
- 30日間保持後、アーカイブ

## Idempotency（冪等性）

- `eventId` で重複を検知
- 同一 `eventId` での再処理は既存レコードを返却
- イベント処理は冪等（何度実行しても同一結果）

---

# 9. 観測性（logs/metrics/traces）

## ログ

| イベント | ログレベル | 必須フィールド | 備考 |
|----------|------------|----------------|------|
| AuditLogRecording | DEBUG | traceId, eventId, eventType | - |
| AuditLogRecorded | INFO | traceId, auditLogId, eventType, resourceType | - |
| AuditLogDuplicate | INFO | traceId, eventId | 冪等リクエスト検出 |
| AuditLogRecordFailed | ERROR | traceId, eventId, errorCode, errorMessage | - |
| ChecksumVerificationFailed | CRITICAL | traceId, auditLogId | 改ざん検知 |

## メトリクス

| メトリクス名 | 型 | ラベル | 説明 |
|--------------|-----|--------|------|
| `audit_log_recorded_total` | Counter | event_type, resource_type | 記録成功数 |
| `audit_log_duplicate_total` | Counter | event_type | 重複イベント検知数 |
| `audit_log_failed_total` | Counter | event_type, error_code | 記録失敗数 |
| `audit_log_record_duration_seconds` | Histogram | event_type | 記録処理時間 |
| `audit_log_checksum_failure_total` | Counter | - | 改ざん検知数 |
| `audit_log_dlq_size` | Gauge | - | DLQサイズ |
| `audit_log_storage_bytes` | Gauge | - | ストレージ使用量 |

## トレース

- **SpanName**：`Audit.record`
- **必須属性**：
  - `audit.event_id`
  - `audit.event_type`
  - `audit.resource_type`
  - `audit.resource_id`
  - `audit.action`
- **子Span**：
  - `Audit.checkDuplicate` - 重複チェック
  - `Audit.maskPII` - PIIマスキング
  - `Audit.calculateChecksum` - Checksum計算
  - `Audit.persist` - 永続化
  - `Audit.publishEvent` - イベント発行

## SLI/SLO

| SLI | 計算式 | SLO |
|-----|--------|-----|
| 記録成功率 | recorded_total / (received_total - duplicate_total) | >= 99.99% |
| 記録レイテンシ（p99） | audit_log_record_duration_seconds | <= 1秒 |
| データ整合性 | 100% - (checksum_failure_total / recorded_total) | 100% |

---

# 10. セキュリティ（authn/authz/audit/PII）

## 認証（AuthN）

- イベント受信：内部サービス間通信（mTLS）
- API閲覧：有効なAccessToken（JWT）+ 管理者権限

## 認可（AuthZ）

| エンドポイント | 認可ルール |
|----------------|------------|
| GET /audit-logs | 管理者のみ（role: ADMIN） |
| GET /audit-logs/{id} | 管理者のみ |
| イベント受信 | 内部サービスのみ |

## 監査

- 監査ログの閲覧も監査対象（監査の監査）
- 管理者のログ閲覧操作を記録

## PII保護

### マスキング必須フィールド

| フィールド | マスキング例 |
|------------|-------------|
| email | u***@example.com |
| phone | ***-****-1234 |
| ip_address | 192.168.***.*** |
| name | 田*** |
| card_number | (記録禁止) |
| password | (記録禁止) |

### ログ出力禁止項目
- パスワード・認証情報
- クレジットカード情報
- 生のIPアドレス（マスク前）

---

# 11. テスト戦略（Unit/Integration/Contract/E2E）

## Unit Tests

| テスト対象 | テストケース |
|------------|-------------|
| AuditLog.record | 正常な監査ログ記録 |
| AuditLog.record | 重複eventIdの検出（冪等性） |
| AuditLog.verify | checksum検証成功 |
| AuditLog.verify | checksum検証失敗（改ざん検知） |
| ChecksumCalculator | SHA-256ハッシュ計算 |
| PIIMasker | 各種PIIフィールドのマスキング |

## Integration Tests

| テスト対象 | テストケース |
|------------|-------------|
| AuditLogRepository | 監査ログの挿入、検索 |
| AuditLogRepository | 更新・削除の拒否確認 |
| EventHandler | ドメインイベント受信から記録まで |
| DLQHandler | DLQからの再処理 |

## Contract Tests

- OpenAPI `audit.yaml` に対する契約テスト
- GET /audit-logs のレスポンス形式検証
- 管理者権限チェックの検証

## E2E Tests

| シナリオ | 検証内容 |
|----------|----------|
| 正常記録フロー | イベント発行 → 監査ログ記録 → 検索で確認 |
| 冪等性 | 同一イベント2回発行 → 1件のみ記録 |
| 改ざん検知 | 直接DBを更新 → verify()で検知 |
| 権限検証 | 非管理者のAPI呼び出し → 403 |
| DLQフロー | DB障害 → DLQ退避 → 復旧後再処理 |

## 境界値テスト

- changes JSONBの最大サイズ
- metadata JSONBの最大サイズ
- eventTypeの最大長（100文字）

---

# 12. ADRリンク

- ADR-013: 監査ログの改ざん防止戦略（作成予定）
- ADR-014: 監査ログの保持期間とアーカイブ（作成予定）

---

# 13. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| 追記専用テーブル | 監査ログの改ざん防止の標準パターン | 業界標準 |
| SHA-256 checksum | NIST推奨のハッシュアルゴリズム | セキュリティ標準 |
| 連鎖ハッシュ | ブロックチェーン的改ざん検知 | 設計判断 |
| eventIdによる冪等性 | At-least-once配信への対応 | メッセージング標準 |
| PIIマスキング | GDPR, 個人情報保護法 | 法的要件 |

---

# 14. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| 保持期間の法的要件 | 各国・業界の法的保持期間の確認 | 高 |
| アーカイブストレージ | S3 Glacier / Azure Archive等の選定 | 中 |
| 全文検索 | Elasticsearch導入の検討 | 中 |
| エクスポート形式 | CSV / JSON / PDF対応 | 低 |
| 外部監査連携 | 外部監査システムへのエクスポート | 低 |
