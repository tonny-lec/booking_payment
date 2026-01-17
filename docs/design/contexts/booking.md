---
doc_type: "context"
id: "booking"
bounded_context: "Booking"
version: "1.0"
last_updated: "2026-01-17"
status: "stable"
---

# 1. 目的

Bookingコンテキストは、**リソース（会議室、席など）の予約管理**を担当する。

## 責務

- 予約の作成・変更・キャンセル
- 時間帯の重複（衝突）検出と防止
- 予約状態のライフサイクル管理（PENDING → CONFIRMED → CANCELLED）
- 楽観的ロックによる同時更新の競合防止

## スコープ外

- リソース（会議室、席など）のマスタ管理：別コンテキストまたは別タスク
- 支払い処理：Paymentコンテキストが担当
- 繰り返し予約（定期予約）：Phase 2以降

---

# 2. 用語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **Booking**：リソースの特定時間帯の利用権を表す集約
  - **BookingStatus**：PENDING | CONFIRMED | CANCELLED
  - **TimeRange**：開始時刻（startAt）と終了時刻（endAt）のペア
  - **ResourceId**：予約対象のリソースを識別するID

---

# 3. 集約一覧（Aggregate Catalog）

## 3.1 Booking（集約ルート）

```
Booking (Aggregate Root) {
  id: BookingId (UUID)
  userId: UserId (UUID)
  resourceId: ResourceId (UUID)
  timeRange: TimeRange (value object)
  status: BookingStatus (PENDING | CONFIRMED | CANCELLED)
  note: String? (max: 500)
  version: Integer (楽観的ロック用)
  cancelledAt: DateTime?
  cancelReason: String?
  createdAt: DateTime
  updatedAt: DateTime
}
```

### 不変条件

1. `timeRange.startAt < timeRange.endAt`（開始は終了より前）
2. `timeRange.startAt >= now`（過去の予約は不可、作成時点で検証）
3. 同一`resourceId`で重複する`TimeRange`を持つ有効な予約（PENDING/CONFIRMED）は存在不可
4. `version`は更新のたびにインクリメント

### 振る舞い

- `create(userId, resourceId, timeRange, note)`: 新規予約作成
- `updateTimeRange(newTimeRange, expectedVersion)`: 時間範囲の変更
- `confirm()`: PENDING → CONFIRMED（支払い完了時）
- `cancel(reason)`: PENDING/CONFIRMED → CANCELLED

### 状態遷移

```
    create()
       │
       ▼
    PENDING ──────────────→ CONFIRMED
       │      confirm()         │
       │                        │
       │ cancel()               │ cancel()
       ▼                        ▼
   CANCELLED ←────────────── CANCELLED
```

## 3.2 TimeRange（値オブジェクト）

```
TimeRange {
  startAt: DateTime
  endAt: DateTime

  // 不変条件
  invariant: startAt < endAt

  // 振る舞い
  overlaps(other: TimeRange): Boolean
  contains(point: DateTime): Boolean
  duration(): Duration
}
```

### 重複判定アルゴリズム

```
overlaps(a, b) = a.startAt < b.endAt AND b.startAt < a.endAt
```

**注意**：隣接する予約（A.endAt == B.startAt）は重複しない

---

# 4. Context Map

```
┌─────────────────┐
│       IAM       │
│   (Identity)    │
└────────┬────────┘
         │ AccessToken（認証）
         ▼
┌─────────────────────────────────────────┐
│              Booking                     │
│                                          │
│  ・予約作成/変更/キャンセル               │
│  ・衝突検出                              │
│  ・状態管理                              │
└────────┬─────────────────┬──────────────┘
         │                 │
         │ BookingCreated  │ BookingCancelled
         │ BookingConfirmed│
         ▼                 ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Payment   │     │    Audit    │     │Notification │
│             │     │             │     │             │
│ 支払い処理   │     │ 監査記録    │     │ 通知送信    │
└─────────────┘     └─────────────┘     └─────────────┘
```

## 関係性

| 関係 | 種別 | 説明 |
|------|------|------|
| IAM → Booking | Customer-Supplier | IAMがAccessTokenを提供、Bookingが検証して使用 |
| Booking → Payment | Publisher-Subscriber | BookingCreatedイベントでPaymentが支払い処理を開始 |
| Booking → Audit | Publisher-Subscriber | 予約操作イベントをAuditが購読して記録 |
| Booking → Notification | Publisher-Subscriber | 予約イベントをNotificationが購読して通知 |
| Payment → Booking | Conformist | PaymentCapturedでBookingがCONFIRMEDに遷移 |

## 統合パターン

- **IAM との統合**：
  - `Authorization: Bearer <token>` ヘッダーからAccessTokenを取得
  - トークンの `sub` クレームからuserIdを抽出
  - 予約の所有者（userId一致）のみ操作可能

- **Payment との統合**：
  - BookingCreated イベント → Payment がPENDING状態の支払いを作成
  - PaymentCaptured イベント → Booking がCONFIRMEDに状態遷移

- **Audit との統合**：
  - すべての予約操作イベントを発行
  - Auditコンテキストがイベントを購読して監査ログを記録

---

# 5. 永続化

## 5.1 bookings テーブル

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | UUID | PK | 予約ID |
| user_id | UUID | NOT NULL | ユーザーID |
| resource_id | UUID | NOT NULL | リソースID |
| start_at | TIMESTAMP | NOT NULL | 開始時刻 |
| end_at | TIMESTAMP | NOT NULL | 終了時刻 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING/CONFIRMED/CANCELLED |
| note | VARCHAR(500) | NULL | メモ |
| version | INTEGER | NOT NULL, DEFAULT 1 | 楽観的ロック用 |
| cancelled_at | TIMESTAMP | NULL | キャンセル日時 |
| cancel_reason | VARCHAR(500) | NULL | キャンセル理由 |
| created_at | TIMESTAMP | NOT NULL | 作成日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |

**インデックス：**
- `idx_bookings_user_id` ON bookings(user_id) - ユーザーの予約一覧取得
- `idx_bookings_resource_time` ON bookings(resource_id, start_at, end_at) - 衝突検出用
- `idx_bookings_status_start` ON bookings(status, start_at) - ステータス×日時フィルタ

**制約：**
- `CHECK (start_at < end_at)` - 時間範囲の整合性
- `CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))` - ステータス値

## 5.2 衝突検出クエリ

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

# 6. ドメインイベント

## 6.1 BookingCreated

予約が新規作成されたときに発行。

```
BookingCreated {
  eventId: UUID
  aggregateId: BookingId
  occurredAt: DateTime
  payload: {
    bookingId: UUID
    userId: UUID
    resourceId: UUID
    startAt: DateTime
    endAt: DateTime
    status: "PENDING"
    note: String?
  }
}
```

**購読者：** Payment（支払い作成）、Audit（監査記録）、Notification（予約確認通知）

## 6.2 BookingUpdated

予約が変更されたときに発行。

```
BookingUpdated {
  eventId: UUID
  aggregateId: BookingId
  occurredAt: DateTime
  payload: {
    bookingId: UUID
    userId: UUID
    changes: {
      startAt?: { before: DateTime, after: DateTime }
      endAt?: { before: DateTime, after: DateTime }
      note?: { before: String?, after: String? }
    }
    version: Integer
  }
}
```

**購読者：** Audit（監査記録）、Notification（変更通知）

## 6.3 BookingConfirmed

予約が確定（支払い完了）したときに発行。

```
BookingConfirmed {
  eventId: UUID
  aggregateId: BookingId
  occurredAt: DateTime
  payload: {
    bookingId: UUID
    userId: UUID
    resourceId: UUID
    startAt: DateTime
    endAt: DateTime
    confirmedAt: DateTime
  }
}
```

**購読者：** Audit（監査記録）、Notification（予約確定通知）、Ledger（売上記録）

## 6.4 BookingCancelled

予約がキャンセルされたときに発行。

```
BookingCancelled {
  eventId: UUID
  aggregateId: BookingId
  occurredAt: DateTime
  payload: {
    bookingId: UUID
    userId: UUID
    resourceId: UUID
    previousStatus: "PENDING" | "CONFIRMED"
    cancelReason: String?
    cancelledAt: DateTime
  }
}
```

**購読者：** Payment（返金処理、CONFIRMEDの場合）、Audit（監査記録）、Notification（キャンセル通知）

---

# 7. 非機能（SLO/Obs/Sec）

## 7.1 SLO（Service Level Objectives）

| SLI | 目標値 | 測定方法 |
|-----|--------|----------|
| 可用性 | 99.9% | 成功レスポンス / 総リクエスト |
| レイテンシ（p99） | < 300ms | 予約作成処理時間の99パーセンタイル |
| 衝突検出精度 | 100% | 重複予約が発生しないこと |
| エラー率 | < 0.1% | 5xx エラー / 総リクエスト |

## 7.2 Observability

- **詳細**：`docs/design/observability.md`
- **主要メトリクス**：
  - `booking_create_total{status}` - 予約作成試行数
  - `booking_create_duration_seconds{status}` - 予約作成処理時間
  - `booking_conflict_total{resource_id}` - 衝突発生数
  - `booking_active_count{status}` - アクティブ予約数

## 7.3 Security

- **詳細**：`docs/design/security.md`
- **主要対策**：
  - 認証：AccessToken（JWT）必須
  - 認可：予約所有者（userId一致）のみ操作可能
  - 監査：全操作を監査ログに記録
  - PII：予約データにPIIは含まない（userIdはUUID）

---

# 8. ADRリンク

| ADR | タイトル | 状態 |
|-----|----------|------|
| ADR-004 | 予約衝突検出戦略 | 作成予定 |
| ADR-005 | 楽観的ロックの採用理由 | 作成予定 |
| ADR-006 | 予約ステータス遷移の設計 | 作成予定 |

---

# 9. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| 楽観的ロック | 予約更新は低頻度、悲観的ロックはスケーラビリティに影響 | 一般的なパターン |
| TimeRange重複判定 | 区間の重複判定の標準アルゴリズム | 数学的に正確 |
| 状態遷移の設計 | 予約・決済システムの一般的なパターン | 推論を含む |
| インデックス設計 | 衝突検出クエリの性能要件 | 実測で調整が必要 |

---

# 10. 未決事項

| 項目 | 内容 | 優先度 | 担当 |
|------|------|--------|------|
| 予約可能な最大期間 | 1回の予約で予約可能な最大時間（例：8時間） | 中 | 未定 |
| 予約可能な先行期間 | 何日先まで予約可能か（例：30日） | 中 | 未定 |
| キャンセルポリシー | キャンセル期限、キャンセル料の設計 | 高 | Slice B |
| 繰り返し予約 | 週次/月次の定期予約 | 低 | Phase 2 |
| リソース管理 | リソースのマスタ管理（CRUD） | 中 | 別タスク |
| 予約の自動キャンセル | 支払いタイムアウト時の自動キャンセル | 高 | Slice B |
