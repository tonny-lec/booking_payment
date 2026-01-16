---
doc_type: "usecase"
id: "booking-create"
bounded_context: "Booking"
related_features: ["contract-first", "observability", "conflict-detection"]
related_skills: ["ddd", "optimistic-locking", "time-range-overlap"]
status: "draft"
---

# 1. 目的 / 背景

## 目的
ユーザーがリソース（会議室、席など）の特定時間帯を予約し、利用権を確保できるようにする。

## 背景
- 予約システムでは、同一リソースの同一時間帯に複数の予約が発生しないよう衝突防止が必要
- 予約は支払いと連携し、支払い完了後に確定（CONFIRMED）となる
- 楽観的ロックにより、同時更新時の競合を検出・防止

## ユースケース概要
1. ユーザーがリソースIDと時間帯を指定して予約作成
2. システムが時間帯の重複をチェック
3. 重複なし：予約を作成（PENDING状態）
4. 重複あり：409 Conflictを返却

---

# 2. ユビキタス言語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **Booking**：リソースの特定時間帯の利用権を表す集約
  - **BookingStatus**：PENDING | CONFIRMED | CANCELLED
  - **TimeRange**：開始時刻（startAt）と終了時刻（endAt）のペア
  - **ResourceId**：予約対象のリソースを識別するID

---

# 3. 依存関係（Context Map）

```
┌─────────────┐
│    IAM      │
│  (Identity) │
└──────┬──────┘
       │ AccessToken（認証）
       ▼
┌─────────────┐
│   Booking   │
│             │
└──────┬──────┘
       │ BookingCreated Event
       ▼
┌─────────────┐     ┌─────────────┐
│   Payment   │     │ Notification│
│             │     │             │
└─────────────┘     └─────────────┘
```

- **IAM → Booking**：AccessTokenでユーザーを認証・認可
- **Booking → Payment**：予約作成後、支払い処理をトリガー
- **Booking → Notification**：予約作成イベントを通知サービスに送信
- **関係タイプ**：Published Language（BookingCreated Event）

---

# 4. 入出力（Command/Query/Event）

## Command: CreateBookingCommand
```
CreateBookingCommand {
  userId: UUID (from AccessToken)
  resourceId: UUID (required)
  startAt: DateTime (required, ISO 8601)
  endAt: DateTime (required, ISO 8601)
  note: String? (optional, max: 500)
}
```

## Response: Booking
```
Booking {
  id: UUID
  userId: UUID
  resourceId: UUID
  startAt: DateTime
  endAt: DateTime
  status: BookingStatus
  note: String?
  version: Integer
  createdAt: DateTime
  updatedAt: DateTime
}
```

## Domain Event: BookingCreated
```
BookingCreated {
  eventId: UUID
  bookingId: UUID
  userId: UUID
  resourceId: UUID
  startAt: DateTime
  endAt: DateTime
  status: "PENDING"
  occurredAt: DateTime
}
```

---

# 5. ドメインモデル（集約/不変条件）

## 集約：Booking

```
Booking (Aggregate Root) {
  id: BookingId (UUID)
  userId: UserId (UUID)
  resourceId: ResourceId (UUID)
  timeRange: TimeRange {
    startAt: DateTime
    endAt: DateTime
  }
  status: BookingStatus (PENDING | CONFIRMED | CANCELLED)
  note: String? (max: 500)
  version: Integer (楽観的ロック用)
  createdAt: DateTime
  updatedAt: DateTime
}
```

## 値オブジェクト：TimeRange

```
TimeRange {
  startAt: DateTime
  endAt: DateTime

  // 不変条件
  invariant: startAt < endAt
  invariant: startAt >= now (過去の予約は不可)
}
```

## 不変条件

1. **TimeRangeの整合性**：startAt < endAt
2. **過去の予約禁止**：startAt >= 現在時刻
3. **リソース×時間帯の一意性**：同一resourceIdで重複するTimeRangeを持つ有効な予約（PENDING/CONFIRMED）は存在不可
4. **version整合性**：更新時、リクエストのversionと現在のversionが一致すること

## 状態遷移

```
    作成
     │
     ▼
  PENDING ─────────────→ CONFIRMED
     │     支払い完了        │
     │                      │
     │ キャンセル/タイムアウト   │ キャンセル
     ▼                      ▼
 CANCELLED ←───────────── CANCELLED
```

## 衝突検出アルゴリズム

2つのTimeRangeが重複するかの判定：
```
overlap(a, b) = a.startAt < b.endAt AND b.startAt < a.endAt
```

---

# 6. API（OpenAPI参照）

- SSOT：`docs/api/openapi/booking.yaml`
- エンドポイント：
  - `POST /bookings` - 予約作成
  - `GET /bookings/{id}` - 予約取得
  - `PUT /bookings/{id}` - 予約変更
  - `DELETE /bookings/{id}` - 予約キャンセル

---

# 7. 永続化

## テーブル設計（推論、実装時に検証が必要）

### bookings テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| user_id | UUID | NOT NULL, INDEX |
| resource_id | UUID | NOT NULL |
| start_at | TIMESTAMP | NOT NULL |
| end_at | TIMESTAMP | NOT NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' |
| note | VARCHAR(500) | NULL |
| version | INTEGER | NOT NULL, DEFAULT 1 |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

**インデックス：**
- `bookings(user_id)` - ユーザーの予約一覧取得
- `bookings(resource_id, start_at, end_at)` - 衝突検出用
- `bookings(status, start_at)` - ステータス×日時でのフィルタ

## 衝突検出クエリ

```sql
SELECT EXISTS (
  SELECT 1 FROM bookings
  WHERE resource_id = :resourceId
    AND status IN ('PENDING', 'CONFIRMED')
    AND start_at < :endAt
    AND end_at > :startAt
    AND id != :excludeId  -- 更新時は自身を除外
) AS has_conflict;
```

---

# 8. 失敗モードとリカバリ（timeout/retry/idempotency）

## 失敗モード一覧

| 失敗モード | HTTPステータス | 原因 | リカバリ |
|------------|----------------|------|----------|
| VALIDATION_ERROR | 400 | 入力値不正（startAt >= endAt等） | クライアントで入力値を修正 |
| UNAUTHORIZED | 401 | AccessToken無効/期限切れ | トークンをリフレッシュして再試行 |
| FORBIDDEN | 403 | 他ユーザーの予約へのアクセス | 権限を確認 |
| NOT_FOUND | 404 | 予約が見つからない | IDを確認 |
| CONFLICT | 409 | 時間帯の衝突またはバージョン不一致 | 別の時間帯を選択、または最新版を取得して再試行 |
| UNPROCESSABLE | 422 | 状態遷移不可（CANCELLED→CONFIRMED等） | 現在の状態を確認 |
| INTERNAL_ERROR | 500 | サーバー内部エラー | 指数バックオフでリトライ |

## Timeout設計

- **API全体のタイムアウト**：5秒
- **衝突検出クエリのタイムアウト**：2秒
- **DB接続タイムアウト**：1秒

## Retry戦略

- **クライアント側**：409（バージョン不一致）の場合は最新版を取得して再試行
- **サーバー側**：DB接続失敗時は最大2回リトライ（指数バックオフ）

## Idempotency

- 予約作成は冪等ではない（同一リクエストで複数予約が作成される可能性）
- 必要に応じて、クライアント側で重複チェックを行う
- 将来的にはIdempotency-Keyの導入を検討（推論）

---

# 9. 観測性（logs/metrics/traces）

## ログ

| イベント | ログレベル | 必須フィールド | 備考 |
|----------|------------|----------------|------|
| BookingCreateAttempted | INFO | traceId, userId, resourceId, timeRange | - |
| BookingCreated | INFO | traceId, bookingId, userId, resourceId | - |
| BookingConflictDetected | WARN | traceId, userId, resourceId, timeRange, conflictingBookingId | 衝突予約IDを含める |
| BookingValidationFailed | WARN | traceId, userId, reason | - |

## メトリクス

| メトリクス名 | 型 | ラベル | 説明 |
|--------------|-----|--------|------|
| `booking_create_total` | Counter | status=[success\|conflict\|error] | 予約作成試行数 |
| `booking_create_duration_seconds` | Histogram | status | 予約作成処理時間 |
| `booking_conflict_total` | Counter | resource_id | 衝突発生数（リソース別） |
| `booking_active_count` | Gauge | status | アクティブ予約数（ステータス別） |

## トレース

- **SpanName**：`Booking.create`
- **必須属性**：
  - `booking.id`（成功時）
  - `booking.resource_id`
  - `booking.status`：created | conflict | error
- **子Span**：
  - `Booking.validateTimeRange` - 時間範囲検証
  - `Booking.checkConflict` - 衝突検出
  - `Booking.persist` - 永続化
  - `Booking.publishEvent` - イベント発行

---

# 10. セキュリティ（authn/authz/audit/PII）

## 認証（AuthN）

- 有効なAccessToken（JWT）が必要
- トークンからuserIdを抽出

## 認可（AuthZ）

| 操作 | 認可ルール |
|------|----------|
| 予約作成 | 認証済みユーザーのみ |
| 予約取得 | 予約の所有者（userId一致）のみ |
| 予約変更 | 予約の所有者のみ |
| 予約キャンセル | 予約の所有者のみ |

## 監査

- 予約作成/変更/キャンセルを監査ログに記録
- 監査ログには以下を含める：
  - タイムスタンプ
  - 操作種別（CREATE/UPDATE/CANCEL）
  - ユーザーID
  - 予約ID
  - 変更内容（before/after）
  - 結果（success/failure）

## PII保護

- 予約データにPIIは含まない（userIdはUUID）
- ログにユーザー名やメールアドレスを出力しない

---

# 11. テスト戦略（Unit/Integration/Contract/E2E）

## Unit Tests

| テスト対象 | テストケース |
|------------|-------------|
| TimeRange | 正常な時間範囲、startAt >= endAtの拒否、過去日時の拒否 |
| Booking集約 | 作成、状態遷移、バージョン更新 |
| ConflictDetector | 重複判定（完全重複、部分重複、隣接、非重複） |

## Integration Tests

| テスト対象 | テストケース |
|------------|-------------|
| BookingRepository | 保存、検索、衝突検出クエリ |
| CreateBookingUseCase | 正常作成、衝突検出、バリデーションエラー |

## Contract Tests

- OpenAPI `booking.yaml` に対する契約テスト
- リクエスト/レスポンスの形式検証
- 409 Conflictレスポンスの形式検証

## E2E Tests

| シナリオ | 検証内容 |
|----------|----------|
| 正常予約作成 | 認証 → 予約作成 → レスポンス検証 |
| 衝突検出 | 予約A作成 → 重複時間で予約B作成 → 409検証 |
| 予約変更 | 予約作成 → 時間変更 → バージョン更新検証 |
| 楽観的ロック | 同時更新 → バージョン不一致検証 |

## 境界値テスト

- TimeRangeの境界（隣接する予約：A.endAt == B.startAt は衝突しない）
- 過去日時の境界（startAt == now は許可、startAt < now は拒否）
- バージョン番号の境界

---

# 12. ADRリンク

- ADR-004: 予約衝突検出戦略（作成予定）
- ADR-005: 楽観的ロックの採用理由（作成予定）
- ADR-006: 予約ステータス遷移の設計（作成予定）

---

# 13. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| 楽観的ロックの採用 | 予約更新は低頻度、悲観的ロックはスケーラビリティに影響 | 一般的なパターン |
| TimeRange重複判定 | 区間の重複判定の標準アルゴリズム | 数学的に正確 |
| 状態遷移の設計 | 予約・決済システムの一般的なパターン | 推論を含む、実運用で調整が必要 |
| インデックス設計 | 衝突検出クエリの性能要件 | 実測で調整が必要 |

---

# 14. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| 予約可能な最大期間 | 1回の予約で予約可能な最大時間（例：8時間） | 中 |
| 予約可能な先行期間 | 何日先まで予約可能か（例：30日） | 中 |
| キャンセルポリシー | キャンセル期限、キャンセル料 | 高（Slice Bで対応） |
| 繰り返し予約 | 週次/月次の定期予約 | 低（Slice C以降） |
| リソース管理 | リソースのマスタ管理API | 中（別ユースケースとして切り出し） |
