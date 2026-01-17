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

## 2. IAM テスト計画

### 2.1 PasswordValidator（IAM-TEST-01）

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
        // 一般的なパスワードリストを含む設定
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

    @Test
    @DisplayName("PV-010: 一般的なパスワードを拒否する")
    void commonPassword_IsRejected() {
        ValidationResult result = validator.validate("Password1!");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains(ValidationError.COMMON_PASSWORD);
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

### 2.2 TokenGenerator（IAM-TEST-02）

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
        assertThat(decoded.getIssuer()).isEqualTo("booking-payment");
        assertThat(decoded.getClaim("roles").asList(String.class))
            .containsExactly("USER");
    }

    @Test
    @DisplayName("TG-004: 期限切れトークンはTokenExpiredExceptionをスローする")
    void expiredToken_ThrowsTokenExpiredException() {
        UserId userId = UserId.of("550e8400-e29b-41d4-a716-446655440000");
        String token = tokenGenerator.generateAccessToken(userId, List.of("USER"));

        // 時間を16分進める
        Clock advancedClock = Clock.fixed(
            fixedClock.instant().plus(Duration.ofMinutes(16)),
            ZoneOffset.UTC
        );
        TokenValidator validator = new TokenValidator(keyPair.getPublic(), advancedClock);

        assertThatThrownBy(() -> validator.validate(token))
            .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    @DisplayName("TG-005: 不正署名のトークンはInvalidSignatureExceptionをスローする")
    void tamperedToken_ThrowsInvalidSignatureException() {
        UserId userId = UserId.of("550e8400-e29b-41d4-a716-446655440000");
        String token = tokenGenerator.generateAccessToken(userId, List.of("USER"));

        // ペイロードを改ざん
        String[] parts = token.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().encodeToString(
            "{\"sub\":\"hacked\"}".getBytes()
        );
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        TokenValidator validator = new TokenValidator(keyPair.getPublic(), fixedClock);

        assertThatThrownBy(() -> validator.validate(tamperedToken))
            .isInstanceOf(InvalidSignatureException.class);
    }
}
```

### 2.3 User集約（IAM-TEST-03）

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

#### 実装例

```java
@Nested
@DisplayName("User Aggregate Unit Tests (IAM-TEST-03)")
class UserAggregateTest {

    private User user;
    private Clock fixedClock;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-01-18T10:00:00Z"), ZoneOffset.UTC);
        passwordEncoder = new BCryptPasswordEncoder(12);

        user = User.create(
            UserId.generate(),
            Email.of("test@example.com"),
            HashedPassword.of(passwordEncoder.encode("Password123!")),
            fixedClock
        );
    }

    @Test
    @DisplayName("USR-001: 正しいパスワードで認証成功")
    void authenticate_WithCorrectPassword_Succeeds() {
        AuthenticationResult result = user.authenticate("Password123!", passwordEncoder, fixedClock);

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(UserLoggedIn.class);
    }

    @Test
    @DisplayName("USR-002: 誤ったパスワードで認証失敗、失敗カウント増加")
    void authenticate_WithWrongPassword_FailsAndIncrementsCounter() {
        AuthenticationResult result = user.authenticate("WrongPassword!", passwordEncoder, fixedClock);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AuthFailureReason.INVALID_CREDENTIALS);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(LoginFailed.class);
    }

    @Test
    @DisplayName("USR-003: ロック中のアカウントは認証をブロックする")
    void authenticate_WhenLocked_ThrowsLoginBlockedException() {
        user.lock(Duration.ofMinutes(30), LockReason.CONSECUTIVE_FAILURES, fixedClock);
        user.clearEvents();

        assertThatThrownBy(() ->
            user.authenticate("Password123!", passwordEncoder, fixedClock)
        ).isInstanceOf(LoginBlockedException.class)
         .hasMessageContaining("Account is locked");
    }

    @Test
    @DisplayName("USR-004: 5回連続失敗でアカウントが自動ロックされる")
    void authenticate_AfterFiveFailures_LocksAccount() {
        int lockThreshold = 5;

        for (int i = 0; i < lockThreshold; i++) {
            try {
                user.authenticate("WrongPassword!", passwordEncoder, fixedClock);
            } catch (LoginBlockedException e) {
                // 5回目でロック
            }
        }

        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.getLockedUntil())
            .isEqualTo(fixedClock.instant().plus(Duration.ofMinutes(30)));
        assertThat(user.getDomainEvents())
            .filteredOn(e -> e instanceof AccountLocked)
            .hasSize(1);
    }

    @Test
    @DisplayName("USR-006: ロック期限経過後は認証試行可能")
    void authenticate_AfterLockExpires_AllowsAttempt() {
        user.lock(Duration.ofMinutes(30), LockReason.CONSECUTIVE_FAILURES, fixedClock);

        // 31分後の時計
        Clock afterLockClock = Clock.fixed(
            fixedClock.instant().plus(Duration.ofMinutes(31)),
            ZoneOffset.UTC
        );

        AuthenticationResult result = user.authenticate(
            "Password123!", passwordEncoder, afterLockClock
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
```

### 2.4 UserRepository 統合テスト（IAM-TEST-04）

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

#### 実装例

```java
@SpringBootTest
@Transactional
@DisplayName("UserRepository Integration Tests (IAM-TEST-04)")
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create(
            UserId.generate(),
            Email.of("test@example.com"),
            HashedPassword.of("$2a$12$hashedpassword"),
            Clock.systemUTC()
        );
    }

    @Test
    @DisplayName("UR-001: ユーザーをDBに保存できる")
    void save_PersistsUserToDatabase() {
        User saved = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("UR-002: Emailでユーザーを検索できる")
    void findByEmail_WithExistingEmail_ReturnsUser() {
        userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail(Email.of("test@example.com"));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("UR-003: 存在しないEmailはOptional.empty()を返す")
    void findByEmail_WithNonExistentEmail_ReturnsEmpty() {
        Optional<User> found = userRepository.findByEmail(Email.of("nonexistent@example.com"));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("UR-006: 楽観的ロック競合でOptimisticLockingFailureExceptionをスローする")
    void save_WithVersionConflict_ThrowsException() {
        User saved = userRepository.save(testUser);
        entityManager.flush();

        // 別トランザクションで更新をシミュレート
        entityManager.getEntityManager()
            .createNativeQuery("UPDATE users SET version = version + 1 WHERE id = :id")
            .setParameter("id", saved.getId().value())
            .executeUpdate();

        saved.lock(Duration.ofMinutes(30), LockReason.ADMIN_ACTION, Clock.systemUTC());

        assertThatThrownBy(() -> {
            userRepository.save(saved);
            entityManager.flush();
        }).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("UR-007: 重複Emailは一意制約違反をスローする")
    void save_WithDuplicateEmail_ThrowsException() {
        userRepository.save(testUser);
        entityManager.flush();

        User duplicateUser = User.create(
            UserId.generate(),
            Email.of("test@example.com"), // 同じEmail
            HashedPassword.of("$2a$12$anotherhashedpassword"),
            Clock.systemUTC()
        );

        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

### 2.5 E2E テスト：login→refresh→logout フロー（IAM-TEST-05）

認証フロー全体のE2Eテスト。

#### テストシナリオ

```
シナリオ: 正常な認証フロー
  Given ユーザー "user@example.com" が存在する
  When POST /auth/login でログインする
  Then 200 OK + accessToken + refreshToken を受け取る

  When POST /auth/refresh でトークンをリフレッシュする
  Then 200 OK + 新しい accessToken を受け取る

  When POST /auth/logout でログアウトする
  Then 204 No Content

  When 古い refreshToken で POST /auth/refresh する
  Then 401 Unauthorized（失効済み）
```

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

#### 実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("IAM E2E Tests (IAM-TEST-05)")
class IamE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // テストユーザーを作成
        testUser = User.create(
            UserId.generate(),
            Email.of("e2e-test@example.com"),
            HashedPassword.of(passwordEncoder.encode("TestPassword123!")),
            Clock.systemUTC()
        );
        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(testUser);
    }

    @Test
    @DisplayName("E2E-IAM-001: 正常な認証フロー（login→refresh→logout）")
    void fullAuthenticationFlow_Succeeds() {
        // Step 1: ログイン
        LoginRequest loginRequest = new LoginRequest(
            "e2e-test@example.com",
            "TestPassword123!"
        );

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
            "/auth/login",
            loginRequest,
            TokenResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        String accessToken = loginResponse.getBody().accessToken();
        String refreshToken = loginResponse.getBody().refreshToken();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // Step 2: トークンリフレッシュ
        RefreshRequest refreshRequest = new RefreshRequest(refreshToken);

        ResponseEntity<TokenResponse> refreshResponse = restTemplate.postForEntity(
            "/auth/refresh",
            refreshRequest,
            TokenResponse.class
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        String newAccessToken = refreshResponse.getBody().accessToken();
        assertThat(newAccessToken).isNotBlank();
        assertThat(newAccessToken).isNotEqualTo(accessToken); // 新しいトークン

        // Step 3: ログアウト
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(newAccessToken);
        HttpEntity<Void> logoutRequest = new HttpEntity<>(headers);

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
            "/auth/logout",
            HttpMethod.POST,
            logoutRequest,
            Void.class
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Step 4: 古いRefreshTokenでリフレッシュ試行（失敗するべき）
        ResponseEntity<ErrorResponse> expiredRefreshResponse = restTemplate.postForEntity(
            "/auth/refresh",
            refreshRequest, // 古いrefreshToken
            ErrorResponse.class
        );

        assertThat(expiredRefreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(expiredRefreshResponse.getBody().error()).isEqualTo("token_revoked");
    }

    @Test
    @DisplayName("E2E-IAM-002: 無効な認証情報でログイン失敗")
    void login_WithInvalidCredentials_Returns401() {
        LoginRequest loginRequest = new LoginRequest(
            "e2e-test@example.com",
            "WrongPassword!"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/auth/login",
            loginRequest,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("invalid_credentials");
    }

    @Test
    @DisplayName("E2E-IAM-003: ロック中のアカウントでログイン失敗")
    void login_WithLockedAccount_Returns401() {
        // アカウントをロック
        testUser.lock(Duration.ofMinutes(30), LockReason.ADMIN_ACTION, Clock.systemUTC());
        userRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest(
            "e2e-test@example.com",
            "TestPassword123!"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/auth/login",
            loginRequest,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("account_locked");
    }
}
```

### 2.6 権限テスト：無効トークンでのアクセス拒否（IAM-TEST-06）

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

#### 実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Authorization Tests (IAM-TEST-06)")
class AuthorizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Test
    @DisplayName("AUTH-001: トークンなしでのアクセスは401を返す")
    void accessWithoutToken_Returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/bookings/550e8400-e29b-41d4-a716-446655440000",
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("missing_token");
    }

    @Test
    @DisplayName("AUTH-002: 不正形式トークンは401を返す")
    void accessWithMalformedToken_Returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-valid-jwt-token");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings/550e8400-e29b-41d4-a716-446655440000",
            HttpMethod.GET,
            request,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("invalid_token");
    }

    @Test
    @DisplayName("AUTH-003: 期限切れトークンは401を返す")
    void accessWithExpiredToken_Returns401() {
        // 過去の時点で生成されたトークンをシミュレート
        Clock pastClock = Clock.fixed(
            Instant.now().minus(Duration.ofHours(1)),
            ZoneOffset.UTC
        );
        String expiredToken = tokenGenerator.generateAccessToken(
            UserId.of("550e8400-e29b-41d4-a716-446655440000"),
            List.of("USER"),
            pastClock
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings/550e8400-e29b-41d4-a716-446655440000",
            HttpMethod.GET,
            request,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("token_expired");
    }

    @Test
    @DisplayName("AUTH-006: 権限不足は403を返す")
    void accessWithInsufficientRole_Returns403() {
        // USERロールでADMIN専用エンドポイントにアクセス
        String userToken = tokenGenerator.generateAccessToken(
            UserId.of("550e8400-e29b-41d4-a716-446655440000"),
            List.of("USER")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/admin/users",
            HttpMethod.GET,
            request,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("insufficient_permissions");
    }

    @Test
    @DisplayName("AUTH-007: 別ユーザーのリソースへのアクセスは403を返す")
    void accessOtherUserResource_Returns403() {
        // user-aのトークンでuser-bの予約にアクセス
        String userAToken = tokenGenerator.generateAccessToken(
            UserId.of("user-a-id"),
            List.of("USER")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // user-bが所有する予約にアクセス
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings/user-b-booking-id",
            HttpMethod.GET,
            request,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("access_denied");
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
public class IamTestFixtures {

    public static User createActiveUser() {
        return User.create(
            UserId.of("test-user-001"),
            Email.of("test-user@example.com"),
            HashedPassword.of("$2a$12$fixedHashForTest"),
            Clock.fixed(Instant.parse("2026-01-18T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    public static User createLockedUser() {
        User user = createActiveUser();
        user.lock(Duration.ofMinutes(30), LockReason.CONSECUTIVE_FAILURES, Clock.systemUTC());
        return user;
    }
}
```
