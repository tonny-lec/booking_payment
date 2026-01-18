---
doc_type: "adr"
id: "0006"
title: "予約ステータス遷移の設計"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team", "Product Team"]
---

# ADR-006: 予約ステータス遷移の設計

## ステータス

Accepted

## コンテキスト

Booking集約は作成から完了・キャンセルまでのライフサイクルを持つ。このライフサイクルを状態（ステータス）として管理し、不正な遷移を防止する必要がある。

### 検討すべき要件

1. **ビジネスルール**: 予約→支払い→確定の業務フロー
2. **整合性**: 不正な状態遷移を防止
3. **拡張性**: 将来のステータス追加に対応
4. **明確性**: 各ステータスの意味が一意

### 業務フロー

```
ユーザー → 予約作成 → 支払い → 予約確定 → サービス利用
              ↓          ↓         ↓
           キャンセル  キャンセル  キャンセル（返金）
```

## 決定

**3状態のシンプルなステータスモデルを採用する。**

### 採用するステータス

| ステータス | 説明 | 遷移元 | 遷移先 |
|------------|------|--------|--------|
| PENDING | 予約作成済み、支払い待ち | (初期状態) | CONFIRMED, CANCELLED |
| CONFIRMED | 支払い完了、予約確定 | PENDING | CANCELLED |
| CANCELLED | キャンセル済み（終状態） | PENDING, CONFIRMED | (なし) |

### 状態遷移図

```
                    ┌─────────────────────────────────────────┐
                    │           Booking Lifecycle              │
                    └─────────────────────────────────────────┘

                              create()
                                 │
                                 ▼
                         ┌──────────────┐
                         │   PENDING    │
                         │              │
                         │ 支払い待ち    │
                         └──────┬───────┘
                                │
               ┌────────────────┼────────────────┐
               │                │                │
               │ confirm()      │                │ cancel()
               │ (PaymentCaptured)               │
               ▼                                 ▼
       ┌──────────────┐                  ┌──────────────┐
       │  CONFIRMED   │                  │  CANCELLED   │
       │              │                  │              │
       │ 支払い完了    │   cancel()       │ 終状態       │
       │ 予約確定      │─────────────────>│              │
       └──────────────┘                  └──────────────┘
```

### 遷移ルール

| 遷移 | トリガー | 条件 | 発行イベント |
|------|----------|------|--------------|
| → PENDING | create() | 時間帯の衝突なし | BookingCreated |
| PENDING → CONFIRMED | confirm() | PaymentCapturedイベント受信 | BookingConfirmed |
| PENDING → CANCELLED | cancel() | ユーザー操作 / タイムアウト | BookingCancelled |
| CONFIRMED → CANCELLED | cancel() | ユーザー操作 / 管理者操作 | BookingCancelled |

## 検討した選択肢

### 選択肢1: 2状態モデル（不採用）

```
ACTIVE ←→ CANCELLED
```

**メリット:**
- 最もシンプル

**デメリット:**
- 支払い前後の区別ができない
- 支払い待ちの予約を識別できない

### 選択肢2: 3状態モデル（採用）

```
PENDING → CONFIRMED → CANCELLED
            ↑            ↑
            └────────────┘
```

**メリット:**
- 支払い前後が明確
- ビジネスフローを正確に表現
- シンプルで理解しやすい

**デメリット:**
- 「進行中」の状態がない（例：チェックイン中）

### 選択肢3: 5状態モデル（将来検討）

```
PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
    ↓         ↓           ↓
  CANCELLED CANCELLED  CANCELLED
```

**メリット:**
- きめ細かなライフサイクル管理
- サービス提供中の状態を追跡

**デメリット:**
- 複雑性の増加
- 現時点では不要な機能

**結論:** Phase 1では選択肢2を採用。Phase 2でIN_PROGRESS/COMPLETEDの追加を検討。

### 選択肢4: ステートマシンライブラリ使用（部分採用）

Spring State Machine等のライブラリを使用。

**メリット:**
- 遷移ルールの宣言的定義
- 遷移ガード、アクションの標準化

**デメリット:**
- 学習コスト
- 3状態には過剰

**結論:** コード内でシンプルに実装。状態が増えた場合に再検討。

## 結果

### 正の影響

1. **明確なビジネスモデル**: 支払い前後の区別が明確
2. **整合性保証**: 不正遷移をドメイン層で防止
3. **シンプルさ**: 3状態で十分なカバレッジ
4. **拡張性**: 将来のステータス追加が容易

### 負の影響

1. **限定的な表現力**: サービス提供中の状態がない
   - **緩和策**: Phase 2でステータス追加予定
2. **CANCELLED後の復元不可**: 終状態からの復帰なし
   - **緩和策**: 運用上、新規予約で対応

### 実装例

```java
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false; // 終状態
        };
    }
}

@Entity
public class Booking {
    private BookingStatus status;

    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot confirm booking in status: " + status
            );
        }
        this.status = BookingStatus.CONFIRMED;
        registerEvent(new BookingConfirmed(this));
    }

    public void cancel(String reason) {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        BookingStatus previousStatus = this.status;
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
        registerEvent(new BookingCancelled(this, previousStatus, reason));
    }
}
```

### イベントとステータスの関係

| イベント | 発行元ステータス | 遷移後ステータス |
|----------|------------------|------------------|
| BookingCreated | - | PENDING |
| BookingConfirmed | PENDING | CONFIRMED |
| BookingCancelled | PENDING or CONFIRMED | CANCELLED |

### キャンセルポリシー（将来検討）

| 元ステータス | キャンセル時の処理 |
|--------------|-------------------|
| PENDING | 支払いなし → 処理なし |
| CONFIRMED | 支払い済み → 返金処理を開始 |

```java
public void cancel(String reason) {
    BookingStatus previousStatus = this.status;
    // ... 状態遷移 ...

    if (previousStatus == BookingStatus.CONFIRMED) {
        // Payment コンテキストに返金を依頼
        registerEvent(new RefundRequested(this.id, this.paymentId));
    }
}
```

### API レスポンス例

```json
{
  "id": "booking-123",
  "status": "CONFIRMED",
  "allowedTransitions": ["CANCELLED"],
  "statusHistory": [
    { "status": "PENDING", "at": "2026-01-18T10:00:00Z" },
    { "status": "CONFIRMED", "at": "2026-01-18T10:05:00Z" }
  ]
}
```

## 関連決定

- ADR-004: 予約衝突検出戦略
- ADR-005: 楽観的ロックの採用理由

## 参考資料

- Domain-Driven Design: Aggregate State
- Martin Fowler: State Machine
- Event Sourcing patterns
