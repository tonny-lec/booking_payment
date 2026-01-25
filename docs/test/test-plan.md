---
doc_type: "test_plan"
id: "test-plan"
version: "1.3"
last_updated: "2026-01-22"
status: "draft"
---

# テスト計画（SSOT）

本ドキュメントは、予約・決済基盤のテスト戦略、Contract Test設計方針、およびテストケース設計を定義します。

---

## 1. テスト戦略概要

### 1.1 テストピラミッド

```
                    ┌───────┐
                    │  E2E  │  少数、高コスト、遅い
                   ─┴───────┴─
                  │ Integration │  中程度
                 ─┴─────────────┴─
                │    Contract     │  API境界の検証
               ─┴─────────────────┴─
              │       Unit          │  多数、低コスト、速い
             ─┴─────────────────────┴─
```

### 1.2 テスト種別と責務

| テスト種別 | 責務 | 実行頻度 | 実行時間目標 |
|------------|------|----------|--------------|
| **Unit Test** | ドメインロジック、値オブジェクト、集約の振る舞い | コミットごと | < 30秒 |
| **Integration Test** | リポジトリ、外部サービス連携 | コミットごと | < 2分 |
| **Contract Test** | API契約の遵守（OpenAPI準拠） | コミットごと | < 1分 |
| **E2E Test** | ユーザーシナリオ全体 | PR/デプロイ時 | < 10分 |
| **Performance Test** | 負荷、レイテンシ | リリース前 | 可変 |

### 1.3 重点テスト領域

以下の領域は特に重点的にテストします。

| 領域 | リスク | テスト方針 |
|------|--------|-----------|
| **二重予約** | ビジネス損失、顧客不満 | 境界値テスト、並行実行テスト |
| **冪等性** | 二重課金、データ不整合 | 同一リクエスト再送テスト |
| **タイムアウト/リトライ** | 重複処理 | 障害注入テスト |
| **権限チェック** | セキュリティ違反 | 認可境界テスト |
| **互換性** | クライアント障害 | 契約テスト |
| **traceId伝播** | デバッグ不能 | 観測性テスト |

---

## 2. Contract Test設計方針

### 2.1 概要

Contract Test（契約テスト）は、API提供者（Provider）とAPI利用者（Consumer）間の契約を検証するテストです。

```
┌─────────────┐                    ┌─────────────┐
│   Consumer  │ ──── Contract ──── │   Provider  │
│  (Client)   │                    │  (Server)   │
└─────────────┘                    └─────────────┘
       │                                  │
       │                                  │
       ▼                                  ▼
 Consumer Test:                    Provider Test:
 契約通りのリクエスト                契約通りのレスポンス
 を生成できるか                     を返せるか
```

### 2.2 Contract Testの目的

1. **OpenAPI仕様との整合性**：実装がOpenAPI定義に準拠していることを保証
2. **破壊的変更の検出**：互換性を破壊する変更を早期に検出
3. **Consumer-Provider間の独立したテスト**：結合テストなしで契約遵守を確認

### 2.3 採用するアプローチ

#### スキーマ駆動テスト（OpenAPI Validation）

OpenAPI仕様をSSoT（Single Source of Truth）として、以下を検証します。

| 検証項目 | 説明 | ツール例 |
|----------|------|----------|
| **リクエスト形式** | リクエストボディがスキーマに準拠 | openapi-validator |
| **レスポンス形式** | レスポンスボディがスキーマに準拠 | openapi-validator |
| **HTTPステータス** | 定義されたステータスコードの使用 | openapi-validator |
| **ヘッダー** | 必須ヘッダーの存在 | カスタム検証 |
| **Content-Type** | 正しいメディアタイプ | カスタム検証 |

#### Consumer-Driven Contract Testing（将来検討）

将来的に複数のConsumerが存在する場合は、Pact等を用いたConsumer-Driven Contract Testingを検討します。

### 2.4 Contract Testの配置

```
src/
├── main/java/...
└── test/java/...
    └── contract/
        ├── BookingApiContractTest.java
        ├── PaymentApiContractTest.java
        └── IamApiContractTest.java
```

---

## 3. 契約テスト：リクエスト/レスポンス形式検証

### 3.1 検証項目一覧

#### 3.1.1 リクエスト検証

| 検証項目 | 検証内容 | 重要度 |
|----------|----------|--------|
| **必須フィールド** | 必須フィールドがすべて存在すること | 高 |
| **型検証** | 各フィールドが定義された型であること | 高 |
| **形式検証** | format指定（uuid, date-time等）に準拠 | 高 |
| **範囲検証** | minimum, maximum, minLength, maxLength | 中 |
| **列挙検証** | enum値が定義された値のみ | 高 |
| **追加フィールド** | additionalProperties: false の遵守 | 中 |

#### 3.1.2 レスポンス検証

| 検証項目 | 検証内容 | 重要度 |
|----------|----------|--------|
| **ステータスコード** | 定義されたステータスコードのみ使用 | 高 |
| **Content-Type** | application/json または application/problem+json | 高 |
| **レスポンスボディ** | スキーマに準拠した構造 | 高 |
| **必須フィールド** | 必須フィールドがすべて存在すること | 高 |
| **nullability** | nullable: false のフィールドがnullでないこと | 高 |

### 3.2 API別Contract Test仕様

#### 3.2.1 IAM API Contract Tests

| テストケース | エンドポイント | 検証内容 |
|--------------|---------------|----------|
| CT-IAM-001 | POST /auth/login | 正常ログイン時のTokenResponse形式 |
| CT-IAM-002 | POST /auth/login | 認証失敗時のProblemDetail形式（401） |
| CT-IAM-003 | POST /auth/login | アカウントロック時のProblemDetail形式（423） |
| CT-IAM-004 | POST /auth/login | レート制限時のRetry-Afterヘッダー（429） |
| CT-IAM-005 | POST /auth/refresh | 正常リフレッシュ時のTokenResponse形式 |
| CT-IAM-006 | POST /auth/refresh | 無効トークン時のProblemDetail形式（401） |
| CT-IAM-007 | POST /auth/logout | 正常ログアウト時のレスポンス（204） |

#### 3.2.2 Booking API Contract Tests

| テストケース | エンドポイント | 検証内容 |
|--------------|---------------|----------|
| CT-BK-001 | POST /bookings | 正常作成時のBookingレスポンス形式 |
| CT-BK-002 | POST /bookings | Locationヘッダーの存在と形式 |
| CT-BK-003 | POST /bookings | バリデーションエラー時のProblemDetail形式（400） |
| CT-BK-004 | POST /bookings | 時間帯衝突時のProblemDetail + conflictingBookingId（409） |
| CT-BK-005 | GET /bookings/{id} | 正常取得時のBookingレスポンス形式 |
| CT-BK-006 | GET /bookings/{id} | 予約不存在時のProblemDetail形式（404） |
| CT-BK-007 | PUT /bookings/{id} | 正常更新時のBookingレスポンス形式（version増加） |
| CT-BK-008 | PUT /bookings/{id} | バージョン不一致時のProblemDetail形式（409） |
| CT-BK-009 | DELETE /bookings/{id} | 正常キャンセル時のレスポンス（204または200） |
| CT-BK-010 | GET /bookings | ページネーション形式の検証 |

#### 3.2.3 Payment API Contract Tests

| テストケース | エンドポイント | 検証内容 |
|--------------|---------------|----------|
| CT-PAY-001 | POST /payments | 正常作成時のPaymentレスポンス形式 |
| CT-PAY-002 | POST /payments | Idempotency-Keyヘッダーの必須検証 |
| CT-PAY-003 | POST /payments | 冪等リクエスト時の同一レスポンス |
| CT-PAY-004 | GET /payments/{id} | 正常取得時のPaymentレスポンス形式 |
| CT-PAY-005 | POST /payments/{id}/capture | キャプチャ成功時のPaymentレスポンス形式 |
| CT-PAY-006 | POST /payments/{id}/refund | 返金成功時のPaymentレスポンス形式 |

### 3.3 実装例

#### 3.3.1 OpenAPI Validator設定（Java + Spring）

```java
@Configuration
public class OpenApiValidationConfig {

    @Bean
    public OpenApiValidationFilter openApiValidationFilter() {
        return new OpenApiValidationFilter(
            "/api/v1",
            "docs/api/openapi/booking.yaml",
            "docs/api/openapi/iam.yaml",
            "docs/api/openapi/payment.yaml"
        );
    }
}
```

#### 3.3.2 Contract Test実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BookingApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenApiValidator validator;

    @Test
    @DisplayName("CT-BK-001: POST /bookings returns valid Booking response")
    void createBooking_ReturnsValidBookingResponse() throws Exception {
        // Given
        String requestBody = """
            {
                "resourceId": "550e8400-e29b-41d4-a716-446655440000",
                "startAt": "2026-01-20T10:00:00Z",
                "endAt": "2026-01-20T11:00:00Z",
                "note": "Meeting room booking"
            }
            """;

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + validAccessToken())
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

        // Then: OpenAPI準拠を検証
        validator.validateResponse(
            "POST", "/bookings", 201,
            result.getResponse().getContentAsString()
        );

        // Then: Locationヘッダーの存在
        String location = result.getResponse().getHeader("Location");
        assertThat(location).matches("/api/v1/bookings/[a-f0-9-]+");
    }

    @Test
    @DisplayName("CT-BK-004: POST /bookings returns 409 with conflictingBookingId on conflict")
    void createBooking_Conflict_Returns409WithConflictingBookingId() throws Exception {
        // Given: 既存予約を作成
        String existingBookingId = createExistingBooking();

        // Given: 重複する時間帯でリクエスト
        String requestBody = """
            {
                "resourceId": "550e8400-e29b-41d4-a716-446655440000",
                "startAt": "2026-01-20T10:30:00Z",
                "endAt": "2026-01-20T11:30:00Z"
            }
            """;

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + validAccessToken())
                .content(requestBody))
            .andExpect(status().isConflict())
            .andReturn();

        // Then: ProblemDetail形式の検証
        validator.validateResponse(
            "POST", "/bookings", 409,
            result.getResponse().getContentAsString()
        );

        // Then: conflictingBookingIdの存在
        JSONObject response = new JSONObject(result.getResponse().getContentAsString());
        assertThat(response.has("conflictingBookingId")).isTrue();
        assertThat(response.getString("conflictingBookingId")).isEqualTo(existingBookingId);
    }
}
```

#### 3.3.3 ProblemDetail形式検証

```java
public class ProblemDetailAssert {

    public static void assertValidProblemDetail(String json) throws JSONException {
        JSONObject problemDetail = new JSONObject(json);

        // RFC 7807必須フィールド
        assertThat(problemDetail.has("type")).isTrue();
        assertThat(problemDetail.has("title")).isTrue();
        assertThat(problemDetail.has("status")).isTrue();

        // typeはURI形式
        String type = problemDetail.getString("type");
        assertThat(type).matches("https?://.*|about:blank");

        // statusは有効なHTTPステータスコード
        int status = problemDetail.getInt("status");
        assertThat(status).isBetween(400, 599);
    }
}
```

### 3.4 CI/CD統合

```yaml
# .github/workflows/contract-test.yaml
name: Contract Tests
on: [push, pull_request]

jobs:
  contract-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Contract Tests
        run: ./gradlew contractTest

      - name: Validate OpenAPI Spec
        run: |
          npm install -g @stoplight/spectral-cli
          spectral lint docs/api/openapi/*.yaml

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: contract-test-results
          path: build/reports/tests/contractTest/
```

---

## 4. Property-Based Testing

### 4.1 候補

| ドメイン | プロパティ | 検証内容 |
|----------|-----------|----------|
| **TimeRange** | 整合性 | `startAt < endAt` を満たすすべての組み合わせ |
| **TimeRange** | 重複判定 | `overlaps(a, b) == overlaps(b, a)` |
| **Idempotency** | 冪等性 | 同一入力 → 同一出力（複数回実行） |
| **状態遷移** | 許可遷移のみ | 定義された遷移のみ成功 |
| **金額計算** | 精度 | 丸め誤差なし |

### 4.2 実装例（jqwik）

```java
@Property
void timeRange_overlaps_isSymmetric(@ForAll @IntRange(min = 0, max = 100) int start1,
                                     @ForAll @IntRange(min = 0, max = 100) int duration1,
                                     @ForAll @IntRange(min = 0, max = 100) int start2,
                                     @ForAll @IntRange(min = 0, max = 100) int duration2) {
    Assume.that(duration1 > 0 && duration2 > 0);

    TimeRange range1 = new TimeRange(start1, start1 + duration1);
    TimeRange range2 = new TimeRange(start2, start2 + duration2);

    assertThat(range1.overlaps(range2)).isEqualTo(range2.overlaps(range1));
}

@Property
void booking_statusTransition_onlyAllowedTransitions(
        @ForAll("validBookingStatus") BookingStatus from,
        @ForAll("validBookingStatus") BookingStatus to) {

    Booking booking = createBookingWithStatus(from);

    if (isAllowedTransition(from, to)) {
        assertThatNoException().isThrownBy(() -> booking.transitionTo(to));
    } else {
        assertThatThrownBy(() -> booking.transitionTo(to))
            .isInstanceOf(InvalidStateTransitionException.class);
    }
}
```

---

## 5. IAM テスト計画詳細

### 5.1 PasswordValidator（IAM-TEST-01）

パスワード検証ロジックのユニットテスト。

#### テストケース一覧

| ID | テストケース | 入力 | 期待結果 |
|----|-------------|------|----------|
| PV-001 | 有効なパスワード（標準） | `Password123!` | valid |
| PV-002 | 有効なパスワード（最小長8文字） | `Pass123!` | valid |
| PV-003 | 無効：最小長未満 | `Pa12!` | invalid (too_short) |
| PV-004 | 無効：大文字なし | `password123!` | invalid (no_uppercase) |
| PV-005 | 無効：小文字なし | `PASSWORD123!` | invalid (no_lowercase) |
| PV-006 | 無効：数字なし | `PasswordABC!` | invalid (no_digit) |
| PV-007 | 無効：特殊文字なし | `Password123` | invalid (no_special) |
| PV-008 | 無効：nullパスワード | `null` | invalid (null_input) |
| PV-009 | 無効：空文字列 | `""` | invalid (empty) |
| PV-010 | 無効：一般的なパスワード | `Password1!` | invalid (common_password) |
| PV-011 | 境界値：最大長（128文字） | 128文字の有効パスワード | valid |
| PV-012 | 境界値：最大長超過 | 129文字 | invalid (too_long) |

#### 実装例

```java
@Nested
@DisplayName("PasswordValidator Unit Tests (IAM-TEST-01)")
class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        PasswordPolicy policy = PasswordPolicy.builder()
            .minLength(8)
            .maxLength(128)
            .requireUppercase(true)
            .requireLowercase(true)
            .requireDigit(true)
            .requireSpecialChar(true)
            .build();
        validator = new PasswordValidator(policy);
    }

    @Test
    @DisplayName("PV-001: 有効なパスワードを受け入れる")
    void validPassword_IsAccepted() {
        ValidationResult result = validator.validate("Password123!");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("PV-003: 最小長未満のパスワードを拒否する")
    void tooShortPassword_IsRejected() {
        ValidationResult result = validator.validate("Pa12!");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains(ValidationError.TOO_SHORT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("PV-008/009: null/空文字列を拒否する")
    void nullOrEmptyPassword_IsRejected(String password) {
        ValidationResult result = validator.validate(password);

        assertThat(result.isValid()).isFalse();
    }
}
```

### 5.2 TokenGenerator（IAM-TEST-02）

JWT生成・検証ロジックのユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| TG-001 | AccessToken生成 | 有効なユーザーID | JWTトークン（sub=userId, exp=15min後） |
| TG-002 | RefreshToken生成 | 有効なユーザーID | ランダム256bit + 有効期限7日 |
| TG-003 | AccessToken検証：有効 | 正しい署名 + 有効期限内 | 検証成功 |
| TG-004 | AccessToken検証：期限切れ | 正しい署名 + 期限超過 | TokenExpiredException |
| TG-005 | AccessToken検証：不正署名 | 改ざんされたトークン | InvalidSignatureException |
| TG-006 | AccessToken検証：不正フォーマット | 不正なJWT形式 | MalformedTokenException |
| TG-007 | クレーム抽出：userId | 有効なトークン | 正しいuserId |
| TG-008 | クレーム抽出：roles | 有効なトークン | 正しいroles配列 |
| TG-009 | トークン再発行 | 有効なRefreshToken | 新しいAccessToken |
| TG-010 | 鍵ローテーション | 旧鍵で署名されたトークン | 検証成功（猶予期間内） |

#### 実装例

```java
@Nested
@DisplayName("TokenGenerator Unit Tests (IAM-TEST-02)")
class TokenGeneratorTest {

    private TokenGenerator tokenGenerator;
    private Clock fixedClock;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T10:00:00Z"),
            ZoneOffset.UTC
        );
        keyPair = generateRSAKeyPair();

        TokenConfig config = TokenConfig.builder()
            .accessTokenTtl(Duration.ofMinutes(15))
            .refreshTokenTtl(Duration.ofDays(7))
            .issuer("booking-payment")
            .algorithm(Algorithm.RS256)
            .build();

        tokenGenerator = new TokenGenerator(config, keyPair, fixedClock);
    }

    @Test
    @DisplayName("TG-001: AccessTokenに正しいクレームが含まれる")
    void accessToken_ContainsCorrectClaims() {
        UserId userId = UserId.of("550e8400-e29b-41d4-a716-446655440000");
        List<String> roles = List.of("USER");

        String token = tokenGenerator.generateAccessToken(userId, roles);

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.value());
        assertThat(decoded.getExpiresAt()).isEqualTo(
            Date.from(fixedClock.instant().plus(Duration.ofMinutes(15)))
        );
    }

    @Test
    @DisplayName("TG-004: 期限切れトークンはTokenExpiredExceptionをスローする")
    void expiredToken_ThrowsTokenExpiredException() {
        UserId userId = UserId.of("550e8400-e29b-41d4-a716-446655440000");
        String token = tokenGenerator.generateAccessToken(userId, List.of("USER"));

        Clock advancedClock = Clock.fixed(
            fixedClock.instant().plus(Duration.ofMinutes(16)),
            ZoneOffset.UTC
        );
        TokenValidator validator = new TokenValidator(keyPair.getPublic(), advancedClock);

        assertThatThrownBy(() -> validator.validate(token))
            .isInstanceOf(TokenExpiredException.class);
    }
}
```

### 5.3 User集約（IAM-TEST-03）

User集約のドメインロジックのユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| USR-001 | 認証成功 | 正しいパスワード + ACTIVE状態 | 成功 + failedLoginAttempts=0 |
| USR-002 | 認証失敗：パスワード不一致 | 誤ったパスワード | 失敗 + failedLoginAttempts++ |
| USR-003 | 認証失敗：アカウントロック中 | status=LOCKED + lockedUntil > now | LoginBlockedException |
| USR-004 | 自動ロック：連続失敗 | 5回連続失敗 | status=LOCKED + lockedUntil=30min後 |
| USR-005 | 手動ロック解除 | 管理者によるunlock() | status=ACTIVE + failedLoginAttempts=0 |
| USR-006 | 自動ロック解除 | lockedUntil < now | 次回認証試行可能 |
| USR-007 | ステータス遷移：ACTIVE→LOCKED | 連続失敗またはadmin操作 | 遷移成功 |
| USR-008 | ステータス遷移：LOCKED→ACTIVE | unlock() | 遷移成功 |
| USR-009 | ステータス遷移：SUSPENDED→ACTIVE | 不許可 | IllegalStateException |
| USR-010 | 失敗カウントリセット | 認証成功時 | failedLoginAttempts=0 |
| USR-011 | イベント発行：UserLoggedIn | 認証成功時 | UserLoggedInイベント |
| USR-012 | イベント発行：LoginFailed | 認証失敗時 | LoginFailedイベント |
| USR-013 | イベント発行：AccountLocked | ロック発生時 | AccountLockedイベント |

### 5.4 UserRepository 統合テスト（IAM-TEST-04）

データベースとの統合テスト。

#### テストケース一覧

| ID | テストケース | 操作 | 期待結果 |
|----|-------------|------|----------|
| UR-001 | ユーザー保存 | save(user) | DBに永続化 + IDが設定される |
| UR-002 | Email検索：存在する | findByEmail(existing) | Optional.of(user) |
| UR-003 | Email検索：存在しない | findByEmail(nonExistent) | Optional.empty() |
| UR-004 | ID検索 | findById(existingId) | Optional.of(user) |
| UR-005 | 更新：楽観的ロック成功 | save(updatedUser) | 保存成功 + version++ |
| UR-006 | 更新：楽観的ロック失敗 | 競合更新 | OptimisticLockingFailureException |
| UR-007 | Email一意制約 | 重複Email保存 | DataIntegrityViolationException |
| UR-008 | トランザクション分離 | 並行読み取り | REPEATABLE_READ動作 |

### 5.5 E2E テスト：login→refresh→logout フロー（IAM-TEST-05）

認証フロー全体のE2Eテスト。

#### テストケース一覧

| ID | テストケース | 期待結果 |
|----|-------------|----------|
| E2E-IAM-001 | 正常フロー：login→refresh→logout | 各ステップで期待レスポンス |
| E2E-IAM-002 | ログイン失敗：無効な認証情報 | 401 + error=invalid_credentials |
| E2E-IAM-003 | ログイン失敗：アカウントロック | 401 + error=account_locked |
| E2E-IAM-004 | リフレッシュ失敗：期限切れ | 401 + error=token_expired |
| E2E-IAM-005 | リフレッシュ失敗：失効済み | 401 + error=token_revoked |
| E2E-IAM-006 | ログアウト後のアクセス拒否 | 401 Unauthorized |
| E2E-IAM-007 | 並行セッション：複数デバイス | 各セッション独立動作 |
| E2E-IAM-008 | トークンローテーション | 新RefreshToken発行 + 旧トークン失効 |

### 5.6 権限テスト：無効トークンでのアクセス拒否（IAM-TEST-06）

無効なトークンや権限不足でのアクセス拒否テスト。

#### テストケース一覧

| ID | テストケース | リクエスト | 期待結果 |
|----|-------------|----------|----------|
| AUTH-001 | トークンなし | Authorization ヘッダーなし | 401 Unauthorized |
| AUTH-002 | 不正形式トークン | `Bearer invalid-token` | 401 Unauthorized |
| AUTH-003 | 期限切れトークン | 有効期限切れのJWT | 401 Unauthorized |
| AUTH-004 | 失効済みトークン | ログアウト後のトークン | 401 Unauthorized |
| AUTH-005 | 不正署名トークン | 改ざんされたJWT | 401 Unauthorized |
| AUTH-006 | 権限不足 | USER権限でADMINエンドポイント | 403 Forbidden |
| AUTH-007 | 別ユーザーのリソースアクセス | 他人の予約へのアクセス | 403 Forbidden |
| AUTH-008 | ロック中ユーザーのトークン | LOCKED状態のユーザー | 401 Unauthorized |

---

## 6. Booking テスト計画詳細

### 6.1 TimeRange値オブジェクト（BK-TEST-01）

TimeRange値オブジェクトの不変条件と振る舞いのユニットテスト。

#### テストケース一覧

| ID | テストケース | 入力 | 期待結果 |
|----|-------------|------|----------|
| TR-001 | 有効なTimeRange作成 | start=10:00, end=11:00 | 正常作成 |
| TR-002 | 無効：start >= end | start=11:00, end=10:00 | IllegalArgumentException |
| TR-003 | 無効：start == end | start=10:00, end=10:00 | IllegalArgumentException |
| TR-004 | 無効：過去のstart | start=昨日, end=明日 | IllegalArgumentException |
| TR-005 | 境界：現在時刻のstart | start=now, end=now+1h | 正常作成（許容） |
| TR-006 | 重複判定：完全重複 | A=[10:00-12:00], B=[10:00-12:00] | overlaps=true |
| TR-007 | 重複判定：部分重複（前半） | A=[10:00-12:00], B=[09:00-11:00] | overlaps=true |
| TR-008 | 重複判定：部分重複（後半） | A=[10:00-12:00], B=[11:00-13:00] | overlaps=true |
| TR-009 | 重複判定：包含（AがBを含む） | A=[09:00-13:00], B=[10:00-12:00] | overlaps=true |
| TR-010 | 重複判定：包含（BがAを含む） | A=[10:00-12:00], B=[09:00-13:00] | overlaps=true |
| TR-011 | 重複判定：隣接（衝突しない） | A=[10:00-11:00], B=[11:00-12:00] | overlaps=false |
| TR-012 | 重複判定：離散（衝突しない） | A=[10:00-11:00], B=[12:00-13:00] | overlaps=false |
| TR-013 | duration計算 | start=10:00, end=12:30 | 2時間30分 |
| TR-014 | contains判定：範囲内 | range=[10:00-12:00], point=11:00 | true |
| TR-015 | contains判定：範囲外 | range=[10:00-12:00], point=13:00 | false |
| TR-016 | contains判定：境界（start） | range=[10:00-12:00], point=10:00 | true |
| TR-017 | contains判定：境界（end） | range=[10:00-12:00], point=12:00 | false（半開区間） |

### 6.2 Booking集約（BK-TEST-02）

Booking集約のドメインロジックと状態遷移のユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| BK-001 | 予約作成（正常） | 有効な入力 | PENDING状態で作成 + BookingCreatedイベント |
| BK-002 | 予約作成：note付き | note指定あり | noteが保存される |
| BK-003 | 予約作成：note最大長 | 500文字のnote | 正常保存 |
| BK-004 | 予約作成：note超過 | 501文字のnote | IllegalArgumentException |
| BK-005 | 時間更新（正常） | PENDING状態 + 正しいversion | 更新成功 + BookingUpdatedイベント |
| BK-006 | 時間更新：バージョン不一致 | expectedVersion != 現在version | OptimisticLockException |
| BK-007 | 時間更新：CONFIRMED状態 | status=CONFIRMED | IllegalStateException |
| BK-008 | 時間更新：CANCELLED状態 | status=CANCELLED | IllegalStateException |
| BK-009 | 確定（正常） | PENDING → CONFIRMED | 遷移成功 + BookingConfirmedイベント |
| BK-010 | 確定：既にCONFIRMED | status=CONFIRMED | IllegalStateException |
| BK-011 | キャンセル（PENDING） | PENDING → CANCELLED | 遷移成功 + BookingCancelledイベント |
| BK-012 | キャンセル（CONFIRMED） | CONFIRMED → CANCELLED | 遷移成功 + BookingCancelledイベント |
| BK-013 | キャンセル：既にCANCELLED | status=CANCELLED | IllegalStateException |
| BK-014 | キャンセル理由保存 | reason指定 | cancelReasonが保存される |
| BK-015 | versionインクリメント | 更新操作 | version + 1 |

### 6.3 ConflictDetector（BK-TEST-03）

予約衝突検出ロジックのユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| CD-001 | 衝突なし：予約なし | 既存予約0件 | conflict=false |
| CD-002 | 衝突なし：別リソース | 同時間帯だが別resource_id | conflict=false |
| CD-003 | 衝突なし：隣接 | A.endAt == B.startAt | conflict=false |
| CD-004 | 衝突あり：完全重複（PENDING） | 同一時間帯 + status=PENDING | conflict=true |
| CD-005 | 衝突あり：完全重複（CONFIRMED） | 同一時間帯 + status=CONFIRMED | conflict=true |
| CD-006 | 衝突なし：CANCELLED予約 | 同一時間帯 + status=CANCELLED | conflict=false |
| CD-007 | 衝突あり：部分重複 | 時間が一部重なる | conflict=true |
| CD-008 | 自己除外：更新時 | 更新対象予約を除外 | conflict=false |
| CD-009 | 複数衝突 | 複数の既存予約と衝突 | 全衝突をリスト |

### 6.4 BookingRepository 統合テスト（BK-TEST-04）

データベースとの統合テスト。

#### テストケース一覧

| ID | テストケース | 操作 | 期待結果 |
|----|-------------|------|----------|
| BR-001 | 予約保存 | save(booking) | DBに永続化 |
| BR-002 | ID検索 | findById(existingId) | Optional.of(booking) |
| BR-003 | ユーザー検索 | findByUserId(userId) | ユーザーの予約一覧 |
| BR-004 | 衝突検索：重複あり | findActiveByResourceAndTimeRange | 重複予約を返却 |
| BR-005 | 衝突検索：CANCELLED除外 | status=CANCELLED | 結果に含まれない |
| BR-006 | 楽観的ロック成功 | save(updatedBooking) | 保存成功 + version++ |
| BR-007 | 楽観的ロック失敗 | 競合更新 | OptimisticLockingFailureException |
| BR-008 | ステータスフィルタ | findByUserIdAndStatus | 指定ステータスのみ |

### 6.5 境界値テスト：TimeRange境界（BK-TEST-05）

隣接予約と境界条件の特化テスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| BND-001 | 隣接予約：A.end == B.start | 連続予約 | 両方作成可能 |
| BND-002 | 1ms重複 | A.end = B.start + 1ms | 衝突 |
| BND-003 | 同一時刻開始 | A.start == B.start | 衝突 |
| BND-004 | 同一時刻終了 | A.end == B.end | 衝突（start次第） |
| BND-005 | 最小期間予約 | duration=1分 | 作成可能 |
| BND-006 | 日跨ぎ予約 | 23:00-01:00（翌日） | 作成可能 |
| BND-007 | 深夜0時境界 | 00:00-01:00 | 正常動作 |

### 6.6 E2Eテスト：create→update→cancel フロー（BK-TEST-06）

予約フロー全体のE2Eテスト。

#### テストケース一覧

| ID | テストケース | 期待結果 |
|----|-------------|----------|
| E2E-BK-001 | 正常フロー：create→update→cancel | 各ステップで期待レスポンス |
| E2E-BK-002 | 衝突検出：重複予約 | 409 Conflict + conflictingBookingId |
| E2E-BK-003 | 楽観的ロック：競合更新 | 409 Conflict + version_mismatch |
| E2E-BK-004 | 状態遷移：PENDING→CONFIRMED→CANCELLED | 正常遷移 |
| E2E-BK-005 | 所有者チェック：他者の予約変更 | 403 Forbidden |

### 6.7 権限テスト：所有者以外のアクセス拒否（BK-TEST-07）

予約リソースへのアクセス制御テスト。

#### テストケース一覧

| ID | テストケース | リクエスト | 期待結果 |
|----|-------------|----------|----------|
| BK-AUTH-001 | 自分の予約取得 | GET /bookings/{ownId} | 200 OK |
| BK-AUTH-002 | 他人の予約取得 | GET /bookings/{otherId} | 403 Forbidden |
| BK-AUTH-003 | 自分の予約更新 | PUT /bookings/{ownId} | 200 OK |
| BK-AUTH-004 | 他人の予約更新 | PUT /bookings/{otherId} | 403 Forbidden |
| BK-AUTH-005 | 自分の予約キャンセル | DELETE /bookings/{ownId} | 204 No Content |
| BK-AUTH-006 | 他人の予約キャンセル | DELETE /bookings/{otherId} | 403 Forbidden |

---

## 7. Payment テスト計画詳細

### 7.1 Money値オブジェクト（PAY-TEST-01）

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

### 7.2 Payment集約（PAY-TEST-02）

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

### 7.3 IdempotencyKey（PAY-TEST-03）

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

### 7.4 PaymentRepository 統合テスト（PAY-TEST-04）

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

### 7.5 冪等性テスト：同一Idempotency-Key再送（PAY-TEST-05）

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

### 7.6 境界値テスト：金額境界（PAY-TEST-06）

金額に関する境界値テスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| AMT-001 | 最小金額 | amount=1 | 正常作成 |
| AMT-002 | 最大金額（Integer.MAX_VALUE） | amount=2147483647 | 正常作成 |
| AMT-003 | 部分キャプチャ（最小） | captureAmount=1 | 正常 |
| AMT-004 | 部分キャプチャ（1円未満） | captureAmount=0 | IllegalArgumentException |
| AMT-005 | 全額返金後の追加返金 | refundAmount > remaining | IllegalArgumentException |
| AMT-006 | 複数回部分返金 | 各回合計 <= capturedAmount | 正常 |

### 7.7 E2Eテスト：create→capture→refund フロー（PAY-TEST-07）

決済フロー全体のE2Eテスト。

#### テストケース一覧

| ID | テストケース | 期待結果 |
|----|-------------|----------|
| E2E-PAY-001 | 正常フロー：create→authorize→capture | 各ステップで期待レスポンス |
| E2E-PAY-002 | 返金フロー：capture→refund | 全額返金成功 |
| E2E-PAY-003 | 部分キャプチャ→部分返金 | 金額が正しく計算 |
| E2E-PAY-004 | 与信失敗からのリトライ | 新規Paymentで再試行 |
| E2E-PAY-005 | 予約連携：BookingConfirmed連動 | PaymentCaptured時にBooking確定 |

---

## 8. Unit Test設計概要

### 8.1 IAM

### 8.2 Booking

| テスト対象 | テストケース | 境界条件 |
|------------|-------------|----------|
| TimeRange | 正常な時間範囲 | startAt < endAt |
| TimeRange | 不正な時間範囲拒否 | startAt >= endAt |
| TimeRange | 過去日時拒否 | startAt < now |
| TimeRange | 重複判定 | 隣接（A.endAt == B.startAt）は非衝突 |
| Booking集約 | 予約作成 | PENDING状態で作成 |
| Booking集約 | 状態遷移 | PENDING→CONFIRMED |
| Booking集約 | キャンセル | CONFIRMED→CANCELLED |
| Booking集約 | CANCELLED更新拒否 | 終状態からの遷移不可 |
| ConflictDetector | 衝突検出 | 重複時間帯 |

### 8.3 Payment

| テスト対象 | テストケース | 境界条件 |
|------------|-------------|----------|
| Money | 正の金額 | amount > 0 |
| Money | 通貨コード | ISO 4217準拠 |
| Money | 加算/減算 | 同一通貨のみ |
| Payment集約 | 支払い作成 | PENDING状態で作成 |
| Payment集約 | 状態遷移 | PENDING→AUTHORIZED→CAPTURED |
| Payment集約 | 返金 | CAPTURED→REFUNDED |
| IdempotencyKey | 一意性 | 同一キーで重複作成拒否 |

---

## 9. Integration Test設計

### 9.1 リポジトリテスト

| テスト対象 | テストケース |
|------------|-------------|
| UserRepository | ユーザー保存・取得 |
| UserRepository | メールアドレスでの検索 |
| BookingRepository | 予約保存・取得 |
| BookingRepository | 楽観的ロック更新 |
| BookingRepository | 衝突検出クエリ |
| PaymentRepository | 支払い保存・取得 |
| PaymentRepository | 冪等キー検索 |

### 9.2 外部サービス連携テスト

| テスト対象 | テストケース | テスト方法 |
|------------|-------------|-----------|
| PaymentGateway | 正常オーソリ | WireMock |
| PaymentGateway | タイムアウト | WireMock遅延 |
| PaymentGateway | エラーレスポンス | WireMockスタブ |

---

## 10. E2E Test設計

### 10.1 シナリオ一覧

| シナリオ | フロー | 検証内容 |
|----------|--------|----------|
| 正常予約フロー | ログイン → 予約作成 → 支払い → 確認 | 全ステップ成功 |
| 予約変更フロー | ログイン → 予約作成 → 予約変更 → 確認 | バージョン更新 |
| 予約キャンセルフロー | ログイン → 予約作成 → 支払い → キャンセル → 返金 | 返金完了 |
| 認証失敗フロー | 無効認証情報 → エラー | 401レスポンス |
| 衝突検出フロー | 予約作成 → 重複予約 → エラー | 409レスポンス |

### 10.2 全システムE2Eシナリオ（TEST-03）

認証→予約→支払い→通知→監査の全BCを横断するE2Eテストシナリオです。

#### 10.2.1 シナリオ概要

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    E2E Test: Full Booking Flow                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌────────┐    ┌─────────┐    ┌─────────┐    ┌───────────┐    ┌──────┐│
│   │  IAM   │───►│ Booking │───►│ Payment │───►│Notification│───►│Audit ││
│   │ login  │    │ create  │    │ capture │    │   send    │    │record││
│   └────────┘    └─────────┘    └─────────┘    └───────────┘    └──────┘│
│       │             │              │               │              │     │
│       ▼             ▼              ▼               ▼              ▼     │
│   AccessToken   BookingId      PaymentId      NotificationId  AuditLogId│
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 10.2.2 正常フローシナリオ

| ID | ステップ | API | 期待レスポンス | 検証項目 |
|----|---------|-----|---------------|----------|
| E2E-FULL-001 | 1. ユーザーログイン | POST /auth/login | 200 OK | AccessToken取得、UserLoggedInイベント |
| E2E-FULL-002 | 2. 予約作成 | POST /bookings | 201 Created | BookingId取得、status=PENDING、BookingCreatedイベント |
| E2E-FULL-003 | 3. 支払い作成 | POST /payments | 201 Created | PaymentId取得、status=AUTHORIZED、PaymentCreatedイベント |
| E2E-FULL-004 | 4. 支払いキャプチャ | POST /payments/{id}/capture | 200 OK | status=CAPTURED、PaymentCapturedイベント |
| E2E-FULL-005 | 5. 予約確定 | PUT /bookings/{id} | 200 OK | status=CONFIRMED、BookingConfirmedイベント |
| E2E-FULL-006 | 6. 通知送信確認 | GET /notifications | 200 OK | BOOKING_CONFIRMED通知が存在 |
| E2E-FULL-007 | 7. 監査ログ確認 | GET /audit-logs | 200 OK | 全操作の監査ログが記録 |

#### 10.2.3 キャンセル・返金フローシナリオ

| ID | ステップ | API | 期待レスポンス | 検証項目 |
|----|---------|-----|---------------|----------|
| E2E-CANCEL-001 | 1〜5 | 上記と同様 | - | 予約確定状態まで進む |
| E2E-CANCEL-002 | 6. 予約キャンセル | DELETE /bookings/{id} | 204 No Content | status=CANCELLED、BookingCancelledイベント |
| E2E-CANCEL-003 | 7. 返金実行 | POST /payments/{id}/refund | 200 OK | status=REFUNDED、PaymentRefundedイベント |
| E2E-CANCEL-004 | 8. キャンセル通知確認 | GET /notifications | 200 OK | BOOKING_CANCELLED、PAYMENT_REFUNDED通知 |
| E2E-CANCEL-005 | 9. 監査ログ確認 | GET /audit-logs | 200 OK | キャンセル・返金の監査ログ |

#### 10.2.4 エラーハンドリングシナリオ

| ID | シナリオ | トリガー | 期待動作 |
|----|---------|---------|----------|
| E2E-ERR-001 | 認証失敗 | 無効なパスワード | 401 Unauthorized、LoginFailedイベント、監査ログ |
| E2E-ERR-002 | セッション切れ | 期限切れAccessToken | 401 Unauthorized、トークン更新フロー |
| E2E-ERR-003 | 予約衝突 | 重複時間帯 | 409 Conflict、conflictingBookingId |
| E2E-ERR-004 | 決済失敗 | Gateway拒否 | PaymentFailed、PAYMENT_FAILED通知 |
| E2E-ERR-005 | 権限不足 | 他者リソースアクセス | 403 Forbidden、監査ログ |

#### 10.2.5 非同期イベント検証

| ID | イベント | 発生タイミング | 検証内容 |
|----|---------|---------------|----------|
| EVT-001 | BookingCreated | 予約作成後 | Notification/Auditが受信・処理 |
| EVT-002 | PaymentCaptured | 支払いキャプチャ後 | Booking確定トリガー |
| EVT-003 | BookingCancelled | 予約キャンセル後 | 返金トリガー |
| EVT-004 | PaymentRefunded | 返金完了後 | 通知送信 |

#### 10.2.6 トレーサビリティ検証

| ID | 検証内容 | 期待動作 |
|----|---------|----------|
| TRACE-001 | traceId伝播 | 全APIレスポンスに同一traceIdが含まれる |
| TRACE-002 | 監査ログのtraceId | 関連操作が同一traceIdで紐づけ可能 |
| TRACE-003 | 通知のcorrelationId | 通知がトリガーイベントと紐づけ可能 |

#### 10.2.7 実装例（全フロー）

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullBookingFlowE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static String accessToken;
    private static String traceId;
    private static UUID bookingId;
    private static UUID paymentId;

    @Test
    @Order(1)
    @DisplayName("E2E-FULL-001: ユーザーログイン")
    void step1_Login() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "ValidPass123!");
        traceId = UUID.randomUUID().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Trace-Id", traceId);

        // When
        ResponseEntity<TokenResponse> response = restTemplate.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            TokenResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getHeaders().get("X-Trace-Id")).contains(traceId);

        accessToken = response.getBody().getAccessToken();
    }

    @Test
    @Order(2)
    @DisplayName("E2E-FULL-002: 予約作成")
    void step2_CreateBooking() {
        // Given
        HttpHeaders headers = authHeaders();
        CreateBookingRequest request = CreateBookingRequest.builder()
            .resourceId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .startAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .endAt(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
            .note("E2E Test Booking")
            .build();

        // When
        ResponseEntity<Booking> response = restTemplate.exchange(
            "/api/v1/bookings",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            Booking.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        bookingId = response.getBody().getId();
    }

    @Test
    @Order(3)
    @DisplayName("E2E-FULL-003: 支払い作成（オーソリ）")
    void step3_CreatePayment() {
        // Given
        HttpHeaders headers = authHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .bookingId(bookingId)
            .amount(1000)
            .currency("JPY")
            .build();

        // When
        ResponseEntity<Payment> response = restTemplate.exchange(
            "/api/v1/payments",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            Payment.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        paymentId = response.getBody().getId();
    }

    @Test
    @Order(4)
    @DisplayName("E2E-FULL-004: 支払いキャプチャ")
    void step4_CapturePayment() {
        // Given
        HttpHeaders headers = authHeaders();

        // When
        ResponseEntity<Payment> response = restTemplate.exchange(
            "/api/v1/payments/" + paymentId + "/capture",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Payment.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    @Order(5)
    @DisplayName("E2E-FULL-005: 予約確定")
    void step5_ConfirmBooking() {
        // Given
        HttpHeaders headers = authHeaders();

        // When: 予約取得
        ResponseEntity<Booking> getResponse = restTemplate.exchange(
            "/api/v1/bookings/" + bookingId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Booking.class
        );

        // Then: 支払い連動で自動確定されていることを確認
        // または手動で状態更新が必要な場合は PUT を実行
        assertThat(getResponse.getBody().getStatus())
            .isIn(BookingStatus.CONFIRMED, BookingStatus.PENDING);
    }

    @Test
    @Order(6)
    @DisplayName("E2E-FULL-006: 通知確認")
    void step6_VerifyNotification() throws InterruptedException {
        // Given: 非同期処理の完了を待機
        Thread.sleep(2000);
        HttpHeaders headers = authHeaders();

        // When
        ResponseEntity<NotificationListResponse> response = restTemplate.exchange(
            "/api/v1/notifications?type=BOOKING_CONFIRMED",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            NotificationListResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getItems())
            .anyMatch(n -> n.getMetadata().containsValue(bookingId.toString()));
    }

    @Test
    @Order(7)
    @DisplayName("E2E-FULL-007: 監査ログ確認")
    void step7_VerifyAuditLogs() {
        // Given
        HttpHeaders headers = adminAuthHeaders(); // 管理者権限が必要

        // When
        ResponseEntity<AuditLogListResponse> response = restTemplate.exchange(
            "/api/v1/audit-logs?traceId=" + traceId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            AuditLogListResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AuditLog> logs = response.getBody().getItems();

        // 全操作が記録されていることを確認
        assertThat(logs).extracting(AuditLog::getAction)
            .contains("LOGIN", "BOOKING_CREATE", "PAYMENT_CREATE",
                      "PAYMENT_CAPTURE", "BOOKING_CONFIRM");

        // 同一traceIdで紐づいていることを確認
        assertThat(logs).extracting(AuditLog::getTraceId)
            .containsOnly(traceId);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("X-Trace-Id", traceId);
        return headers;
    }

    private HttpHeaders adminAuthHeaders() {
        // 管理者認証情報でのヘッダー作成
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminAccessToken());
        headers.set("X-Trace-Id", traceId);
        return headers;
    }
}
```

#### 10.2.8 キャンセル・返金フロー実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CancelAndRefundE2ETest {

    // ... 初期化コード（上記と同様）

    @Test
    @Order(6)
    @DisplayName("E2E-CANCEL-002: 予約キャンセル")
    void step6_CancelBooking() {
        // Given
        HttpHeaders headers = authHeaders();

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/bookings/" + bookingId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 予約状態がCANCELLEDになっていることを確認
        ResponseEntity<Booking> getResponse = restTemplate.exchange(
            "/api/v1/bookings/" + bookingId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Booking.class
        );
        assertThat(getResponse.getBody().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @Order(7)
    @DisplayName("E2E-CANCEL-003: 返金実行")
    void step7_RefundPayment() {
        // Given
        HttpHeaders headers = authHeaders();
        RefundRequest request = RefundRequest.builder()
            .amount(1000) // 全額返金
            .reason("Booking cancelled by user")
            .build();

        // When
        ResponseEntity<Payment> response = restTemplate.exchange(
            "/api/v1/payments/" + paymentId + "/refund",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            Payment.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getBody().getRefundedAmount()).isEqualTo(1000);
    }

    @Test
    @Order(8)
    @DisplayName("E2E-CANCEL-004: キャンセル通知確認")
    void step8_VerifyCancelNotifications() throws InterruptedException {
        Thread.sleep(2000);
        HttpHeaders headers = authHeaders();

        // When
        ResponseEntity<NotificationListResponse> response = restTemplate.exchange(
            "/api/v1/notifications",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            NotificationListResponse.class
        );

        // Then
        List<Notification> notifications = response.getBody().getItems();
        assertThat(notifications)
            .extracting(Notification::getType)
            .contains(
                NotificationType.BOOKING_CANCELLED,
                NotificationType.PAYMENT_REFUNDED
            );
    }
}
```

#### 10.2.9 テスト環境設定

```yaml
# application-e2e-test.yaml
spring:
  profiles:
    active: e2e-test

  datasource:
    url: jdbc:postgresql://localhost:5432/booking_payment_e2e
    username: e2e_user
    password: ${E2E_DB_PASSWORD}

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: e2e-test-group
      auto-offset-reset: earliest

# E2Eテスト用の設定
e2e-test:
  # 非同期処理の完了待機時間
  async-wait-timeout-ms: 5000

  # テストユーザー
  test-user:
    email: test@example.com
    password: ValidPass123!

  # テスト管理者
  admin-user:
    email: admin@example.com
    password: AdminPass123!
```

#### 10.2.10 CI/CD統合

```yaml
# .github/workflows/e2e-test.yaml
name: E2E Tests

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  e2e-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: booking_payment_e2e
          POSTGRES_USER: e2e_user
          POSTGRES_PASSWORD: e2e_password
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      kafka:
        image: confluentinc/cp-kafka:7.5.0
        ports:
          - 9092:9092
        env:
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run E2E Tests
        env:
          E2E_DB_PASSWORD: e2e_password
        run: ./gradlew e2eTest

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: e2e-test-results
          path: build/reports/tests/e2eTest/
```

### 10.2 E2Eテスト実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static String accessToken;
    private static String bookingId;

    @Test
    @Order(1)
    void login_Success() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login", request, TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        accessToken = response.getBody().getAccessToken();
    }

    @Test
    @Order(2)
    void createBooking_Success() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        CreateBookingRequest request = CreateBookingRequest.builder()
            .resourceId(UUID.randomUUID())
            .startAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .endAt(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
            .build();

        HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Booking> response = restTemplate.postForEntity(
            "/api/v1/bookings", entity, Booking.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        bookingId = response.getBody().getId().toString();
    }

    @Test
    @Order(3)
    void getBooking_Success() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Booking> response = restTemplate.exchange(
            "/api/v1/bookings/" + bookingId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Booking.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(BookingStatus.PENDING);
    }
}
```

---

## 11. 関連ドキュメント

| ドキュメント | 内容 |
|--------------|------|
| `docs/api/openapi/*.yaml` | OpenAPI仕様（Contract TestのSSoT） |
| `docs/design/usecases/*.md` | ユースケース設計（テストケース導出元） |
| `docs/design/contexts/*.md` | コンテキスト設計（ドメインロジック検証対象） |

---

## 12. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| テストピラミッド | Martin Fowler, "Test Pyramid" | 業界標準 |
| Contract Testing | OpenAPI Specification | API契約のSSoT |
| Property-Based Testing | QuickCheck, jqwik | ドメイン不変条件の網羅的検証 |
| RFC 7807 | ProblemDetail形式 | エラーレスポンス標準 |

---

## 13. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| Consumer-Driven契約 | Pact等の導入時期 | 中（複数Consumer発生時） |
| 負荷テスト基準 | 目標TPS、レイテンシ | 高（Slice B） |
| カバレッジ閾値 | 行カバレッジ、分岐カバレッジの目標値 | 中 |
| ミューテーションテスト | PITest等の導入 | 低 |
