---
doc_type: "adr"
id: "0004"
title: "予約衝突検出戦略"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team"]
---

# ADR-004: 予約衝突検出戦略

## ステータス

Accepted

## コンテキスト

Bookingコンテキストでは、同一リソースに対する時間帯の重複（衝突）を防止する必要がある。衝突検出は予約システムのコア機能であり、100%の精度が求められる。

### 検討すべき要件

1. **精度**: 重複予約を100%防止（ビジネスクリティカル）
2. **パフォーマンス**: p99 < 300ms での検出
3. **スケーラビリティ**: 高負荷時でも衝突を見逃さない
4. **シンプルさ**: 実装・保守が容易

### TimeRange重複の定義

```
TimeRange A と B が重複する条件:
  A.startAt < B.endAt AND B.startAt < A.endAt

例:
  A: 10:00-11:00, B: 10:30-11:30 → 重複（✗）
  A: 10:00-11:00, B: 11:00-12:00 → 隣接、重複なし（✓）
  A: 10:00-12:00, B: 10:30-11:30 → A が B を包含、重複（✗）
```

## 決定

**データベースレベルでの衝突検出 + アプリケーション層での事前チェックを採用する。**

### 採用する構成

| 層 | 役割 | 実装 |
|----|------|------|
| Application層 | 事前チェック（楽観的） | SELECTクエリで衝突候補を確認 |
| Database層 | 最終保証（悲観的） | INSERT/UPDATE時にEXISTSチェック |
| Database層 | 補助制約 | 部分ユニーク制約（可能な場合） |

### 衝突検出クエリ

```sql
-- 衝突検出（事前チェック + トランザクション内確認）
SELECT EXISTS (
  SELECT 1 FROM bookings
  WHERE resource_id = :resourceId
    AND status IN ('PENDING', 'CONFIRMED')
    AND start_at < :endAt
    AND end_at > :startAt
    AND id != :excludeId  -- 更新時は自身を除外
  FOR UPDATE  -- 行ロックで並行処理を制御
) AS has_conflict;
```

### インデックス設計

```sql
-- 衝突検出用複合インデックス
CREATE INDEX idx_bookings_conflict_check
ON bookings (resource_id, status, start_at, end_at)
WHERE status IN ('PENDING', 'CONFIRMED');
```

## 検討した選択肢

### 選択肢1: アプリケーション層のみで検出（不採用）

```java
public boolean hasConflict(ResourceId resourceId, TimeRange range) {
    List<Booking> existing = repository.findByResourceIdAndStatus(...);
    return existing.stream().anyMatch(b -> b.getTimeRange().overlaps(range));
}
```

**メリット:**
- 実装がシンプル
- DBに依存しないロジック

**デメリット:**
- レースコンディションに脆弱（チェック後・保存前に別の予約が入る可能性）
- 100%の衝突防止を保証できない

```
Thread A: check → no conflict     → save ✓
Thread B:          check → no conflict → save ✓  ← 両方通過！
```

### 選択肢2: 悲観的ロック（テーブルロック）（不採用）

```sql
LOCK TABLE bookings IN EXCLUSIVE MODE;
-- 衝突チェック
-- INSERT/UPDATE
COMMIT;
```

**メリット:**
- 100%の衝突防止を保証

**デメリット:**
- スケーラビリティが著しく低下
- 高負荷時にタイムアウト多発
- 異なるリソースの予約も直列化される

### 選択肢3: 行レベルロック + トランザクション（採用）

```sql
BEGIN;

-- 衝突候補を行ロックで保護
SELECT id FROM bookings
WHERE resource_id = :resourceId
  AND status IN ('PENDING', 'CONFIRMED')
  AND start_at < :endAt
  AND end_at > :startAt
FOR UPDATE;

-- 結果が0件なら衝突なし → INSERT/UPDATE
INSERT INTO bookings (...) VALUES (...);

COMMIT;
```

**メリット:**
- 100%の衝突防止を保証
- 異なるリソースは並行処理可能
- PostgreSQLで標準的なパターン

**デメリット:**
- トランザクション管理が必要
- デッドロックの可能性（同一リソースへの並行アクセス）

### 選択肢4: Exclusion Constraint（PostgreSQL固有）（将来検討）

```sql
CREATE EXTENSION btree_gist;

ALTER TABLE bookings
ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING GIST (
  resource_id WITH =,
  tstzrange(start_at, end_at, '[)') WITH &&
) WHERE (status IN ('PENDING', 'CONFIRMED'));
```

**メリット:**
- DBレベルで衝突を完全に防止
- アプリケーション層のチェック不要

**デメリット:**
- PostgreSQL固有（ポータビリティ低下）
- 部分制約（WHERE句）のサポートが複雑
- マイグレーション時の考慮事項

**結論:** 将来的なオプションとして記録。現時点では選択肢3を採用。

## 結果

### 正の影響

1. **100%の衝突防止**: トランザクション + 行ロックで保証
2. **スケーラビリティ**: リソース単位でのロックにより並行性を維持
3. **ポータビリティ**: 標準SQLで実装可能
4. **デバッグ容易性**: 明示的なロジックで追跡可能

### 負の影響

1. **デッドロックリスク**: 同一リソースへの高頻度アクセス
   - **緩和策**: デッドロック検知 + リトライロジック（最大3回）
2. **レイテンシ増加**: FOR UPDATEによるロック待ち
   - **緩和策**: タイムアウト設定（5秒）
3. **複雑性**: トランザクション管理のオーバーヘッド
   - **緩和策**: リポジトリ層でカプセル化

### 実装例

```java
@Repository
public class BookingRepositoryImpl implements BookingRepository {

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking save(Booking booking) {
        // 1. 衝突チェック（行ロック付き）
        boolean hasConflict = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
              SELECT 1 FROM bookings
              WHERE resource_id = ?
                AND status IN ('PENDING', 'CONFIRMED')
                AND start_at < ?
                AND end_at > ?
                AND id != ?
              FOR UPDATE NOWAIT
            )
            """,
            Boolean.class,
            booking.getResourceId(),
            booking.getTimeRange().getEndAt(),
            booking.getTimeRange().getStartAt(),
            booking.getId()
        );

        if (hasConflict) {
            throw new BookingConflictException(booking.getResourceId());
        }

        // 2. 保存
        return doSave(booking);
    }
}
```

### エラーハンドリング

| 状況 | 例外 | HTTPステータス |
|------|------|----------------|
| 衝突検出 | BookingConflictException | 409 Conflict |
| ロック待ちタイムアウト | LockTimeoutException | 503 Service Unavailable |
| デッドロック | DeadlockException | 503 + リトライ |

## 関連決定

- ADR-005: 楽観的ロックの採用理由
- ADR-006: 予約ステータス遷移の設計

## 参考資料

- PostgreSQL: Explicit Locking
- Martin Fowler: Pessimistic Offline Lock
- DDD: Aggregate Invariants
