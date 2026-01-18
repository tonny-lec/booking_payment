---
doc_type: "adr"
id: "0008"
title: "外部決済ゲートウェイの抽象化（ACL）"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team"]
---

# ADR-008: 外部決済ゲートウェイの抽象化（ACL）

## ステータス

Accepted

## コンテキスト

Paymentコンテキストは外部決済ゲートウェイ（Stripe, PayPay, 銀行振込等）と連携する必要がある。これらの外部サービスは：

- 異なるAPI仕様を持つ
- 独自のエラーコード・例外体系を持つ
- 予告なく仕様変更される可能性がある
- 将来的にプロバイダの追加・変更がありうる

ドメイン層を外部サービスの変更から保護し、テスタビリティを確保する必要がある。

### 検討すべき要件

1. **独立性**: ドメイン層が外部サービスに直接依存しない
2. **交換可能性**: ゲートウェイの追加・変更が容易
3. **テスタビリティ**: 外部サービスなしでテスト可能
4. **エラー変換**: 外部エラーをドメインエラーに変換

## 決定

**Anti-Corruption Layer（ACL）パターンを採用し、ポート＆アダプタで外部依存を抽象化する。**

### 採用する構成

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Payment Context                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                     Domain Layer                             │    │
│  │                                                              │    │
│  │  Payment (Aggregate)                                         │    │
│  │  PaymentService (Domain Service)                             │    │
│  │                                                              │    │
│  └──────────────────────────┬───────────────────────────────────┘    │
│                             │                                        │
│                             │ uses                                   │
│                             ▼                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              Port (Interface)                                 │   │
│  │                                                               │   │
│  │  PaymentGatewayPort                                           │   │
│  │  - authorize(request): AuthorizationResult                    │   │
│  │  - capture(transactionId, amount): CaptureResult              │   │
│  │  - refund(transactionId, amount): RefundResult                │   │
│  │  - void(transactionId): VoidResult                            │   │
│  │                                                               │   │
│  └──────────────────────────┬───────────────────────────────────┘   │
│                             │                                        │
│                             │ implements                             │
│                             ▼                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              Anti-Corruption Layer (Adapters)                 │   │
│  │                                                               │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │   │
│  │  │   Stripe    │  │   PayPay    │  │    Mock     │           │   │
│  │  │   Adapter   │  │   Adapter   │  │   Adapter   │           │   │
│  │  └──────┬──────┘  └──────┬──────┘  └─────────────┘           │   │
│  │         │                │                                    │   │
│  └─────────┼────────────────┼────────────────────────────────────┘   │
│            │                │                                        │
└────────────┼────────────────┼────────────────────────────────────────┘
             │                │
             ▼                ▼
      ┌─────────────┐  ┌─────────────┐
      │   Stripe    │  │   PayPay    │
      │    API      │  │    API      │
      └─────────────┘  └─────────────┘
```

### ポート定義

```java
public interface PaymentGatewayPort {

    AuthorizationResult authorize(AuthorizationRequest request);

    CaptureResult capture(CaptureRequest request);

    RefundResult refund(RefundRequest request);

    VoidResult voidAuthorization(VoidRequest request);

    TransactionStatus getStatus(String transactionId);
}
```

## 検討した選択肢

### 選択肢1: 直接依存（不採用）

ドメインサービスから直接Stripe SDKを呼び出す。

```java
// Bad: 直接依存
public class PaymentService {
    private final StripeClient stripeClient;

    public void authorize(Payment payment) {
        stripeClient.paymentIntents().create(...);  // Stripe固有
    }
}
```

**メリット:**
- 実装がシンプル
- 抽象化のオーバーヘッドなし

**デメリット:**
- ドメインがStripeに強く依存
- Stripe APIの変更がドメインに波及
- 単体テストが困難（Stripeモック必要）
- ゲートウェイ変更時に大規模修正

### 選択肢2: 薄いラッパー（不採用）

Stripeの型をそのまま使うが、呼び出しをラップ。

```java
public interface PaymentGateway {
    PaymentIntent authorize(PaymentIntentCreateParams params);  // Stripe型
}
```

**メリット:**
- 直接依存より分離
- 型変換のオーバーヘッドなし

**デメリット:**
- ドメインがStripeの型に依存
- 別ゲートウェイへの切り替えが困難

### 選択肢3: Anti-Corruption Layer（採用）

完全に抽象化されたポートとドメイン固有の型。

```java
// Good: ドメイン固有の型
public interface PaymentGatewayPort {
    AuthorizationResult authorize(AuthorizationRequest request);
}

public record AuthorizationRequest(
    Money amount,
    PaymentMethod method,
    String description
) {}

public record AuthorizationResult(
    String transactionId,
    AuthorizationStatus status,
    Instant authorizedAt
) {}
```

**メリット:**
- ドメインが外部サービスに依存しない
- ゲートウェイの追加・変更が容易
- 単体テストでモック差し替え可能
- エラー変換が統一的

**デメリット:**
- 型変換のオーバーヘッド
- 実装コードが増える

### 選択肢4: ゲートウェイサービス分離（将来検討）

決済ゲートウェイ連携を別マイクロサービスとして分離。

**メリット:**
- 完全な技術的独立性
- 独立したスケーリング

**デメリット:**
- 運用複雑性の増加
- ネットワークレイテンシ

**結論:** 現時点では過剰。規模拡大時に検討。

## 結果

### 正の影響

1. **ドメイン保護**: 外部変更からドメインを隔離
2. **交換可能性**: 新ゲートウェイはAdapter追加のみ
3. **テスタビリティ**: MockAdapterで高速テスト
4. **統一エラー処理**: 外部エラーをドメインエラーに変換

### 負の影響

1. **実装量増加**: Adapterごとに型変換コード
   - **緩和策**: 共通変換ユーティリティ
2. **パフォーマンス**: 型変換のオーバーヘッド
   - **緩和策**: 決済頻度では無視可能

### 実装例

#### ポート（インターフェース）

```java
public interface PaymentGatewayPort {

    AuthorizationResult authorize(AuthorizationRequest request)
        throws PaymentGatewayException;

    CaptureResult capture(CaptureRequest request)
        throws PaymentGatewayException;

    RefundResult refund(RefundRequest request)
        throws PaymentGatewayException;
}

// ドメイン固有の例外
public class PaymentGatewayException extends RuntimeException {
    private final GatewayErrorCode errorCode;
    private final boolean retryable;

    public enum GatewayErrorCode {
        CARD_DECLINED,
        INSUFFICIENT_FUNDS,
        EXPIRED_CARD,
        NETWORK_ERROR,
        TIMEOUT,
        UNKNOWN
    }
}
```

#### Stripeアダプタ

```java
@Component
@Profile("stripe")
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private final StripeClient stripeClient;

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.amount().toMinorUnits())
                .setCurrency(request.amount().currency().getCode())
                .setPaymentMethod(request.paymentMethodId())
                .setCaptureMethod(CaptureMethod.MANUAL)  // 2フェーズ決済
                .build();

            PaymentIntent intent = stripeClient.paymentIntents().create(params);

            return new AuthorizationResult(
                intent.getId(),
                mapStatus(intent.getStatus()),
                Instant.ofEpochSecond(intent.getCreated())
            );
        } catch (StripeException e) {
            throw mapToGatewayException(e);
        }
    }

    private PaymentGatewayException mapToGatewayException(StripeException e) {
        GatewayErrorCode code = switch (e.getCode()) {
            case "card_declined" -> GatewayErrorCode.CARD_DECLINED;
            case "expired_card" -> GatewayErrorCode.EXPIRED_CARD;
            case "insufficient_funds" -> GatewayErrorCode.INSUFFICIENT_FUNDS;
            default -> GatewayErrorCode.UNKNOWN;
        };
        boolean retryable = e instanceof ApiConnectionException;
        return new PaymentGatewayException(code, retryable, e);
    }
}
```

#### モックアダプタ（テスト用）

```java
@Component
@Profile("test")
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    private final Map<String, TransactionState> transactions = new ConcurrentHashMap<>();

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        String txId = "mock_" + UUID.randomUUID();
        transactions.put(txId, new TransactionState(
            request.amount(),
            TransactionStatus.AUTHORIZED
        ));
        return new AuthorizationResult(
            txId,
            AuthorizationStatus.AUTHORIZED,
            Instant.now()
        );
    }

    // テストヘルパー
    public void simulateDecline() {
        // 次回リクエストで CARD_DECLINED を返す
    }
}
```

### 設定による切り替え

```yaml
# application.yml
spring:
  profiles:
    active: stripe  # or paypay, test

payment:
  gateway:
    stripe:
      api-key: ${STRIPE_API_KEY}
      timeout: 30s
    paypay:
      api-key: ${PAYPAY_API_KEY}
      merchant-id: ${PAYPAY_MERCHANT_ID}
```

## 関連決定

- ADR-007: 冪等キー戦略の採用
- ADR-009: 支払いステータス遷移の設計
- ADR-010: タイムアウト時の状態管理

## 参考資料

- Eric Evans: Domain-Driven Design (Anti-Corruption Layer)
- Hexagonal Architecture (Ports & Adapters)
- Stripe API Documentation
