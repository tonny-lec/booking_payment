---
doc_type: "usecase"
id: "iam-login"
bounded_context: "IAM"
related_features: ["contract-first", "observability", "security"]
related_skills: ["authentication", "jwt", "rate-limiting"]
status: "draft"
---

# 1. 目的 / 背景

## 目的
ユーザーがシステムにログインし、認証済みの状態でAPIを利用できるようにする。

## 背景
- 予約・決済システムでは、ユーザーの識別と認可が必要
- JWTベースの認証により、ステートレスなAPI認証を実現
- RefreshTokenローテーションにより、セキュリティと利便性を両立

## ユースケース概要
1. ユーザーがメールアドレスとパスワードでログイン
2. システムが認証情報を検証
3. 成功時：AccessToken + RefreshTokenを発行
4. 失敗時：エラーを返却（brute-force対策を含む）

---

# 2. ユビキタス言語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **User**：システムに登録された利用者
  - **AccessToken**：短命のJWT（API認証用）
  - **RefreshToken**：長命のトークン（AccessToken再発行用）
  - **Credential**：認証情報（email + password）

---

# 3. 依存関係（Context Map）

```
┌─────────────┐
│    IAM      │
│  (Identity) │
└──────┬──────┘
       │
       │ Downstream（認証情報提供）
       ▼
┌─────────────┐     ┌─────────────┐
│   Booking   │     │   Payment   │
│             │     │             │
└─────────────┘     └─────────────┘
```

- **IAM → Booking/Payment**：AccessTokenを発行し、他コンテキストが認証・認可に使用
- **関係タイプ**：Customer-Supplier（IAMがSupplier）

---

# 4. 入出力（Command/Query/Event）

## Command: LoginCommand
```
LoginCommand {
  email: String (required, format: email)
  password: String (required, min: 8)
  clientIp: String (internal, for rate limiting)
  userAgent: String (internal, for audit)
}
```

## Response: TokenResponse
```
TokenResponse {
  accessToken: String (JWT)
  refreshToken: String
  tokenType: "Bearer"
  expiresIn: Integer (seconds)
}
```

## Domain Event: UserLoggedIn
```
UserLoggedIn {
  eventId: UUID
  userId: UUID
  occurredAt: DateTime
  clientIp: String (masked for privacy)
  userAgent: String
}
```

## Domain Event: LoginFailed
```
LoginFailed {
  eventId: UUID
  email: String (masked: u***@example.com)
  reason: Enum (INVALID_CREDENTIALS | ACCOUNT_LOCKED | RATE_LIMITED)
  occurredAt: DateTime
  clientIp: String (masked)
}
```

---

# 5. ドメインモデル（集約/不変条件）

## 集約：User

```
User (Aggregate Root) {
  id: UserId (UUID)
  email: Email (unique)
  passwordHash: HashedPassword
  status: UserStatus (ACTIVE | LOCKED | SUSPENDED)
  failedLoginAttempts: Integer
  lastFailedLoginAt: DateTime?
  lockedUntil: DateTime?
  createdAt: DateTime
  updatedAt: DateTime
}
```

## 不変条件

1. **email は一意**：同一メールアドレスで複数ユーザーを作成不可
2. **passwordHash は平文を保持しない**：bcrypt等でハッシュ化
3. **failedLoginAttempts は非負**：0以上の整数
4. **LOCKED状態のユーザーはログイン不可**：lockedUntil が未来の場合はログイン拒否

## 状態遷移

```
          認証成功
ACTIVE ─────────────→ ACTIVE（failedAttempts=0にリセット）
   │
   │ 認証失敗（N回未満）
   ▼
ACTIVE（failedAttempts++）
   │
   │ 認証失敗（N回目）
   ▼
LOCKED（lockedUntil=now+lockDuration）
   │
   │ lockDuration経過
   ▼
ACTIVE（自動解除）
```

---

# 6. API（OpenAPI参照）

- SSOT：`docs/api/openapi/iam.yaml`
- エンドポイント：
  - `POST /auth/login` - ログイン
  - `POST /auth/refresh` - トークンリフレッシュ
  - `POST /auth/logout` - ログアウト

---

# 7. 永続化

## テーブル設計（推論、実装時に検証が必要）

### users テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' |
| failed_login_attempts | INTEGER | NOT NULL, DEFAULT 0 |
| last_failed_login_at | TIMESTAMP | NULL |
| locked_until | TIMESTAMP | NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### refresh_tokens テーブル
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| user_id | UUID | FK(users), NOT NULL |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL |
| expires_at | TIMESTAMP | NOT NULL |
| revoked_at | TIMESTAMP | NULL |
| created_at | TIMESTAMP | NOT NULL |

**インデックス：**
- `users(email)` - ログイン時の検索
- `refresh_tokens(token_hash)` - リフレッシュ時の検索
- `refresh_tokens(user_id, revoked_at)` - ユーザーの有効トークン検索

---

# 8. 失敗モードとリカバリ（timeout/retry/idempotency）

## 失敗モード一覧

| 失敗モード | HTTPステータス | 原因 | リカバリ |
|------------|----------------|------|----------|
| INVALID_CREDENTIALS | 401 | メールまたはパスワードが不正 | ユーザーに再入力を促す |
| ACCOUNT_LOCKED | 423 | 連続失敗によりアカウントロック | ロック解除まで待機、または管理者に連絡 |
| RATE_LIMITED | 429 | 短時間に過剰なリクエスト | Retry-Afterヘッダーの秒数後に再試行 |
| INTERNAL_ERROR | 500 | サーバー内部エラー | 指数バックオフでリトライ |

## Timeout設計

- **API全体のタイムアウト**：5秒
- **パスワード検証のタイムアウト**：2秒（bcrypt等は計算コストが高い）
- **DB接続タイムアウト**：1秒

## Retry戦略

- **クライアント側**：429の場合はRetry-Afterに従う
- **サーバー側**：DB接続失敗時は最大2回リトライ（指数バックオフ）

## Idempotency

- ログイン操作は本質的に冪等ではない（毎回新しいトークンを発行）
- ただし、ネットワーク障害による重複リクエストでもセキュリティ上の問題は発生しない

---

# 9. 観測性（logs/metrics/traces）

## ログ

| イベント | ログレベル | 必須フィールド | PIIポリシー |
|----------|------------|----------------|-------------|
| LoginAttempted | INFO | traceId, email(masked), clientIp(masked) | emailは先頭1文字+***@domain形式 |
| LoginSucceeded | INFO | traceId, userId, clientIp(masked) | userIdは出力可 |
| LoginFailed | WARN | traceId, email(masked), reason, clientIp(masked) | パスワードは絶対に出力しない |
| AccountLocked | WARN | traceId, userId, lockDuration | - |

## メトリクス

| メトリクス名 | 型 | ラベル | 説明 |
|--------------|-----|--------|------|
| `iam_login_total` | Counter | status=[success\|failure], reason | ログイン試行総数 |
| `iam_login_duration_seconds` | Histogram | status | ログイン処理時間 |
| `iam_account_locked_total` | Counter | - | アカウントロック発生数 |
| `iam_active_sessions` | Gauge | - | アクティブセッション数（RefreshToken数） |

## トレース

- **SpanName**：`IAM.login`
- **必須属性**：
  - `user.id`（成功時のみ）
  - `auth.result`：success | failure
  - `auth.failure_reason`（失敗時のみ）
- **子Span**：
  - `IAM.validateCredentials` - パスワード検証
  - `IAM.generateTokens` - トークン生成

---

# 10. セキュリティ（authn/authz/audit/PII）

## 認証（AuthN）

- パスワードは bcrypt (cost=12) でハッシュ化
- パスワード検証は timing-safe comparison を使用

## 認可（AuthZ）

- ログインエンドポイントは認証不要（公開API）
- リフレッシュ/ログアウトは有効なトークンが必要

## Brute-force対策

| 条件 | アクション |
|------|----------|
| 同一IPから60秒間に10回以上失敗 | 429 Rate Limited（60秒間ブロック） |
| 同一アカウントで5回連続失敗 | アカウントを15分間ロック |
| 同一アカウントで24時間以内に3回ロック | アカウントを24時間ロック |

## 監査

- すべてのログイン試行（成功/失敗）を監査ログに記録
- 監査ログには以下を含める：
  - タイムスタンプ
  - ユーザー識別子（成功時）またはメールアドレス（マスク済み）
  - クライアントIP（マスク済み）
  - User-Agent
  - 結果（success/failure）と理由

## PII保護

- **ログ出力禁止**：パスワード（平文/ハッシュ両方）
- **マスク必須**：メールアドレス（u***@example.com形式）
- **マスク推奨**：IPアドレス（最後のオクテットを***に）

---

# 11. テスト戦略（Unit/Integration/Contract/E2E）

## Unit Tests

| テスト対象 | テストケース |
|------------|-------------|
| PasswordValidator | 正常なパスワード検証、不正なパスワード拒否 |
| TokenGenerator | JWT生成、有効期限設定、クレーム設定 |
| User集約 | 失敗カウント増加、ロック判定、ロック解除 |

## Integration Tests

| テスト対象 | テストケース |
|------------|-------------|
| UserRepository | ユーザー検索、更新の永続化 |
| RefreshTokenRepository | トークン保存、失効処理 |
| LoginUseCase | 正常ログイン、認証失敗、アカウントロック |

## Contract Tests

- OpenAPI `iam.yaml` に対する契約テスト
- リクエスト/レスポンスの形式検証
- エラーレスポンスのProblemDetail形式検証

## E2E Tests

| シナリオ | 検証内容 |
|----------|----------|
| 正常ログインフロー | login → refresh → logout の一連の流れ |
| 認証失敗フロー | 不正パスワード → エラーレスポンス検証 |
| アカウントロックフロー | 連続失敗 → ロック → 時間経過後解除 |
| レート制限フロー | 過剰リクエスト → 429 → Retry-After検証 |

## 境界値テスト

- パスワード最小長（8文字）の境界
- 失敗回数のロック閾値（5回）の境界
- トークン有効期限の境界

---

# 12. ADRリンク

- ADR-001: JWT認証方式の採用（作成予定）
- ADR-002: RefreshTokenローテーション戦略（作成予定）
- ADR-003: Brute-force対策の閾値設定（作成予定）

---

# 13. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| JWT形式の採用 | RFC 7519, OWASP推奨 | ステートレス認証の標準 |
| bcrypt cost=12 | OWASP Password Storage Cheat Sheet | 2026年時点の推奨値（推論、実測で調整が必要） |
| Brute-force閾値 | OWASP Authentication Cheat Sheet | 業界標準を参考に設定（推論、運用データで調整が必要） |
| RefreshTokenローテーション | OAuth 2.0 Security Best Current Practice | トークン漏洩時の被害軽減 |

---

# 14. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| パスワードポリシー | 複雑性要件（大文字/小文字/数字/記号）の詳細 | 中 |
| MFA対応 | 多要素認証の追加（TOTP/SMS等） | 低（Slice A対象外） |
| セッション管理 | 同時ログイン数の制限 | 低 |
| 監査ログの保持期間 | 法的要件に基づく設定 | 中 |
