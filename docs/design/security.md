---
doc_type: "design"
id: "security"
status: "draft"
---

# Security設計（SSOT）

本ドキュメントはシステム全体のセキュリティ設計を定義する。

## 関連ドキュメント

- PII：`docs/security/pii-policy.md`
- Secrets：`docs/security/secrets.md`
- SBOM/CVE：`docs/security/sbom-cve-ops.md`
- IAMコンテキスト：`docs/design/contexts/iam.md`

---

# 1. IAM セキュリティ設計

## 1.1 JWT構造（IAM-SEC-01）

### 概要

JWT（JSON Web Token）は RFC 7519 に準拠し、ステートレスな認証を実現する。

### トークン構造

```
┌─────────────────────────────────────────────────────────────────────┐
│                          JWT Token                                   │
├─────────────────────────────────────────────────────────────────────┤
│  Header (Base64URL)  .  Payload (Base64URL)  .  Signature           │
└─────────────────────────────────────────────────────────────────────┘
```

### Header

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-2026-01"
}
```

| フィールド | 説明 | 必須 |
|------------|------|------|
| `alg` | 署名アルゴリズム（RS256固定） | ✓ |
| `typ` | トークンタイプ（JWT固定） | ✓ |
| `kid` | キーID（鍵ローテーション対応） | ✓ |

### Payload（AccessToken）

```json
{
  "iss": "https://api.example.com",
  "sub": "user-uuid-here",
  "aud": "booking-payment-api",
  "iat": 1705555200,
  "exp": 1705556100,
  "jti": "unique-token-id",
  "type": "access",
  "roles": ["user"]
}
```

| クレーム | 説明 | 必須 | 例 |
|----------|------|------|-----|
| `iss` | 発行者（Issuer） | ✓ | `https://api.example.com` |
| `sub` | サブジェクト（ユーザーID） | ✓ | UUID |
| `aud` | 対象（Audience） | ✓ | `booking-payment-api` |
| `iat` | 発行時刻（Unix timestamp） | ✓ | - |
| `exp` | 有効期限（Unix timestamp） | ✓ | - |
| `jti` | トークン識別子（JWT ID） | ✓ | UUID |
| `type` | トークン種別 | ✓ | `access` / `refresh` |
| `roles` | ユーザーロール | ✓ | `["user"]` |

### Payload含めてはいけない情報（禁止項目）

| 禁止項目 | 理由 |
|----------|------|
| email | PII漏洩リスク |
| name | PII漏洩リスク |
| password関連 | セキュリティリスク |
| 電話番号 | PII漏洩リスク |
| 住所 | PII漏洩リスク |
| クレジットカード情報 | PCI DSS違反 |

### Signature

```
RSASSA-PKCS1-v1_5 with SHA-256 (RS256)
signature = RS256(base64UrlEncode(header) + "." + base64UrlEncode(payload), privateKey)
```

---

## 1.2 トークン有効期限（IAM-SEC-02）

### 有効期限設計

| トークン種別 | 有効期限 | 用途 | 根拠 |
|--------------|----------|------|------|
| AccessToken | 15分 | API認証 | 短命にして漏洩時のリスク軽減 |
| RefreshToken | 7日 | AccessToken再発行 | ユーザー利便性とセキュリティのバランス |
| RefreshToken（Remember Me） | 30日 | 長期セッション | オプトイン方式で明示的同意が必要 |

### 有効期限の考慮事項

```
┌──────────────────────────────────────────────────────────────────┐
│                     Token Lifetime                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  AccessToken (15min)                                              │
│  ├─ 短命：漏洩時の被害を最小化                                    │
│  ├─ ステートレス：サーバー側での無効化不可                        │
│  └─ 頻繁な更新：RefreshTokenで自動更新                            │
│                                                                   │
│  RefreshToken (7d / 30d)                                          │
│  ├─ 長命：ユーザー体験向上（再ログイン頻度低減）                  │
│  ├─ ステートフル：DB保存により即時無効化可能                      │
│  └─ ローテーション：使用ごとに新トークン発行                      │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### クロック許容範囲（Clock Skew）

- 許容範囲：**±30秒**
- 検証時に `iat` と `exp` に対してこの許容範囲を適用

### 実装例（Java）

```java
public class TokenConfiguration {
    // AccessToken: 15分
    public static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(15);

    // RefreshToken: 7日（通常）
    public static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);

    // RefreshToken: 30日（Remember Me）
    public static final Duration REFRESH_TOKEN_VALIDITY_REMEMBER_ME = Duration.ofDays(30);

    // Clock Skew許容範囲
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(30);
}
```

---

## 1.3 署名アルゴリズム（IAM-SEC-03）

### アルゴリズム選定

| アルゴリズム | 種別 | 採用 | 理由 |
|--------------|------|------|------|
| **RS256** | 非対称鍵 | ✓ 採用 | 公開鍵で検証可能、鍵管理が分離できる |
| RS384 | 非対称鍵 | △ 代替可 | より強いハッシュ、パフォーマンス低下 |
| RS512 | 非対称鍵 | △ 代替可 | 最も強いハッシュ、パフォーマンス低下 |
| HS256 | 対称鍵 | ✗ 不採用 | 秘密鍵を全サービスで共有する必要がある |
| ES256 | 楕円曲線 | △ 将来検討 | より短い鍵長で同等の強度 |

### RS256を選定した理由

1. **鍵管理の分離**
   - IAMサービスのみが秘密鍵を保持
   - 他サービス（Booking, Payment等）は公開鍵のみで検証可能
   - 秘密鍵漏洩リスクを最小化

2. **スケーラビリティ**
   - 公開鍵は自由に配布可能
   - JWKSエンドポイントで動的に取得可能

3. **業界標準**
   - OAuth 2.0 / OIDC の標準的な選択
   - 広範なライブラリサポート

### 鍵仕様

| 項目 | 値 | 備考 |
|------|-----|------|
| アルゴリズム | RSA | - |
| 鍵長 | 2048ビット以上（推奨：4096ビット） | NIST推奨 |
| ハッシュ | SHA-256 | - |
| パディング | PKCS#1 v1.5 | RS256標準 |

### 鍵ローテーション

```
┌────────────────────────────────────────────────────────────────┐
│                    Key Rotation Process                         │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 新しい鍵ペアを生成（kid: key-2026-02）                      │
│  2. JWKS に新旧両方の公開鍵を掲載（grace period: 24時間）       │
│  3. 新トークンは新しい鍵で署名開始                              │
│  4. 古いAccessTokenが全て期限切れになるのを待つ（15分+α）       │
│  5. 旧RefreshTokenは引き続き検証可能（最大30日）                │
│  6. grace period終了後、旧公開鍵をJWKSから削除                  │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### JWKSエンドポイント

```
GET /.well-known/jwks.json

Response:
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "key-2026-01",
      "use": "sig",
      "alg": "RS256",
      "n": "<modulus>",
      "e": "AQAB"
    }
  ]
}
```

---

## 1.4 RefreshTokenローテーション（IAM-SEC-04）

### ローテーション方針

**採用方式：Rotating Refresh Token（回転方式）**

RefreshTokenを使用するたびに新しいトークンを発行し、古いトークンを無効化する。

### シーケンス

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │                    │   IAM    │                    │    DB    │
└────┬─────┘                    └────┬─────┘                    └────┬─────┘
     │                               │                               │
     │  POST /auth/refresh           │                               │
     │  { refresh_token: "RT_old" }  │                               │
     ├──────────────────────────────>│                               │
     │                               │                               │
     │                               │  SELECT refresh_token         │
     │                               │  WHERE hash = hash("RT_old")  │
     │                               ├──────────────────────────────>│
     │                               │                               │
     │                               │  { valid, not revoked }       │
     │                               │<──────────────────────────────┤
     │                               │                               │
     │                               │  UPDATE: revoke RT_old        │
     │                               │  INSERT: RT_new               │
     │                               ├──────────────────────────────>│
     │                               │                               │
     │  { access_token: "...",      │                               │
     │    refresh_token: "RT_new" } │                               │
     │<──────────────────────────────┤                               │
     │                               │                               │
```

### 再利用検知（Replay Detection）

既に使用（revoke）されたRefreshTokenが再度使用された場合：

1. **検知**：`revoked_at IS NOT NULL` のトークンでリフレッシュ試行
2. **対処**：
   - 当該ユーザーの全RefreshTokenを即座に失効
   - `RefreshTokenReplayDetected` イベントを発行
   - Auditログに記録
   - 任意：ユーザーに通知（セキュリティアラート）

```java
public class RefreshTokenReplayHandler {
    public void handleReplay(UserId userId, RefreshTokenId replayedTokenId) {
        // 1. 全RefreshTokenを失効
        refreshTokenRepository.revokeAllByUserId(userId);

        // 2. イベント発行
        eventPublisher.publish(new RefreshTokenReplayDetected(
            userId,
            replayedTokenId,
            Instant.now()
        ));

        // 3. セキュリティアラート（オプション）
        notificationService.sendSecurityAlert(userId,
            SecurityAlertType.TOKEN_REPLAY_DETECTED);
    }
}
```

### ローテーションの設定

| 項目 | 値 | 説明 |
|------|-----|------|
| ローテーション有効 | Yes | 常にローテーション |
| 同時有効トークン数 | 最大5 | 複数デバイス対応 |
| 最古トークン自動失効 | Yes | 6個目の発行で最古を失効 |
| Replay検知 | Yes | 再利用は全トークン失効 |

---

## 1.5 Brute-force対策（IAM-SEC-05）

### 多層防御アプローチ

```
┌────────────────────────────────────────────────────────────────────┐
│                    Brute-force Protection Layers                   │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Layer 1: Rate Limiting（IP/エンドポイント単位）                    │
│  └─ WAF/API Gatewayで実施                                          │
│                                                                     │
│  Layer 2: Account-based Rate Limiting（アカウント単位）             │
│  └─ アプリケーションで実施                                          │
│                                                                     │
│  Layer 3: Account Lockout（アカウントロック）                       │
│  └─ 連続失敗でアカウントを一時ロック                                │
│                                                                     │
│  Layer 4: Progressive Delay（遅延増加）                             │
│  └─ 失敗回数に応じてレスポンス遅延                                  │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

### 閾値設定

#### Layer 1: IP Rate Limiting

| 項目 | 値 | 備考 |
|------|-----|------|
| `/auth/login` | 10 req/min/IP | WAF/API Gatewayで設定 |
| `/auth/refresh` | 30 req/min/IP | - |
| `/auth/logout` | 10 req/min/IP | - |
| 超過時レスポンス | 429 Too Many Requests | Retry-Afterヘッダー付与 |

#### Layer 2: Account Rate Limiting

| 項目 | 値 | 備考 |
|------|-----|------|
| 同一アカウントへの試行 | 5 req/min | - |
| ウィンドウ | Sliding Window（1分） | - |
| 超過時レスポンス | 429 Too Many Requests | - |

#### Layer 3: Account Lockout

| 項目 | 値 | 備考 |
|------|-----|------|
| 連続失敗閾値 | 5回 | パスワード不一致のみカウント |
| ロック期間 | 15分（自動解除） | 初回ロック |
| 再ロック時の期間 | 30分、60分、...（累進） | 最大24時間 |
| カウントリセット | 成功ログイン時 | - |
| ロック時レスポンス | 403 Forbidden + `error: account_locked` | - |

```java
public class AccountLockoutPolicy {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration INITIAL_LOCKOUT = Duration.ofMinutes(15);
    private static final Duration MAX_LOCKOUT = Duration.ofHours(24);

    public Duration calculateLockoutDuration(int lockoutCount) {
        // 累進的なロック期間: 15min, 30min, 60min, 120min, ...
        long minutes = INITIAL_LOCKOUT.toMinutes() * (long) Math.pow(2, lockoutCount - 1);
        return Duration.ofMinutes(Math.min(minutes, MAX_LOCKOUT.toMinutes()));
    }
}
```

#### Layer 4: Progressive Delay

| 失敗回数 | 追加遅延 | 備考 |
|----------|----------|------|
| 1-2回 | 0秒 | 通常レスポンス |
| 3回 | 1秒 | 軽微な遅延 |
| 4回 | 2秒 | - |
| 5回 | ロック | アカウントロックへ移行 |

### 監視とアラート

| メトリクス | 閾値 | アクション |
|------------|------|------------|
| `iam_login_failed_total` | >100/min | アラート発報 |
| `iam_account_locked_total` | >10/min | アラート発報 |
| 単一IPからの失敗 | >50/hour | IPブロック検討 |

### 実装例

```java
@Service
public class BruteForceProtectionService {
    private final UserRepository userRepository;
    private final RateLimiter accountRateLimiter;

    public void recordFailedAttempt(User user) {
        int attempts = user.incrementFailedLoginAttempts();

        if (attempts >= AccountLockoutPolicy.MAX_FAILED_ATTEMPTS) {
            Duration lockoutDuration = lockoutPolicy.calculateLockoutDuration(
                user.getLockoutCount() + 1
            );
            user.lock(lockoutDuration);
            eventPublisher.publish(new AccountLocked(
                user.getId(),
                LockReason.CONSECUTIVE_FAILURES,
                user.getLockedUntil()
            ));
        }

        userRepository.save(user);
    }

    public void recordSuccessfulLogin(User user) {
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }
}
```

---

# 2. 共通セキュリティポリシー

## 2.1 OWASP準拠

以下のOWASP Top 10項目に対応：

| カテゴリ | 対策 |
|----------|------|
| A01:2021 Broken Access Control | JWT検証、認可チェック |
| A02:2021 Cryptographic Failures | RS256署名、bcryptハッシュ |
| A03:2021 Injection | 入力検証、パラメータ化クエリ |
| A04:2021 Insecure Design | DDD、Hexagonal Architecture |
| A05:2021 Security Misconfiguration | セキュリティヘッダー、最小権限 |
| A06:2021 Vulnerable Components | SBOM、CVEスキャン |
| A07:2021 Authentication Failures | Brute-force対策、トークンローテーション |
| A08:2021 Data Integrity Failures | JWT署名検証 |

## 2.2 パスワードポリシー

| 項目 | 要件 |
|------|------|
| 最小文字数 | 12文字 |
| 必須文字種 | 大文字、小文字、数字、記号のうち3種以上 |
| ハッシュアルゴリズム | bcrypt |
| bcrypt cost | 12（推奨値、2026年時点） |
| ソルト | bcrypt自動生成 |
| 平文保存 | 禁止 |

```java
public class PasswordValidator {
    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public ValidationResult validate(String password) {
        if (password.length() < MIN_LENGTH) {
            return ValidationResult.failure("Password must be at least 12 characters");
        }

        int categoryCount = 0;
        if (UPPERCASE.matcher(password).find()) categoryCount++;
        if (LOWERCASE.matcher(password).find()) categoryCount++;
        if (DIGIT.matcher(password).find()) categoryCount++;
        if (SPECIAL.matcher(password).find()) categoryCount++;

        if (categoryCount < 3) {
            return ValidationResult.failure(
                "Password must contain at least 3 of: uppercase, lowercase, digit, special"
            );
        }

        return ValidationResult.success();
    }
}
```

## 2.3 セキュリティヘッダー

| ヘッダー | 値 | 目的 |
|----------|-----|------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS強制 |
| `X-Content-Type-Options` | `nosniff` | MIMEスニッフィング防止 |
| `X-Frame-Options` | `DENY` | クリックジャッキング防止 |
| `Content-Security-Policy` | `default-src 'self'` | XSS防止 |
| `X-XSS-Protection` | `0` | ブラウザXSSフィルター無効化（CSP優先） |
| `Cache-Control` | `no-store` | 認証レスポンスのキャッシュ禁止 |

---

# 3. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| RS256選定 | RFC 7518 (JWA), OAuth 2.0 BCP | 非対称鍵の標準選択 |
| AccessToken 15分 | OAuth 2.0 Security BCP | 短命トークンの推奨 |
| RefreshToken 7日 | 業界標準、UX考慮 | Remember Meで延長可 |
| bcrypt cost=12 | OWASP Password Storage Cheat Sheet | 2026年推奨値（推論） |
| 5回失敗でロック | NIST SP 800-63B | 推奨範囲内 |
| 15分ロック | ユーザビリティとセキュリティのバランス | - |

---

# 4. 未決事項

| 項目 | 内容 | 優先度 | 担当 |
|------|------|--------|------|
| ES256移行 | 楕円曲線への将来的な移行 | 低 | Phase 2 |
| 地理的異常検知 | 異常なログイン地域からのアクセス検知 | 中 | Phase 2 |
| デバイストラスト | 信頼済みデバイス管理 | 低 | Phase 2 |
