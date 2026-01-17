---
doc_type: "usecase"
id: "booking-update"
bounded_context: "Booking"
related_features: ["contract-first", "observability", "optimistic-locking"]
related_skills: ["ddd", "optimistic-locking", "time-range-overlap"]
status: "draft"
---

# 1. 目的 / 背景

## 目的
ユーザーが既存の予約の時間帯やメモを変更できるようにする。

## 背景
- 予約後にスケジュール変更が発生することは一般的
- 時間帯の変更時は、新しい時間帯での衝突検出が必要
- 同時更新による競合を防ぐため、楽観的ロックを使用
- 変更履歴は監査ログに記録される

## ユースケース概要
1. ユーザーが予約ID、新しい時間帯、バージョンを指定
2. システムがバージョンを検証（楽観的ロック）
3. 新しい時間帯で衝突チェックを実行
4. 衝突なし：予約を更新、バージョンをインクリメント
5. 衝突あり：409 Conflictを返却

---

# 2. ユビキタス言語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **Booking**：リソースの特定時間帯の利用権を表す集約
  - **BookingStatus**：PENDING | CONFIRMED | CANCELLED
  - **TimeRange**：開始時刻（startAt）と終了時刻（endAt）のペア
  - **Version**：楽観的ロック用のバージョン番号

---

# 3. 依存関係（Context Map）

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
│  ・予約更新                              │
│  ・衝突検出                              │
│  ・楽観的ロック                          │
└────────┬─────────────────┬──────────────┘
         │                 │
         │ BookingUpdated  │
         ▼                 ▼
┌─────────────┐     ┌─────────────┐
│    Audit    │     │Notification │
│             │     │             │
│ 監査記録    │     │ 変更通知    │
└─────────────┘     └─────────────┘
```

## 関係性

| 関係 | 種別 | 説明 |
|------|------|------|
| IAM → Booking | Customer-Supplier | IAMがAccessTokenを提供、Bookingが検証して使用 |
| Booking → Audit | Publisher-Subscriber | BookingUpdatedイベントをAuditが購読して記録 |
| Booking → Notification | Publisher-Subscriber | 予約変更をNotificationが購読して通知 |

---

# 4. 入出力（Command/Query/Event）

## Command: UpdateBookingCommand
```
UpdateBookingCommand {
  bookingId: UUID (required, from path)
  userId: UUID (from AccessToken)
  startAt: DateTime (required, ISO 8601)
  endAt: DateTime (required, ISO 8601)
  note: String? (optional, max: 500)
  expectedVersion: Integer (required, for optimistic locking)
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
  version: Integer (incremented)
  createdAt: DateTime
  updatedAt: DateTime
}
```

## Domain Event: BookingUpdated
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

---

# 5. ドメインモデル（集約/不変条件）

## 集約：Booking（更新時の振る舞い）

```
Booking (Aggregate Root) {
  // 更新メソッド
  updateTimeRange(newTimeRange: TimeRange, expectedVersion: Integer): Result<Booking, Error>
  updateNote(note: String?, expectedVersion: Integer): Result<Booking, Error>
}
```

## 不変条件

1. **TimeRangeの整合性**：startAt < endAt
2. **過去の予約禁止**：startAt >= 現在時刻
3. **リソース×時間帯の一意性**：同一resourceIdで重複するTimeRangeを持つ有効な予約（PENDING/CONFIRMED）は存在不可（自身を除く）
4. **version一致**：リクエストのexpectedVersionと現在のversionが一致すること
5. **ステータス制約**：CANCELLED状態の予約は更新不可
6. **CONFIRMED予約の制約**：CONFIRMED状態では時間帯の変更に制限がある可能性（未決事項）

## 楽観的ロックの処理フロー

```
1. 予約を取得
2. 現在のversion == expectedVersion を検証
   ├─ 一致しない場合 → 409 Conflict (VERSION_MISMATCH)
   └─ 一致する場合
       ├─ TimeRange変更の場合 → 衝突検出（自身を除外）
       │   ├─ 衝突あり → 409 Conflict (TIME_CONFLICT)
       │   └─ 衝突なし → 更新実行
       └─ note のみ変更の場合 → 更新実行
3. version++
4. BookingUpdatedイベントを発行
```

## 衝突検出クエリ（更新時）

```sql
SELECT EXISTS (
  SELECT 1 FROM bookings
  WHERE resource_id = :resourceId
    AND status IN ('PENDING', 'CONFIRMED')
    AND start_at < :newEndAt
    AND end_at > :newStartAt
    AND id != :bookingId  -- 自身を除外
) AS has_conflict;
```

---

# 6. API（OpenAPI参照）

- SSOT：`docs/api/openapi/booking.yaml`
- エンドポイント：
  - `PUT /bookings/{id}` - 予約更新

---

# 7. 永続化

## 更新クエリ

```sql
UPDATE bookings
SET start_at = :newStartAt,
    end_at = :newEndAt,
    note = :newNote,
    version = version + 1,
    updated_at = :now
WHERE id = :bookingId
  AND version = :expectedVersion
  AND status != 'CANCELLED';
```

**注意**：`WHERE version = :expectedVersion` により楽観的ロックを実現。更新行数が0の場合はバージョン不一致。

---

# 8. 失敗モードとリカバリ（timeout/retry/idempotency）

## 失敗モード一覧

| 失敗モード | HTTPステータス | 原因 | リカバリ |
|------------|----------------|------|----------|
| VALIDATION_ERROR | 400 | 入力値不正（startAt >= endAt等） | クライアントで入力値を修正 |
| UNAUTHORIZED | 401 | AccessToken無効/期限切れ | トークンをリフレッシュして再試行 |
| FORBIDDEN | 403 | 他ユーザーの予約へのアクセス | 権限を確認 |
| NOT_FOUND | 404 | 予約が見つからない | IDを確認 |
| VERSION_MISMATCH | 409 | バージョン不一致（楽観的ロック失敗） | 最新版を取得して再試行 |
| TIME_CONFLICT | 409 | 新しい時間帯が他の予約と衝突 | 別の時間帯を選択 |
| INVALID_STATE | 422 | CANCELLED状態の予約を更新しようとした | 現在の状態を確認 |
| INTERNAL_ERROR | 500 | サーバー内部エラー | 指数バックオフでリトライ |

## Timeout設計

- **API全体のタイムアウト**：5秒
- **衝突検出クエリのタイムアウト**：2秒
- **DB接続タイムアウト**：1秒

## Retry戦略

- **クライアント側**：
  - 409 VERSION_MISMATCH：最新版を取得（GET）して再試行
  - 409 TIME_CONFLICT：リトライ不可、別の時間帯を選択
- **サーバー側**：DB接続失敗時は最大2回リトライ（指数バックオフ）

## Idempotency

- 予約更新は冪等ではない（versionが毎回変わる）
- 同一内容の更新でもversionはインクリメントされる
- クライアントはGET→PUT→確認のフローで整合性を担保

---

# 9. 観測性（logs/metrics/traces）

## ログ

| イベント | ログレベル | 必須フィールド | 備考 |
|----------|------------|----------------|------|
| BookingUpdateAttempted | INFO | traceId, bookingId, userId, newTimeRange | - |
| BookingUpdated | INFO | traceId, bookingId, userId, changes, newVersion | - |
| BookingVersionMismatch | WARN | traceId, bookingId, expectedVersion, actualVersion | 楽観的ロック失敗 |
| BookingUpdateConflict | WARN | traceId, bookingId, newTimeRange, conflictingBookingId | 時間帯衝突 |
| BookingUpdateForbidden | WARN | traceId, bookingId, requestUserId, ownerUserId | 権限エラー |

## メトリクス

| メトリクス名 | 型 | ラベル | 説明 |
|--------------|-----|--------|------|
| `booking_update_total` | Counter | status=[success\|version_mismatch\|conflict\|error] | 予約更新試行数 |
| `booking_update_duration_seconds` | Histogram | status | 予約更新処理時間 |
| `booking_version_mismatch_total` | Counter | - | バージョン不一致発生数 |
| `booking_update_conflict_total` | Counter | resource_id | 更新時の衝突発生数 |

## トレース

- **SpanName**：`Booking.update`
- **必須属性**：
  - `booking.id`
  - `booking.resource_id`
  - `booking.status`：success | version_mismatch | conflict | error
  - `booking.expected_version`
  - `booking.new_version`（成功時）
- **子Span**：
  - `Booking.validateVersion` - バージョン検証
  - `Booking.checkConflict` - 衝突検出（TimeRange変更時）
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
| 予約更新 | 予約の所有者（booking.userId == token.userId）のみ |

## 監査

- 予約更新を監査ログに記録
- 監査ログには以下を含める：
  - タイムスタンプ
  - 操作種別（UPDATE）
  - ユーザーID
  - 予約ID
  - 変更内容（before/after）
  - バージョン（before/after）
  - 結果（success/failure）

## PII保護

- 予約データにPIIは含まない（userIdはUUID）
- ログにユーザー名やメールアドレスを出力しない

---

# 11. テスト戦略（Unit/Integration/Contract/E2E）

## Unit Tests

| テスト対象 | テストケース |
|------------|-------------|
| Booking.updateTimeRange | 正常更新、バージョン不一致拒否、CANCELLED拒否 |
| Booking.updateNote | 正常更新、null許可、最大長検証 |
| TimeRange | 正常な時間範囲、startAt >= endAtの拒否、過去日時の拒否 |

## Integration Tests

| テスト対象 | テストケース |
|------------|-------------|
| BookingRepository | 楽観的ロック更新、バージョン不一致時のゼロ行更新 |
| UpdateBookingUseCase | 正常更新、衝突検出、バージョン不一致 |

## Contract Tests

- OpenAPI `booking.yaml` に対する契約テスト
- PUT /bookings/{id} のリクエスト/レスポンス形式検証
- 409 Conflictレスポンスのtypeによる判別（VERSION_MISMATCH vs TIME_CONFLICT）

## E2E Tests

| シナリオ | 検証内容 |
|----------|----------|
| 正常更新フロー | GET → PUT → バージョンインクリメント確認 |
| 楽観的ロック検証 | 同時更新 → 片方が409 VERSION_MISMATCH |
| 衝突検出 | 他予約と重複する時間帯に更新 → 409 TIME_CONFLICT |
| 権限検証 | 他ユーザーの予約更新 → 403 |

## 境界値テスト

- TimeRangeの境界（隣接する予約：A.endAt == B.startAt は衝突しない）
- 過去日時の境界（startAt == now は許可、startAt < now は拒否）
- バージョン番号の境界（Integer.MAX_VALUE のラップアラウンド検討）

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
| バージョン番号によるロック | Hibernate/JPAの標準的なパターン | `@Version`アノテーション相当 |
| 409 Conflictの使い分け | RFC 7231、ProblemDetailのtype属性で判別 | VERSION_MISMATCH vs TIME_CONFLICT |
| 変更内容のbefore/after記録 | 監査要件、デバッグ支援 | 一般的なパターン |

---

# 14. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| CONFIRMED予約の変更制限 | 支払い済み予約の時間変更可否、差額精算 | 高（Slice Bで対応） |
| 変更履歴テーブル | 変更履歴を別テーブルで管理するか | 中 |
| 部分更新 | PATCH vs PUT の選択 | 低 |
| 変更通知の詳細 | 変更内容をどこまで通知に含めるか | 中 |
| バージョンのラップアラウンド | Integer.MAX_VALUE到達時の挙動 | 低（実用上問題なし） |
