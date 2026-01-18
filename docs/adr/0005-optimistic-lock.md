---
doc_type: "adr"
id: "0005"
title: "楽観的ロックの採用理由"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team"]
---

# ADR-005: 楽観的ロックの採用理由

## ステータス

Accepted

## コンテキスト

Booking集約は複数のユーザーやシステムから同時に更新される可能性がある。例えば：

- ユーザーAが予約時間を変更中に、ユーザーB（管理者）がキャンセル
- 支払いシステムがCONFIRMEDに遷移中に、ユーザーが更新
- 同一ユーザーが複数タブで同時操作

これらの並行更新による不整合を防止するための同時実行制御戦略が必要である。

### 検討すべき要件

1. **整合性**: Lost Update（更新の喪失）を防止
2. **ユーザビリティ**: 正当な操作への影響を最小化
3. **スケーラビリティ**: 高負荷時でも性能を維持
4. **シンプルさ**: 実装・デバッグが容易

## 決定

**楽観的ロック（Optimistic Locking）をBooking集約の同時実行制御として採用する。**

### 採用する構成

| 項目 | 決定事項 |
|------|----------|
| ロック方式 | 楽観的ロック（version列） |
| バージョン管理 | 整数型、更新ごとにインクリメント |
| 衝突検知 | UPDATE WHERE version = :expected |
| 衝突時の動作 | 409 Conflict + リトライ推奨 |

### 実装パターン

```sql
-- 更新時のバージョンチェック
UPDATE bookings
SET
  start_at = :newStartAt,
  end_at = :newEndAt,
  note = :newNote,
  version = version + 1,
  updated_at = NOW()
WHERE id = :id
  AND version = :expectedVersion;

-- affected rows = 0 の場合、バージョン不一致
```

## 検討した選択肢

### 選択肢1: ロックなし（不採用）

同時実行制御を行わない。

**メリット:**
- 実装がシンプル
- パフォーマンス最高

**デメリット:**
- Lost Update が発生
- データ整合性を保証できない

```
User A: read(v1) ─────────────────> save(v1) ← 更新反映
User B:     read(v1) ────> save(v1)          ← User A の変更が失われる
```

### 選択肢2: 悲観的ロック（不採用）

読み取り時にロックを取得し、更新完了まで保持。

```sql
SELECT * FROM bookings WHERE id = :id FOR UPDATE;
-- 処理...
UPDATE bookings SET ... WHERE id = :id;
COMMIT;
```

**メリット:**
- 衝突を確実に防止
- リトライ不要

**デメリット:**
- 長時間ロック保持でスケーラビリティ低下
- デッドロックのリスク
- ユーザー操作時間中のロック保持は非現実的

```
User A: lock(10:00) ──────── 5分間操作中 ──────── unlock(10:05)
User B:      lock → blocked ──────────────────────> やっとロック取得
```

### 選択肢3: 楽観的ロック（採用）

更新時にバージョンをチェックし、不一致なら拒否。

```
User A: read(v1) ─────────────────> save(v1→v2) ✓
User B:     read(v1) ────> save(v1) ← version mismatch, CONFLICT
```

**メリット:**
- ロック保持時間がゼロ（読み取り時にロックしない）
- スケーラビリティが高い
- 衝突は稀（予約更新は低頻度）

**デメリット:**
- 衝突時はリトライまたはユーザーへの通知が必要
- 高競合環境では効率が低下

### 選択肢4: タイムスタンプベース（不採用）

バージョン番号の代わりに更新日時を使用。

```sql
UPDATE bookings SET ...
WHERE id = :id AND updated_at = :expectedUpdatedAt;
```

**メリット:**
- 追加カラム不要（updated_atを流用）

**デメリット:**
- ミリ秒精度の問題（同一ミリ秒での更新）
- タイムゾーン/クロック同期の問題
- 意図が明確でない

**結論:** バージョン番号の方が明確で安全。

## 結果

### 正の影響

1. **Lost Update防止**: バージョン不一致で更新を拒否
2. **スケーラビリティ**: ロック保持時間ゼロで高並行性を維持
3. **シンプルさ**: 整数インクリメントで実装容易
4. **デバッグ容易性**: バージョン履歴で更新回数を追跡可能

### 負の影響

1. **リトライ必要**: 衝突時にクライアントが再取得・再送信
   - **緩和策**: 明確なエラーレスポンスとリトライガイダンス
2. **高競合時の効率低下**: 同一予約への頻繁な同時更新
   - **緩和策**: 予約更新は低頻度のため、実際には稀

### APIレスポンス設計

```json
// 正常更新
HTTP 200 OK
{
  "id": "booking-123",
  "version": 3,
  ...
}

// バージョン不一致
HTTP 409 Conflict
{
  "type": "https://api.example.com/errors/version-mismatch",
  "title": "Version Mismatch",
  "status": 409,
  "detail": "The booking was modified by another request. Please refresh and try again.",
  "instance": "/bookings/booking-123",
  "currentVersion": 3,
  "expectedVersion": 2
}
```

### 実装例

```java
@Entity
public class Booking {
    @Id
    private BookingId id;

    @Version
    private Integer version;

    // JPA @Version で自動的に楽観的ロック
}

@Service
public class BookingService {

    @Transactional
    public Booking update(UpdateBookingCommand command) {
        Booking booking = repository.findById(command.getBookingId())
            .orElseThrow(BookingNotFoundException::new);

        // バージョンチェック（明示的に行う場合）
        if (!booking.getVersion().equals(command.getExpectedVersion())) {
            throw new OptimisticLockException(
                booking.getVersion(),
                command.getExpectedVersion()
            );
        }

        booking.updateTimeRange(command.getNewTimeRange());
        return repository.save(booking);
        // JPAが自動的にversion+1してUPDATE実行
    }
}
```

### クライアント側の対応

```typescript
async function updateBooking(booking: Booking, updates: BookingUpdates) {
  try {
    const response = await api.patch(`/bookings/${booking.id}`, {
      ...updates,
      expectedVersion: booking.version
    });
    return response.data;
  } catch (error) {
    if (error.status === 409) {
      // バージョン不一致：最新データを取得して再表示
      const latest = await api.get(`/bookings/${booking.id}`);
      showConflictDialog(booking, latest.data);
    }
    throw error;
  }
}
```

### 衝突検出と衝突防止の使い分け

| 機能 | 方式 | 目的 |
|------|------|------|
| 時間帯衝突検出 | 悲観的（FOR UPDATE） | 100%防止が必須（ADR-004） |
| 同時更新制御 | 楽観的（version） | スケーラビリティ優先 |

## 関連決定

- ADR-004: 予約衝突検出戦略
- ADR-006: 予約ステータス遷移の設計

## 参考資料

- Martin Fowler: Optimistic Offline Lock
- JPA Specification: @Version
- PostgreSQL: UPDATE with Returning
