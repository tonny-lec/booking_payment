---
doc_type: "adr"
id: "0010"
title: "タイムアウト時の状態管理"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team"]
---

# ADR-010: タイムアウト時の状態管理

## ステータス

Accepted

## コンテキスト

外部決済ゲートウェイとの通信では、以下のタイムアウトシナリオが発生しうる：

1. **リクエストタイムアウト**: ゲートウェイへのリクエストが時間内に完了しない
2. **レスポンス不明**: リクエストは送信されたが、レスポンスを受信できない
3. **処理中断**: アプリケーション障害で処理が中断

これらのケースでは、実際にゲートウェイ側で処理が完了したか不明であり、状態の不整合が発生するリスクがある。

### 検討すべき要件

1. **整合性**: ゲートウェイとの状態整合性を維持
2. **安全性**: 二重課金を防止
3. **回復性**: 障害からの自動復旧
4. **可観測性**: 不整合状態の検知

## 決定

**「不確定状態」の導入と定期的な状態確認ジョブを採用する。**

### 採用する構成

| 項目 | 決定事項 |
|------|----------|
| タイムアウト検出 | リクエストタイムアウト（30秒） |
| 不確定状態管理 | pending_verification フラグ |
| 状態確認 | 定期ジョブ（5分間隔） |
| 最大試行回数 | 10回（約50分） |
| 最終手段 | 手動介入（アラート発報） |

### タイムアウト発生時のフロー

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    タイムアウト発生時のフロー                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐     authorize()     ┌──────────┐                          │
│  │  Client  │────────────────────>│ Payment  │                          │
│  └──────────┘                     └────┬─────┘                          │
│                                        │                                 │
│                                        │ Gateway.authorize()             │
│                                        ▼                                 │
│                                   ┌──────────┐                          │
│                                   │ Gateway  │                          │
│                                   └────┬─────┘                          │
│                                        │                                 │
│                                        │ TIMEOUT (30s)                   │
│                                        ▼                                 │
│                                   ┌──────────────────────────────────┐   │
│                                   │  Payment                          │   │
│                                   │  status: PENDING                  │   │
│                                   │  pending_verification: true       │   │
│                                   │  last_gateway_check: null         │   │
│                                   └──────────────────────────────────┘   │
│                                                                          │
│  === 定期ジョブ（5分ごと） ===                                           │
│                                                                          │
│  ┌─────────────────────┐        ┌──────────┐                            │
│  │VerificationJob      │───────>│ Gateway  │                            │
│  │                     │        │ .getStatus()                          │
│  │                     │<───────│          │                            │
│  └─────────┬───────────┘        └──────────┘                            │
│            │                                                             │
│            ├── AUTHORIZED → status=AUTHORIZED, flag=false               │
│            ├── NOT_FOUND  → status=FAILED, flag=false                   │
│            └── TIMEOUT    → retry_count++, 次回ジョブで再試行            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 検討した選択肢

### 選択肢1: 即座に失敗扱い（不採用）

タイムアウト発生時に即座にFAILEDに遷移。

**メリット:**
- 実装がシンプル
- 即座に結果を返せる

**デメリット:**
- ゲートウェイ側で成功していた場合、状態不整合
- 二重課金のリスク（ユーザーが再試行した場合）

### 選択肢2: 同期リトライ（不採用）

タイムアウト後に即座にリトライ。

**メリット:**
- ユーザーへの応答が早い

**デメリット:**
- リクエスト処理時間が延長（30秒 × リトライ回数）
- ユーザー体験の低下

### 選択肢3: 不確定状態 + 非同期確認（採用）

タイムアウト時に「確認待ち」フラグを立て、非同期ジョブで状態確認。

**メリット:**
- ユーザーへは即座に応答（「処理中」として）
- バックグラウンドで正確な状態を確認
- 二重課金を防止（冪等キーと組み合わせ）

**デメリット:**
- 実装が複雑
- 最終状態確定まで時間がかかる

### 選択肢4: Sagaパターン（将来検討）

分散トランザクションをSagaで管理。

**メリット:**
- 複数サービス間の整合性

**デメリット:**
- 過剰な複雑性（単一ゲートウェイには不要）

## 結果

### 正の影響

1. **状態整合性**: ゲートウェイの実際の状態と同期
2. **二重課金防止**: 冪等キー + 状態確認で防止
3. **自動回復**: 多くのケースで人手介入不要
4. **可観測性**: 不確定状態を監視可能

### 負の影響

1. **実装複雑性**: 状態確認ジョブの実装
   - **緩和策**: 明確なジョブ設計とテスト
2. **最終確定の遅延**: 最大50分（10回 × 5分）
   - **緩和策**: ユーザーへの状態通知
3. **手動介入ケース**: 最大試行後も不明な場合
   - **緩和策**: アラートと運用手順の整備

### 実装詳細

#### データモデル拡張

```sql
ALTER TABLE payments ADD COLUMN pending_verification BOOLEAN DEFAULT FALSE;
ALTER TABLE payments ADD COLUMN verification_attempts INTEGER DEFAULT 0;
ALTER TABLE payments ADD COLUMN last_verification_at TIMESTAMP;
```

#### 状態確認ジョブ

```java
@Component
public class PaymentVerificationJob {

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(5);

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void verifyPendingPayments() {
        List<Payment> pendingVerification = paymentRepository
            .findByPendingVerificationTrue();

        for (Payment payment : pendingVerification) {
            try {
                verifyPayment(payment);
            } catch (Exception e) {
                log.error("Failed to verify payment: {}", payment.getId(), e);
            }
        }
    }

    private void verifyPayment(Payment payment) {
        if (payment.getVerificationAttempts() >= MAX_ATTEMPTS) {
            // 最大試行回数超過 → 手動介入
            alertService.sendAlert(
                AlertLevel.HIGH,
                "Payment verification failed after max attempts",
                Map.of("paymentId", payment.getId())
            );
            payment.markVerificationFailed();
            paymentRepository.save(payment);
            return;
        }

        try {
            TransactionStatus status = paymentGateway.getStatus(
                payment.getGatewayTransactionId()
            );

            switch (status) {
                case AUTHORIZED -> {
                    payment.confirmAuthorization();
                    payment.clearPendingVerification();
                    eventPublisher.publish(new PaymentAuthorized(payment));
                }
                case NOT_FOUND, DECLINED -> {
                    payment.fail("Gateway status: " + status);
                    payment.clearPendingVerification();
                    eventPublisher.publish(new PaymentFailed(payment));
                }
                case PENDING -> {
                    // まだ処理中 → 次回再確認
                    payment.incrementVerificationAttempts();
                }
            }
        } catch (PaymentGatewayException e) {
            if (e.isRetryable()) {
                payment.incrementVerificationAttempts();
            } else {
                payment.markVerificationFailed();
                alertService.sendAlert(AlertLevel.HIGH,
                    "Non-retryable gateway error during verification",
                    Map.of("paymentId", payment.getId(), "error", e.getMessage())
                );
            }
        }

        payment.setLastVerificationAt(Instant.now());
        paymentRepository.save(payment);
    }
}
```

#### タイムアウト時の処理

```java
@Service
public class PaymentService {

    public Payment authorize(AuthorizationRequest request) {
        Payment payment = Payment.create(request);
        paymentRepository.save(payment);

        try {
            AuthorizationResult result = paymentGateway.authorize(
                request.toGatewayRequest()
            );
            payment.authorize(result.transactionId());
            eventPublisher.publish(new PaymentAuthorized(payment));

        } catch (GatewayTimeoutException e) {
            // タイムアウト → 確認待ち状態に
            payment.markPendingVerification();
            log.warn("Gateway timeout, marking for verification: {}",
                payment.getId());

        } catch (PaymentGatewayException e) {
            payment.fail(e.getMessage());
            eventPublisher.publish(new PaymentFailed(payment));
        }

        return paymentRepository.save(payment);
    }
}
```

### クライアントへの応答

| ケース | HTTPステータス | レスポンス |
|--------|----------------|------------|
| 正常完了 | 201 Created | `{status: "AUTHORIZED"}` |
| タイムアウト（確認待ち） | 202 Accepted | `{status: "PENDING", verificationRequired: true}` |
| 確定失敗 | 400/402 | `{status: "FAILED", reason: "..."}` |

### 監視とアラート

| メトリクス | 閾値 | アクション |
|------------|------|------------|
| `payment_pending_verification_count` | > 10 | 調査開始 |
| `payment_verification_failed_total` | > 0 | 即座に対応 |
| `payment_gateway_timeout_total` | > 5/min | ゲートウェイ障害疑い |

## 関連決定

- ADR-007: 冪等キー戦略の採用
- ADR-008: 外部決済ゲートウェイの抽象化（ACL）
- ADR-009: 支払いステータス遷移の設計

## 参考資料

- Stripe: Handling webhook events
- AWS: Saga pattern for microservices
- Martin Kleppmann: Designing Data-Intensive Applications (Exactly-once semantics)
