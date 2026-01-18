---
doc_type: "adr"
id: "0009"
title: "支払いステータス遷移の設計"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team", "Product Team"]
---

# ADR-009: 支払いステータス遷移の設計

## ステータス

Accepted

## コンテキスト

Payment集約は外部決済ゲートウェイとの連携により、複数の状態を遷移する。2フェーズ決済（Authorize → Capture）を採用し、予約確定まで実際の課金を遅延させる。

### 検討すべき要件

1. **2フェーズ決済**: 与信とキャプチャを分離
2. **失敗処理**: 決済失敗時の明確な状態
3. **返金対応**: キャプチャ後の返金、与信取消
4. **整合性**: 不正な状態遷移を防止

### 業務フロー

```
予約作成 → 支払い作成 → 与信 → (予約確定) → キャプチャ → (サービス利用)
              ↓          ↓                       ↓
           (失敗)     (取消)                  (返金)
```

## 決定

**5状態の決済ステータスモデルを採用する。**

### 採用するステータス

| ステータス | 説明 | 遷移元 | 遷移先 |
|------------|------|--------|--------|
| PENDING | 支払い作成済み、与信待ち | (初期状態) | AUTHORIZED, FAILED |
| AUTHORIZED | 与信成功、キャプチャ待ち | PENDING | CAPTURED, REFUNDED |
| CAPTURED | キャプチャ完了（実際の課金） | AUTHORIZED | REFUNDED |
| REFUNDED | 返金完了（与信取消含む） | AUTHORIZED, CAPTURED | (なし) |
| FAILED | 決済失敗 | PENDING | (なし) |

### 状態遷移図

```
                              create()
                                 │
                                 ▼
                         ┌──────────────┐
                         │   PENDING    │
                         │              │
                         │ 与信待ち      │
                         └──────┬───────┘
                                │
               ┌────────────────┼────────────────┐
               │                │                │
               │ authorize()    │                │ fail()
               │ (Gateway成功)  │                │ (Gateway失敗)
               ▼                                 ▼
       ┌──────────────┐                  ┌──────────────┐
       │  AUTHORIZED  │                  │    FAILED    │
       │              │                  │              │
       │ 与信成功      │                  │ 終状態       │
       │ キャプチャ待ち │                  │              │
       └──────┬───────┘                  └──────────────┘
              │
     ┌────────┼────────┐
     │        │        │
     │ capture()       │ void()
     │                 │ (与信取消)
     ▼                 ▼
┌──────────────┐  ┌──────────────┐
│   CAPTURED   │  │   REFUNDED   │
│              │  │              │
│ キャプチャ完了 │  │ 返金/取消完了 │
│ 実際の課金   │  │ 終状態       │
└──────┬───────┘  └──────────────┘
       │                 ▲
       │ refund()        │
       └─────────────────┘
```

## 検討した選択肢

### 選択肢1: 3状態モデル（不採用）

```
PENDING → COMPLETED → REFUNDED
```

**メリット:**
- シンプル

**デメリット:**
- 2フェーズ決済に対応できない
- 与信とキャプチャの区別がない

### 選択肢2: 4状態モデル（不採用）

```
PENDING → AUTHORIZED → CAPTURED
                ↓
            CANCELLED
```

**メリット:**
- 2フェーズ対応
- シンプル

**デメリット:**
- 与信失敗（FAILED）の状態がない
- CAPTURED後の返金が表現できない

### 選択肢3: 5状態モデル（採用）

```
PENDING → AUTHORIZED → CAPTURED → REFUNDED
   ↓          ↓
 FAILED    REFUNDED
```

**メリット:**
- 2フェーズ決済を完全表現
- 失敗状態が明確
- 返金/取消を統一的に扱える

**デメリット:**
- 状態数が増加

### 選択肢4: 6状態以上（将来検討）

PARTIALLY_CAPTURED, PARTIALLY_REFUNDED等を追加。

**メリット:**
- 部分キャプチャ/部分返金に対応

**デメリット:**
- 複雑性の増加
- Phase 1では不要

**結論:** Phase 2で部分返金対応時に検討。

## 結果

### 正の影響

1. **2フェーズ決済対応**: 与信・キャプチャの分離
2. **明確な失敗状態**: FAILEDで決済失敗を表現
3. **統一的な返金処理**: REFUNDED状態に統合
4. **Booking連携**: 状態に応じたイベント発行

### 負の影響

1. **状態数**: 5状態の管理
   - **緩和策**: 状態遷移図とテストで明確化
2. **REFUNDED の多義性**: void と refund が同じ終状態
   - **緩和策**: イベントで区別（VoidCompleted / RefundCompleted）

### 実装例

```java
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    FAILED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case PENDING -> target == AUTHORIZED || target == FAILED;
            case AUTHORIZED -> target == CAPTURED || target == REFUNDED;
            case CAPTURED -> target == REFUNDED;
            case REFUNDED, FAILED -> false;  // 終状態
        };
    }
}

@Entity
public class Payment {
    private PaymentStatus status;
    private Integer capturedAmount;
    private Integer refundedAmount;

    public void authorize(String gatewayTransactionId) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot authorize payment in status: " + status
            );
        }
        this.gatewayTransactionId = gatewayTransactionId;
        this.status = PaymentStatus.AUTHORIZED;
        registerEvent(new PaymentAuthorized(this));
    }

    public void capture(Integer amount) {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Cannot capture payment in status: " + status
            );
        }
        if (amount == null) {
            amount = this.money.amount();  // 全額キャプチャ
        }
        if (amount > this.money.amount()) {
            throw new IllegalArgumentException(
                "Capture amount exceeds authorized amount"
            );
        }
        this.capturedAmount = amount;
        this.status = PaymentStatus.CAPTURED;
        registerEvent(new PaymentCaptured(this, amount));
    }

    public void voidAuthorization() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Cannot void payment in status: " + status
            );
        }
        this.status = PaymentStatus.REFUNDED;
        registerEvent(new PaymentVoided(this));
    }

    public void refund(Integer amount) {
        if (status != PaymentStatus.CAPTURED) {
            throw new IllegalStateException(
                "Cannot refund payment in status: " + status
            );
        }
        if (amount == null) {
            amount = this.capturedAmount;  // 全額返金
        }
        if (amount > this.capturedAmount - (this.refundedAmount != null ? this.refundedAmount : 0)) {
            throw new IllegalArgumentException(
                "Refund amount exceeds captured amount"
            );
        }
        this.refundedAmount = (this.refundedAmount != null ? this.refundedAmount : 0) + amount;
        if (this.refundedAmount.equals(this.capturedAmount)) {
            this.status = PaymentStatus.REFUNDED;
        }
        registerEvent(new PaymentRefunded(this, amount));
    }

    public void fail(String reason) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot fail payment in status: " + status
            );
        }
        this.failureReason = reason;
        this.status = PaymentStatus.FAILED;
        registerEvent(new PaymentFailed(this, reason));
    }
}
```

### Booking連携

| Paymentイベント | Booking動作 |
|-----------------|-------------|
| PaymentAuthorized | (待機) |
| PaymentCaptured | CONFIRMED に遷移 |
| PaymentVoided | (変更なし、予約はPENDINGのまま) |
| PaymentRefunded | CANCELLED に遷移（CONFIRMED時） |
| PaymentFailed | (予約キャンセル検討) |

### タイムアウト処理

| 状態 | タイムアウト条件 | 動作 |
|------|------------------|------|
| PENDING | 作成から30分経過 | FAILED に遷移、予約キャンセル |
| AUTHORIZED | 与信から7日経過 | void実行、REFUNDED に遷移 |

## 関連決定

- ADR-007: 冪等キー戦略の採用
- ADR-008: 外部決済ゲートウェイの抽象化（ACL）
- ADR-010: タイムアウト時の状態管理

## 参考資料

- Stripe: Payment Intents Lifecycle
- PayPal: Order Status Codes
- PCI DSS: Authorization and Capture
