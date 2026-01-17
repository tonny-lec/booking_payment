---
doc_type: "test_plan"
id: "test-plan"
version: "1.0"
last_updated: "2026-01-17"
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

## 5. Unit Test設計

### 5.1 IAM

| テスト対象 | テストケース | 境界条件 |
|------------|-------------|----------|
| PasswordValidator | 正常パスワード検証 | 最小長、最大長、特殊文字 |
| PasswordValidator | 不正パスワード拒否 | 弱いパスワード |
| TokenGenerator | AccessToken生成 | 有効期限、クレーム |
| TokenGenerator | RefreshToken生成 | ローテーション |
| User集約 | ログイン成功 | 正常認証 |
| User集約 | ログイン失敗カウント | 連続失敗 |
| User集約 | アカウントロック | 閾値到達 |

### 5.2 Booking

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

### 5.3 Payment

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

## 6. Integration Test設計

### 6.1 リポジトリテスト

| テスト対象 | テストケース |
|------------|-------------|
| UserRepository | ユーザー保存・取得 |
| UserRepository | メールアドレスでの検索 |
| BookingRepository | 予約保存・取得 |
| BookingRepository | 楽観的ロック更新 |
| BookingRepository | 衝突検出クエリ |
| PaymentRepository | 支払い保存・取得 |
| PaymentRepository | 冪等キー検索 |

### 6.2 外部サービス連携テスト

| テスト対象 | テストケース | テスト方法 |
|------------|-------------|-----------|
| PaymentGateway | 正常オーソリ | WireMock |
| PaymentGateway | タイムアウト | WireMock遅延 |
| PaymentGateway | エラーレスポンス | WireMockスタブ |

---

## 7. E2E Test設計

### 7.1 シナリオ一覧

| シナリオ | フロー | 検証内容 |
|----------|--------|----------|
| 正常予約フロー | ログイン → 予約作成 → 支払い → 確認 | 全ステップ成功 |
| 予約変更フロー | ログイン → 予約作成 → 予約変更 → 確認 | バージョン更新 |
| 予約キャンセルフロー | ログイン → 予約作成 → 支払い → キャンセル → 返金 | 返金完了 |
| 認証失敗フロー | 無効認証情報 → エラー | 401レスポンス |
| 衝突検出フロー | 予約作成 → 重複予約 → エラー | 409レスポンス |

### 7.2 E2Eテスト実装例

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

## 8. 関連ドキュメント

| ドキュメント | 内容 |
|--------------|------|
| `docs/api/openapi/*.yaml` | OpenAPI仕様（Contract TestのSSoT） |
| `docs/design/usecases/*.md` | ユースケース設計（テストケース導出元） |
| `docs/design/contexts/*.md` | コンテキスト設計（ドメインロジック検証対象） |

---

## 9. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| テストピラミッド | Martin Fowler, "Test Pyramid" | 業界標準 |
| Contract Testing | OpenAPI Specification | API契約のSSoT |
| Property-Based Testing | QuickCheck, jqwik | ドメイン不変条件の網羅的検証 |
| RFC 7807 | ProblemDetail形式 | エラーレスポンス標準 |

---

## 10. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| Consumer-Driven契約 | Pact等の導入時期 | 中（複数Consumer発生時） |
| 負荷テスト基準 | 目標TPS、レイテンシ | 高（Slice B） |
| カバレッジ閾値 | 行カバレッジ、分岐カバレッジの目標値 | 中 |
| ミューテーションテスト | PITest等の導入 | 低 |
