---
doc_type: "test_plan"
id: "test-plan"
version: "0.3"
last_updated: "2026-01-18"
status: "draft"
---

# テスト計画

## 1. テスト戦略

### 1.1 テストピラミッド

```
         ┌─────────────┐
         │   E2E Test  │  ← 少数：重要フロー
         │    (10%)    │
         ├─────────────┤
         │ Integration │  ← 中程度：外部依存
         │    (20%)    │
         ├─────────────┤
         │  Unit Test  │  ← 多数：ビジネスロジック
         │    (70%)    │
         └─────────────┘
```

### 1.2 重点テスト領域

- 二重予約、境界時間
- 冪等性欠落による二重課金
- タイムアウト/リトライでの重複処理
- 権限チェック漏れ
- 互換性破壊
- traceId欠落

### 1.3 Property-Based Testing 候補

- TimeRange（start < end）
- Idempotency（同一入力→同一出力）
- 状態遷移（許可されない遷移は拒否）

---

## 2. Payment テスト計画

### 2.1 Money値オブジェクト（PAY-TEST-01）

Money値オブジェクトの不変条件と演算のユニットテスト。

#### テストケース一覧

| ID | テストケース | 入力 | 期待結果 |
|----|-------------|------|----------|
| MNY-001 | 有効なMoney作成（JPY） | amount=1000, currency=JPY | 正常作成 |
| MNY-002 | 有効なMoney作成（USD） | amount=999, currency=USD | 正常作成（$9.99） |
| MNY-003 | 無効：amount <= 0 | amount=0, currency=JPY | IllegalArgumentException |
| MNY-004 | 無効：負の金額 | amount=-100, currency=JPY | IllegalArgumentException |
| MNY-005 | 無効：不正な通貨コード | amount=100, currency=XXX | IllegalArgumentException |
| MNY-006 | 加算：同一通貨 | 1000 JPY + 500 JPY | 1500 JPY |
| MNY-007 | 加算：異なる通貨 | 1000 JPY + 10 USD | CurrencyMismatchException |
| MNY-008 | 減算：正常 | 1000 JPY - 300 JPY | 700 JPY |
| MNY-009 | 減算：結果が0以下 | 100 JPY - 200 JPY | IllegalArgumentException |
| MNY-010 | 比較：大きい | 1000 JPY vs 500 JPY | true |
| MNY-011 | 比較：等しい | 1000 JPY vs 1000 JPY | false (not greater) |
| MNY-012 | 比較：異なる通貨 | 1000 JPY vs 10 USD | CurrencyMismatchException |
| MNY-013 | 等価性：同一値 | 1000 JPY == 1000 JPY | true |
| MNY-014 | 等価性：異なる値 | 1000 JPY == 500 JPY | false |
| MNY-015 | 境界値：最小金額（1） | amount=1, currency=JPY | 正常作成 |
| MNY-016 | 境界値：最大金額 | amount=Integer.MAX_VALUE | 正常作成 |

#### 実装例

```java
@Nested
@DisplayName("Money Unit Tests (PAY-TEST-01)")
class MoneyTest {

    @Test
    @DisplayName("MNY-001: 有効なMoney（JPY）を作成できる")
    void validMoney_JPY_IsCreated() {
        Money money = Money.of(1000, Currency.JPY);

        assertThat(money.amount()).isEqualTo(1000);
        assertThat(money.currency()).isEqualTo(Currency.JPY);
    }

    @Test
    @DisplayName("MNY-002: USD金額は最小単位（セント）で扱う")
    void validMoney_USD_IsCreatedInCents() {
        Money money = Money.of(999, Currency.USD); // $9.99

        assertThat(money.amount()).isEqualTo(999);
        assertThat(money.currency()).isEqualTo(Currency.USD);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, Integer.MIN_VALUE})
    @DisplayName("MNY-003/004: 0以下の金額はIllegalArgumentExceptionをスローする")
    void invalidAmount_ThrowsException(int invalidAmount) {
        assertThatThrownBy(() -> Money.of(invalidAmount, Currency.JPY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("amount must be positive");
    }

    @Test
    @DisplayName("MNY-005: 不正な通貨コードはIllegalArgumentExceptionをスローする")
    void invalidCurrency_ThrowsException() {
        assertThatThrownBy(() -> Money.of(100, Currency.of("XXX")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid currency code");
    }

    @Test
    @DisplayName("MNY-006: 同一通貨の加算が正しく動作する")
    void add_SameCurrency_Succeeds() {
        Money a = Money.of(1000, Currency.JPY);
        Money b = Money.of(500, Currency.JPY);

        Money result = a.add(b);

        assertThat(result.amount()).isEqualTo(1500);
        assertThat(result.currency()).isEqualTo(Currency.JPY);
    }

    @Test
    @DisplayName("MNY-007: 異なる通貨の加算はCurrencyMismatchExceptionをスローする")
    void add_DifferentCurrency_ThrowsException() {
        Money jpy = Money.of(1000, Currency.JPY);
        Money usd = Money.of(10, Currency.USD);

        assertThatThrownBy(() -> jpy.add(usd))
            .isInstanceOf(CurrencyMismatchException.class)
            .hasMessageContaining("cannot add different currencies");
    }

    @Test
    @DisplayName("MNY-008: 減算が正しく動作する")
    void subtract_ValidAmount_Succeeds() {
        Money a = Money.of(1000, Currency.JPY);
        Money b = Money.of(300, Currency.JPY);

        Money result = a.subtract(b);

        assertThat(result.amount()).isEqualTo(700);
    }

    @Test
    @DisplayName("MNY-009: 結果が0以下になる減算はIllegalArgumentExceptionをスローする")
    void subtract_ResultNonPositive_ThrowsException() {
        Money a = Money.of(100, Currency.JPY);
        Money b = Money.of(200, Currency.JPY);

        assertThatThrownBy(() -> a.subtract(b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("result must be positive");
    }

    @Test
    @DisplayName("MNY-010: isGreaterThanが正しく判定する")
    void isGreaterThan_WhenGreater_ReturnsTrue() {
        Money larger = Money.of(1000, Currency.JPY);
        Money smaller = Money.of(500, Currency.JPY);

        assertThat(larger.isGreaterThan(smaller)).isTrue();
        assertThat(smaller.isGreaterThan(larger)).isFalse();
    }

    @Test
    @DisplayName("MNY-013/014: 等価性が正しく判定される")
    void equals_WorksCorrectly() {
        Money a = Money.of(1000, Currency.JPY);
        Money b = Money.of(1000, Currency.JPY);
        Money c = Money.of(500, Currency.JPY);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("MNY-015: 最小金額（1）で作成できる")
    void minAmount_IsValid() {
        Money money = Money.of(1, Currency.JPY);

        assertThat(money.amount()).isEqualTo(1);
    }
}
```

### 2.2 Payment集約（PAY-TEST-02）

Payment集約のドメインロジックと状態遷移のユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| PAY-001 | 支払い作成（正常） | 有効な入力 | PENDING状態で作成 + PaymentCreatedイベント |
| PAY-002 | 与信成功 | PENDING + gatewayTxId | AUTHORIZED + PaymentAuthorizedイベント |
| PAY-003 | 与信失敗 | PENDING + 失敗理由 | FAILED + PaymentFailedイベント |
| PAY-004 | キャプチャ（全額） | AUTHORIZED状態 | CAPTURED + PaymentCapturedイベント |
| PAY-005 | キャプチャ（部分） | AUTHORIZED + 部分金額 | CAPTURED + capturedAmount設定 |
| PAY-006 | キャプチャ：超過金額 | captureAmount > amount | IllegalArgumentException |
| PAY-007 | 与信取消（void） | AUTHORIZED状態 | REFUNDED + PaymentRefundedイベント |
| PAY-008 | 返金（全額） | CAPTURED状態 | REFUNDED + refundedAmount=capturedAmount |
| PAY-009 | 返金（部分） | CAPTURED + 部分金額 | REFUNDED + refundedAmount設定 |
| PAY-010 | 返金：超過金額 | refundAmount > capturedAmount | IllegalArgumentException |
| PAY-011 | 不正遷移：PENDING→CAPTURED | skip AUTHORIZED | IllegalStateException |
| PAY-012 | 不正遷移：FAILED→AUTHORIZED | 終状態からの遷移 | IllegalStateException |
| PAY-013 | 不正遷移：REFUNDED→CAPTURED | 終状態からの遷移 | IllegalStateException |
| PAY-014 | 二重与信 | AUTHORIZED状態で再度authorize | IllegalStateException |
| PAY-015 | description保存 | description指定あり | descriptionが保存される |

#### 実装例

```java
@Nested
@DisplayName("Payment Aggregate Unit Tests (PAY-TEST-02)")
class PaymentAggregateTest {

    private Clock fixedClock;
    private BookingId bookingId;
    private UserId userId;
    private Money validMoney;
    private IdempotencyKey idempotencyKey;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T10:00:00Z"),
            ZoneOffset.UTC
        );
        bookingId = BookingId.of("booking-001");
        userId = UserId.of("user-001");
        validMoney = Money.of(10000, Currency.JPY);
        idempotencyKey = IdempotencyKey.generate(fixedClock);
    }

    @Test
    @DisplayName("PAY-001: 支払いがPENDING状態で作成される")
    void create_CreatesPaymentInPendingStatus() {
        Payment payment = Payment.create(
            bookingId, userId, validMoney, idempotencyKey, null, fixedClock
        );

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getBookingId()).isEqualTo(bookingId);
        assertThat(payment.getMoney()).isEqualTo(validMoney);
        assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(payment.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(PaymentCreated.class);
    }

    @Test
    @DisplayName("PAY-002: 与信成功でAUTHORIZED状態に遷移する")
    void authorize_WhenPending_TransitionsToAuthorized() {
        Payment payment = createPendingPayment();
        String gatewayTxId = "gw-tx-001";

        payment.authorize(gatewayTxId, fixedClock);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getGatewayTransactionId()).isEqualTo(gatewayTxId);
        assertThat(payment.getDomainEvents())
            .filteredOn(e -> e instanceof PaymentAuthorized)
            .hasSize(1);
    }

    @Test
    @DisplayName("PAY-003: 与信失敗でFAILED状態に遷移する")
    void fail_WhenPending_TransitionsToFailed() {
        Payment payment = createPendingPayment();
        String failureReason = "Card declined";

        payment.fail(failureReason, fixedClock);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        assertThat(payment.getDomainEvents())
            .filteredOn(e -> e instanceof PaymentFailed)
            .hasSize(1);
    }

    @Test
    @DisplayName("PAY-004: キャプチャ（全額）でCAPTURED状態に遷移する")
    void capture_FullAmount_TransitionsToCaptured() {
        Payment payment = createAuthorizedPayment();

        payment.capture(null, fixedClock); // 全額キャプチャ

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getCapturedAmount()).isEqualTo(validMoney.amount());
        assertThat(payment.getDomainEvents())
            .filteredOn(e -> e instanceof PaymentCaptured)
            .hasSize(1);
    }

    @Test
    @DisplayName("PAY-005: 部分キャプチャが正しく動作する")
    void capture_PartialAmount_SetsCapturedAmount() {
        Payment payment = createAuthorizedPayment();
        int partialAmount = 5000;

        payment.capture(partialAmount, fixedClock);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getCapturedAmount()).isEqualTo(partialAmount);
    }

    @Test
    @DisplayName("PAY-006: キャプチャ金額が与信額を超えるとIllegalArgumentExceptionをスローする")
    void capture_ExceedingAmount_ThrowsException() {
        Payment payment = createAuthorizedPayment();
        int exceedingAmount = validMoney.amount() + 1;

        assertThatThrownBy(() -> payment.capture(exceedingAmount, fixedClock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("capture amount exceeds authorized amount");
    }

    @Test
    @DisplayName("PAY-008: 全額返金でREFUNDED状態に遷移する")
    void refund_FullAmount_TransitionsToRefunded() {
        Payment payment = createCapturedPayment();

        payment.refund(null, fixedClock); // 全額返金

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualTo(payment.getCapturedAmount());
        assertThat(payment.getDomainEvents())
            .filteredOn(e -> e instanceof PaymentRefunded)
            .hasSize(1);
    }

    @Test
    @DisplayName("PAY-010: 返金金額がキャプチャ額を超えるとIllegalArgumentExceptionをスローする")
    void refund_ExceedingAmount_ThrowsException() {
        Payment payment = createCapturedPayment();
        int exceedingAmount = payment.getCapturedAmount() + 1;

        assertThatThrownBy(() -> payment.refund(exceedingAmount, fixedClock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("refund amount exceeds captured amount");
    }

    @Test
    @DisplayName("PAY-011: PENDING→CAPTUREDの直接遷移はIllegalStateExceptionをスローする")
    void capture_WhenPending_ThrowsException() {
        Payment payment = createPendingPayment();

        assertThatThrownBy(() -> payment.capture(null, fixedClock))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot capture from PENDING");
    }

    @Test
    @DisplayName("PAY-012: FAILED状態からの遷移はIllegalStateExceptionをスローする")
    void authorize_WhenFailed_ThrowsException() {
        Payment payment = createPendingPayment();
        payment.fail("Initial failure", fixedClock);

        assertThatThrownBy(() -> payment.authorize("gw-tx-001", fixedClock))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot authorize from FAILED");
    }

    @Test
    @DisplayName("PAY-014: 二重与信はIllegalStateExceptionをスローする")
    void authorize_WhenAlreadyAuthorized_ThrowsException() {
        Payment payment = createAuthorizedPayment();

        assertThatThrownBy(() -> payment.authorize("gw-tx-002", fixedClock))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already authorized");
    }

    private Payment createPendingPayment() {
        Payment payment = Payment.create(
            bookingId, userId, validMoney, idempotencyKey, null, fixedClock
        );
        payment.clearEvents();
        return payment;
    }

    private Payment createAuthorizedPayment() {
        Payment payment = createPendingPayment();
        payment.authorize("gw-tx-001", fixedClock);
        payment.clearEvents();
        return payment;
    }

    private Payment createCapturedPayment() {
        Payment payment = createAuthorizedPayment();
        payment.capture(null, fixedClock);
        payment.clearEvents();
        return payment;
    }
}
```

### 2.3 IdempotencyKey（PAY-TEST-03）

冪等キーの生成・検証ロジックのユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| IDM-001 | キー生成 | generate() | UUID形式のキーが生成される |
| IDM-002 | キー一意性 | 複数回generate() | 毎回異なるキー |
| IDM-003 | 有効期限内 | createdAt + 23h | isExpired=false |
| IDM-004 | 有効期限切れ | createdAt + 25h | isExpired=true |
| IDM-005 | 境界：ちょうど24h | createdAt + 24h | isExpired=true（境界含む） |
| IDM-006 | リクエストハッシュ一致 | 同一入力 | 同一ハッシュ |
| IDM-007 | リクエストハッシュ不一致 | 異なる入力 | 異なるハッシュ |
| IDM-008 | ハッシュ対象フィールド | bookingId, amount, currency | ハッシュに含まれる |
| IDM-009 | 無効なUUID形式 | 不正文字列 | IllegalArgumentException |

#### 実装例

```java
@Nested
@DisplayName("IdempotencyKey Unit Tests (PAY-TEST-03)")
class IdempotencyKeyTest {

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T10:00:00Z"),
            ZoneOffset.UTC
        );
    }

    @Test
    @DisplayName("IDM-001: キーが正しく生成される")
    void generate_CreatesValidKey() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);

        assertThat(key.value()).isNotNull();
        assertThat(key.createdAt()).isEqualTo(fixedClock.instant());
        // UUID形式の検証
        assertThatNoException().isThrownBy(() ->
            UUID.fromString(key.value().toString())
        );
    }

    @Test
    @DisplayName("IDM-002: 生成されるキーは毎回一意")
    void generate_ProducesUniqueKeys() {
        Set<IdempotencyKey> keys = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            keys.add(IdempotencyKey.generate(fixedClock));
        }

        assertThat(keys).hasSize(1000);
    }

    @Test
    @DisplayName("IDM-003: 24時間以内は有効")
    void isExpired_Within24Hours_ReturnsFalse() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);

        Clock after23Hours = Clock.fixed(
            fixedClock.instant().plus(Duration.ofHours(23)),
            ZoneOffset.UTC
        );

        assertThat(key.isExpired(after23Hours)).isFalse();
    }

    @Test
    @DisplayName("IDM-004: 24時間経過後は期限切れ")
    void isExpired_After24Hours_ReturnsTrue() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);

        Clock after25Hours = Clock.fixed(
            fixedClock.instant().plus(Duration.ofHours(25)),
            ZoneOffset.UTC
        );

        assertThat(key.isExpired(after25Hours)).isTrue();
    }

    @Test
    @DisplayName("IDM-005: ちょうど24時間で期限切れ")
    void isExpired_ExactlyAt24Hours_ReturnsTrue() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);

        Clock exactlyAt24Hours = Clock.fixed(
            fixedClock.instant().plus(Duration.ofHours(24)),
            ZoneOffset.UTC
        );

        assertThat(key.isExpired(exactlyAt24Hours)).isTrue();
    }

    @Test
    @DisplayName("IDM-006: 同一入力で同一リクエストハッシュが生成される")
    void computeRequestHash_SameInput_ProducesSameHash() {
        BookingId bookingId = BookingId.of("booking-001");
        int amount = 10000;
        Currency currency = Currency.JPY;

        String hash1 = IdempotencyKey.computeRequestHash(bookingId, amount, currency);
        String hash2 = IdempotencyKey.computeRequestHash(bookingId, amount, currency);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("IDM-007: 異なる入力で異なるリクエストハッシュが生成される")
    void computeRequestHash_DifferentInput_ProducesDifferentHash() {
        BookingId bookingId = BookingId.of("booking-001");

        String hash1 = IdempotencyKey.computeRequestHash(bookingId, 10000, Currency.JPY);
        String hash2 = IdempotencyKey.computeRequestHash(bookingId, 10001, Currency.JPY);
        String hash3 = IdempotencyKey.computeRequestHash(bookingId, 10000, Currency.USD);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    @DisplayName("IDM-009: 不正なUUID形式はIllegalArgumentExceptionをスローする")
    void of_InvalidUuid_ThrowsException() {
        assertThatThrownBy(() ->
            IdempotencyKey.of("not-a-valid-uuid", fixedClock.instant())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("invalid UUID format");
    }
}
```

### 2.4 PaymentRepository 統合テスト（PAY-TEST-04）

データベースとの統合テスト。

#### テストケース一覧

| ID | テストケース | 操作 | 期待結果 |
|----|-------------|------|----------|
| PR-001 | 支払い保存 | save(payment) | DBに永続化 |
| PR-002 | ID検索 | findById(existingId) | Optional.of(payment) |
| PR-003 | BookingId検索 | findByBookingId(bookingId) | 関連支払い一覧 |
| PR-004 | IdempotencyKey検索 | findByIdempotencyKey(key) | Optional.of(payment) |
| PR-005 | IdempotencyKey一意制約 | 重複キー保存 | DataIntegrityViolationException |
| PR-006 | ユーザー検索 | findByUserId(userId) | ユーザーの支払い一覧 |
| PR-007 | ステータスフィルタ | findByStatus(CAPTURED) | 指定ステータスのみ |
| PR-008 | 冪等レコード保存 | saveIdempotencyRecord() | 保存成功 |
| PR-009 | 冪等レコード検索 | findIdempotencyRecord(key) | 保存済みレスポンス |
| PR-010 | 期限切れレコード削除 | deleteExpiredIdempotencyRecords() | 期限切れのみ削除 |

#### 実装例

```java
@SpringBootTest
@Transactional
@DisplayName("PaymentRepository Integration Tests (PAY-TEST-04)")
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Clock fixedClock;
    private BookingId bookingId;
    private UserId userId;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T10:00:00Z"),
            ZoneOffset.UTC
        );
        bookingId = BookingId.of("booking-001");
        userId = UserId.of("user-001");
    }

    @Test
    @DisplayName("PR-001: 支払いをDBに保存できる")
    void save_PersistsPaymentToDatabase() {
        Payment payment = createPayment();

        Payment saved = paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        Optional<Payment> found = paymentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBookingId()).isEqualTo(bookingId);
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("PR-004: IdempotencyKeyで支払いを検索できる")
    void findByIdempotencyKey_ReturnsPayment() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);
        Payment payment = Payment.create(
            bookingId, userId, Money.of(10000, Currency.JPY), key, null, fixedClock
        );
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        Optional<Payment> found = paymentRepository.findByIdempotencyKey(key);

        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo(key);
    }

    @Test
    @DisplayName("PR-005: 重複IdempotencyKeyは一意制約違反をスローする")
    void save_WithDuplicateIdempotencyKey_ThrowsException() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);

        Payment first = Payment.create(
            bookingId, userId, Money.of(10000, Currency.JPY), key, null, fixedClock
        );
        paymentRepository.save(first);
        entityManager.flush();

        Payment duplicate = Payment.create(
            BookingId.of("booking-002"), userId, Money.of(5000, Currency.JPY), key, null, fixedClock
        );

        assertThatThrownBy(() -> {
            paymentRepository.save(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("PR-008: 冪等レコードを保存できる")
    void saveIdempotencyRecord_Succeeds() {
        IdempotencyKey key = IdempotencyKey.generate(fixedClock);
        IdempotencyRecord record = IdempotencyRecord.create(
            key,
            "abc123hash",
            201,
            "{\"id\": \"payment-001\"}",
            fixedClock
        );

        idempotencyRecordRepository.save(record);
        entityManager.flush();
        entityManager.clear();

        Optional<IdempotencyRecord> found = idempotencyRecordRepository.findByKey(key);

        assertThat(found).isPresent();
        assertThat(found.get().getResponseStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("PR-010: 期限切れ冪等レコードを削除できる")
    void deleteExpiredIdempotencyRecords_DeletesOnlyExpired() {
        // 期限切れレコード
        IdempotencyKey expiredKey = IdempotencyKey.of(
            UUID.randomUUID(),
            fixedClock.instant().minus(Duration.ofHours(25))
        );
        IdempotencyRecord expiredRecord = IdempotencyRecord.create(
            expiredKey, "hash1", 200, "{}", fixedClock
        );
        idempotencyRecordRepository.save(expiredRecord);

        // 有効なレコード
        IdempotencyKey validKey = IdempotencyKey.generate(fixedClock);
        IdempotencyRecord validRecord = IdempotencyRecord.create(
            validKey, "hash2", 200, "{}", fixedClock
        );
        idempotencyRecordRepository.save(validRecord);
        entityManager.flush();

        int deleted = idempotencyRecordRepository.deleteExpired(fixedClock.instant());
        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(idempotencyRecordRepository.findByKey(expiredKey)).isEmpty();
        assertThat(idempotencyRecordRepository.findByKey(validKey)).isPresent();
    }

    private Payment createPayment() {
        return Payment.create(
            bookingId,
            userId,
            Money.of(10000, Currency.JPY),
            IdempotencyKey.generate(fixedClock),
            "Test payment",
            fixedClock
        );
    }
}
```

### 2.5 冪等性テスト：同一Idempotency-Key再送（PAY-TEST-05）

冪等性保証の検証テスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| IDP-001 | 初回リクエスト | 新規IdempotencyKey | 201 Created + 支払い作成 |
| IDP-002 | 同一キー・同一内容再送 | ハッシュ一致 | 200 OK + 保存済みレスポンス |
| IDP-003 | 同一キー・異なる内容 | ハッシュ不一致 | 409 Conflict |
| IDP-004 | 期限切れキー再利用 | 24時間経過 | 201 Created（新規扱い） |
| IDP-005 | 並行リクエスト | 同一キーで同時送信 | 1件のみ作成 + 他は冪等レスポンス |
| IDP-006 | 処理中の再送 | 初回処理が未完了 | 409 Conflict or 待機 |
| IDP-007 | 失敗後の再送 | 初回が失敗 | 同じ失敗レスポンス |

#### 実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Idempotency Tests (PAY-TEST-05)")
class IdempotencyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    private String accessToken;

    @BeforeEach
    void setUp() {
        accessToken = authenticateAndGetToken("test@example.com", "Password123!");
    }

    @Test
    @DisplayName("IDP-001: 初回リクエストは支払いを作成する")
    void firstRequest_CreatesPayment() {
        String idempotencyKey = UUID.randomUUID().toString();
        CreatePaymentRequest request = new CreatePaymentRequest(
            "booking-001",
            10000,
            "JPY"
        );

        ResponseEntity<PaymentResponse> response = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(request, accessToken, idempotencyKey),
            PaymentResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("IDP-002: 同一キー・同一内容の再送は保存済みレスポンスを返す")
    void duplicateRequest_SameContent_ReturnsCachedResponse() {
        String idempotencyKey = UUID.randomUUID().toString();
        CreatePaymentRequest request = new CreatePaymentRequest(
            "booking-001",
            10000,
            "JPY"
        );

        // 初回リクエスト
        ResponseEntity<PaymentResponse> firstResponse = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(request, accessToken, idempotencyKey),
            PaymentResponse.class
        );
        String firstPaymentId = firstResponse.getBody().id();

        // 同一内容で再送
        ResponseEntity<PaymentResponse> secondResponse = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(request, accessToken, idempotencyKey),
            PaymentResponse.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getBody().id()).isEqualTo(firstPaymentId);

        // 支払いが1件のみ作成されていることを確認
        long count = paymentRepository.countByBookingId(BookingId.of("booking-001"));
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("IDP-003: 同一キー・異なる内容は409 Conflictを返す")
    void duplicateRequest_DifferentContent_Returns409() {
        String idempotencyKey = UUID.randomUUID().toString();

        // 初回リクエスト
        CreatePaymentRequest firstRequest = new CreatePaymentRequest(
            "booking-001",
            10000,
            "JPY"
        );
        restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(firstRequest, accessToken, idempotencyKey),
            PaymentResponse.class
        );

        // 異なる金額で再送
        CreatePaymentRequest secondRequest = new CreatePaymentRequest(
            "booking-001",
            20000, // 異なる金額
            "JPY"
        );
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(secondRequest, accessToken, idempotencyKey),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("idempotency_key_conflict");
    }

    @Test
    @DisplayName("IDP-005: 並行リクエストは1件のみ作成される")
    void concurrentRequests_CreateOnlyOne() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreatePaymentRequest request = new CreatePaymentRequest(
            "booking-concurrent",
            10000,
            "JPY"
        );

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ResponseEntity<?> response = restTemplate.exchange(
                        "/payments",
                        HttpMethod.POST,
                        createRequest(request, accessToken, idempotencyKey),
                        Object.class
                    );
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else if (response.getStatusCode() == HttpStatus.OK) {
                        duplicateCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 全スレッド同時スタート
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 1件のみ作成
        long count = paymentRepository.countByBookingId(BookingId.of("booking-concurrent"));
        assertThat(count).isEqualTo(1);
        // 1回成功、残りは冪等レスポンス
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateCount.get()).isEqualTo(numThreads - 1);
    }

    private <T> HttpEntity<T> createRequest(T body, String token, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);
        return new HttpEntity<>(body, headers);
    }
}
```

### 2.6 境界値テスト：金額境界（PAY-TEST-06）

金額の境界値と制約のテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| AMT-001 | 最小金額（1円/1セント） | amount=1 | 正常作成 |
| AMT-002 | 0円 | amount=0 | 400 Bad Request |
| AMT-003 | 負の金額 | amount=-100 | 400 Bad Request |
| AMT-004 | 大金額 | amount=999,999,999 | 正常作成 |
| AMT-005 | 整数最大値 | amount=Integer.MAX_VALUE | 正常作成または上限エラー |
| AMT-006 | 通貨JPY | currency=JPY | 正常作成（円単位） |
| AMT-007 | 通貨USD | currency=USD | 正常作成（セント単位） |
| AMT-008 | 不正通貨コード | currency=XXX | 400 Bad Request |
| AMT-009 | 部分キャプチャ境界 | captureAmount=amount | 正常（全額） |
| AMT-010 | 部分キャプチャ0 | captureAmount=0 | 400 Bad Request |
| AMT-011 | キャプチャ超過 | captureAmount > amount | 400 Bad Request |
| AMT-012 | 部分返金境界 | refundAmount=capturedAmount | 正常（全額返金） |
| AMT-013 | 複数回返金合計 | 返金合計 <= キャプチャ額 | 正常 |
| AMT-014 | 複数回返金超過 | 返金合計 > キャプチャ額 | 400 Bad Request |

#### 実装例

```java
@Nested
@DisplayName("Amount Boundary Tests (PAY-TEST-06)")
class AmountBoundaryTest {

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T10:00:00Z"),
            ZoneOffset.UTC
        );
    }

    @Test
    @DisplayName("AMT-001: 最小金額（1）で支払いを作成できる")
    void minAmount_IsValid() {
        Money money = Money.of(1, Currency.JPY);
        Payment payment = Payment.create(
            BookingId.of("booking-001"),
            UserId.of("user-001"),
            money,
            IdempotencyKey.generate(fixedClock),
            null,
            fixedClock
        );

        assertThat(payment.getMoney().amount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("AMT-002/003: 0以下の金額はIllegalArgumentExceptionをスローする")
    void zeroOrNegativeAmount_ThrowsException(int invalidAmount) {
        assertThatThrownBy(() -> Money.of(invalidAmount, Currency.JPY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AMT-004: 大金額でも作成できる")
    void largeAmount_IsValid() {
        Money money = Money.of(999_999_999, Currency.JPY);

        assertThat(money.amount()).isEqualTo(999_999_999);
    }

    @Test
    @DisplayName("AMT-009: キャプチャ額=与信額（全額）は正常")
    void captureFullAmount_Succeeds() {
        Payment payment = createAuthorizedPayment(10000);

        payment.capture(10000, fixedClock); // 全額

        assertThat(payment.getCapturedAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("AMT-010: キャプチャ額0はIllegalArgumentExceptionをスローする")
    void captureZeroAmount_ThrowsException() {
        Payment payment = createAuthorizedPayment(10000);

        assertThatThrownBy(() -> payment.capture(0, fixedClock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("capture amount must be positive");
    }

    @Test
    @DisplayName("AMT-011: キャプチャ額超過はIllegalArgumentExceptionをスローする")
    void captureExceedingAmount_ThrowsException() {
        Payment payment = createAuthorizedPayment(10000);

        assertThatThrownBy(() -> payment.capture(10001, fixedClock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds authorized amount");
    }

    @Test
    @DisplayName("AMT-012: 返金額=キャプチャ額（全額返金）は正常")
    void refundFullAmount_Succeeds() {
        Payment payment = createCapturedPayment(10000);

        payment.refund(10000, fixedClock);

        assertThat(payment.getRefundedAmount()).isEqualTo(10000);
    }

    @ParameterizedTest
    @CsvSource({
        "10000, 5000, 5000, true",   // 2回で全額返金
        "10000, 3000, 3000, true",   // 2回で6000返金（残り4000）
        "10000, 5000, 6000, false"   // 2回で11000返金（超過）
    })
    @DisplayName("AMT-013/014: 複数回返金の合計額チェック")
    void multipleRefunds_CheckTotal(
        int capturedAmount,
        int firstRefund,
        int secondRefund,
        boolean shouldSucceed
    ) {
        Payment payment = createCapturedPayment(capturedAmount);
        payment.refund(firstRefund, fixedClock);

        if (shouldSucceed) {
            assertThatNoException().isThrownBy(() ->
                payment.refund(secondRefund, fixedClock)
            );
        } else {
            assertThatThrownBy(() -> payment.refund(secondRefund, fixedClock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds remaining");
        }
    }

    private Payment createAuthorizedPayment(int amount) {
        Payment payment = Payment.create(
            BookingId.of("booking-001"),
            UserId.of("user-001"),
            Money.of(amount, Currency.JPY),
            IdempotencyKey.generate(fixedClock),
            null,
            fixedClock
        );
        payment.authorize("gw-tx-001", fixedClock);
        return payment;
    }

    private Payment createCapturedPayment(int amount) {
        Payment payment = createAuthorizedPayment(amount);
        payment.capture(amount, fixedClock);
        return payment;
    }
}
```

### 2.7 E2E テスト：create→capture→refund フロー（PAY-TEST-07）

支払いライフサイクル全体のE2Eテスト。

#### テストシナリオ

```
シナリオ: 支払いの作成→キャプチャ→返金
  Given ログイン済みユーザーと予約がある
  When POST /payments で支払いを作成する
  Then 201 Created + PENDING状態

  When 外部ゲートウェイが与信成功を返す
  Then AUTHORIZED状態に遷移

  When POST /payments/{id}/capture でキャプチャする
  Then 200 OK + CAPTURED状態 + BookingがCONFIRMEDに遷移

  When POST /payments/{id}/refund で返金する
  Then 200 OK + REFUNDED状態
```

#### テストケース一覧

| ID | テストケース | 期待結果 |
|----|-------------|----------|
| E2E-PAY-001 | 正常フロー：create→authorize→capture→refund | 各ステップで期待レスポンス |
| E2E-PAY-002 | 与信失敗 | FAILED状態 + 失敗理由 |
| E2E-PAY-003 | 部分キャプチャ | 指定金額のみキャプチャ |
| E2E-PAY-004 | 部分返金 | 指定金額のみ返金 |
| E2E-PAY-005 | キャプチャでBooking確定 | BookingがCONFIRMED |
| E2E-PAY-006 | 冪等リクエスト | 同一Idempotency-Keyで同一レスポンス |
| E2E-PAY-007 | ゲートウェイタイムアウト | 適切なエラーハンドリング |

#### 実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Payment E2E Tests (PAY-TEST-07)")
class PaymentE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private PaymentGatewayPort paymentGateway;

    private String accessToken;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        accessToken = authenticateAndGetToken("test@example.com", "Password123!");
        testBooking = createTestBooking();
    }

    @Test
    @DisplayName("E2E-PAY-001: 支払いの作成→キャプチャ→返金フロー")
    void fullPaymentLifecycle_Succeeds() {
        // ゲートウェイのモック設定
        when(paymentGateway.authorize(any())).thenReturn(
            new GatewayResponse("gw-tx-001", true, null)
        );
        when(paymentGateway.capture(any())).thenReturn(
            new GatewayResponse("gw-tx-001", true, null)
        );
        when(paymentGateway.refund(any())).thenReturn(
            new GatewayResponse("gw-tx-001", true, null)
        );

        String idempotencyKey = UUID.randomUUID().toString();

        // Step 1: 支払い作成
        CreatePaymentRequest createRequest = new CreatePaymentRequest(
            testBooking.getId().value(),
            10000,
            "JPY"
        );

        ResponseEntity<PaymentResponse> createResponse = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(createRequest, accessToken, idempotencyKey),
            PaymentResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PaymentResponse created = createResponse.getBody();
        assertThat(created.status()).isEqualTo("AUTHORIZED"); // 自動与信
        String paymentId = created.id();

        // Step 2: キャプチャ
        ResponseEntity<PaymentResponse> captureResponse = restTemplate.exchange(
            "/payments/" + paymentId + "/capture",
            HttpMethod.POST,
            createEmptyRequest(accessToken),
            PaymentResponse.class
        );

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captureResponse.getBody().status()).isEqualTo("CAPTURED");
        assertThat(captureResponse.getBody().capturedAmount()).isEqualTo(10000);

        // Bookingが確定されていることを確認
        Booking booking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Step 3: 返金
        RefundPaymentRequest refundRequest = new RefundPaymentRequest(10000);
        ResponseEntity<PaymentResponse> refundResponse = restTemplate.exchange(
            "/payments/" + paymentId + "/refund",
            HttpMethod.POST,
            createRequest(refundRequest, accessToken, null),
            PaymentResponse.class
        );

        assertThat(refundResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refundResponse.getBody().status()).isEqualTo("REFUNDED");
        assertThat(refundResponse.getBody().refundedAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("E2E-PAY-002: 与信失敗でFAILED状態になる")
    void authorizeFailure_ResultsInFailedStatus() {
        when(paymentGateway.authorize(any())).thenReturn(
            new GatewayResponse(null, false, "Card declined")
        );

        CreatePaymentRequest request = new CreatePaymentRequest(
            testBooking.getId().value(),
            10000,
            "JPY"
        );

        ResponseEntity<PaymentResponse> response = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(request, accessToken, UUID.randomUUID().toString()),
            PaymentResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("FAILED");
        assertThat(response.getBody().failureReason()).isEqualTo("Card declined");
    }

    @Test
    @DisplayName("E2E-PAY-003: 部分キャプチャが正しく動作する")
    void partialCapture_CapturesPartialAmount() {
        when(paymentGateway.authorize(any())).thenReturn(
            new GatewayResponse("gw-tx-001", true, null)
        );
        when(paymentGateway.capture(any())).thenReturn(
            new GatewayResponse("gw-tx-001", true, null)
        );

        // 支払い作成（10000円）
        CreatePaymentRequest createRequest = new CreatePaymentRequest(
            testBooking.getId().value(),
            10000,
            "JPY"
        );
        ResponseEntity<PaymentResponse> createResponse = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(createRequest, accessToken, UUID.randomUUID().toString()),
            PaymentResponse.class
        );
        String paymentId = createResponse.getBody().id();

        // 部分キャプチャ（5000円）
        CapturePaymentRequest captureRequest = new CapturePaymentRequest(5000);
        ResponseEntity<PaymentResponse> captureResponse = restTemplate.exchange(
            "/payments/" + paymentId + "/capture",
            HttpMethod.POST,
            createRequest(captureRequest, accessToken, null),
            PaymentResponse.class
        );

        assertThat(captureResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captureResponse.getBody().capturedAmount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("E2E-PAY-007: ゲートウェイタイムアウト時のエラーハンドリング")
    void gatewayTimeout_ReturnsAppropriateError() {
        when(paymentGateway.authorize(any())).thenThrow(
            new GatewayTimeoutException("Connection timeout")
        );

        CreatePaymentRequest request = new CreatePaymentRequest(
            testBooking.getId().value(),
            10000,
            "JPY"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/payments",
            HttpMethod.POST,
            createRequest(request, accessToken, UUID.randomUUID().toString()),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody().error()).isEqualTo("gateway_timeout");
    }

    private Booking createTestBooking() {
        Clock clock = Clock.systemUTC();
        TimeRange range = TimeRange.of(
            Instant.now().plus(Duration.ofDays(1)),
            Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(1)),
            clock
        );
        Booking booking = Booking.create(
            UserId.of("test-user-id"),
            ResourceId.of("resource-001"),
            range,
            null,
            clock
        );
        return bookingRepository.save(booking);
    }

    private <T> HttpEntity<T> createRequest(T body, String token, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> createEmptyRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
```

---

## 3. テスト環境

### 3.1 ユニットテスト

- **フレームワーク**: JUnit 5 + AssertJ + Mockito
- **実行**: Gradle `test` タスク
- **カバレッジ目標**: 80%以上（ドメインロジック）

### 3.2 統合テスト

- **フレームワーク**: Spring Boot Test + Testcontainers
- **データベース**: PostgreSQL（Testcontainers）
- **実行**: Gradle `integrationTest` タスク

### 3.3 E2Eテスト

- **フレームワーク**: Spring Boot Test + TestRestTemplate
- **環境**: 完全なアプリケーションコンテキスト
- **外部依存**: MockBeanでPaymentGatewayをモック
- **実行**: Gradle `e2eTest` タスク

---

## 4. テストデータ管理

### 4.1 テストデータ原則

1. **独立性**: 各テストは独自のテストデータを持つ
2. **再現性**: テストデータは固定値または明示的なシード
3. **クリーンアップ**: テスト終了後にデータをクリア
4. **PII回避**: テストデータにも本番類似のPIIを使用しない

### 4.2 テストフィクスチャ例

```java
public class PaymentTestFixtures {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-01-18T10:00:00Z"),
        ZoneOffset.UTC
    );

    public static Payment createPendingPayment() {
        return Payment.create(
            BookingId.of("test-booking-001"),
            UserId.of("test-user-001"),
            Money.of(10000, Currency.JPY),
            IdempotencyKey.generate(FIXED_CLOCK),
            "Test payment",
            FIXED_CLOCK
        );
    }

    public static Payment createAuthorizedPayment() {
        Payment payment = createPendingPayment();
        payment.authorize("gw-tx-test-001", FIXED_CLOCK);
        return payment;
    }

    public static Payment createCapturedPayment() {
        Payment payment = createAuthorizedPayment();
        payment.capture(null, FIXED_CLOCK);
        return payment;
    }

    public static Payment createRefundedPayment() {
        Payment payment = createCapturedPayment();
        payment.refund(null, FIXED_CLOCK);
        return payment;
    }

    public static Payment createFailedPayment() {
        Payment payment = createPendingPayment();
        payment.fail("Test failure reason", FIXED_CLOCK);
        return payment;
    }
}
```
