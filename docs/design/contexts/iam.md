---
doc_type: "context"
id: "iam"
bounded_context: "IAM"
version: "1.0"
last_updated: "2026-01-17"
status: "stable"
---

# 1. 目的

IAM（Identity and Access Management）コンテキストは、システム全体の**認証（Authentication）と認可（Authorization）**を担当する。

## 責務

- ユーザーの識別と認証情報の管理
- AccessToken / RefreshToken の発行・検証・失効
- Brute-force攻撃からの保護（レート制限、アカウントロック）
- 他コンテキスト（Booking, Payment等）への認証情報提供

## スコープ外

- ユーザー登録（Onboarding）：別途検討
- 権限管理（RBAC/ABAC）の詳細：Phase 2以降
- MFA（多要素認証）：Phase 2以降

---

# 2. 用語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **User**：システムに登録された利用者
  - **AccessToken**：短命のJWT（API認証用、推奨15分〜1時間）
  - **RefreshToken**：長命のトークン（AccessToken再発行用、推奨7日〜30日）
  - **Credential**：認証情報（email + password）

---

# 3. 集約一覧（Aggregate Catalog）

## 3.1 User（集約ルート）

```
User (Aggregate Root) {
  id: UserId (UUID)
  email: Email (unique, value object)
  passwordHash: HashedPassword (value object)
  status: UserStatus (ACTIVE | LOCKED | SUSPENDED)
  failedLoginAttempts: Integer (>= 0)
  lastFailedLoginAt: DateTime?
  lockedUntil: DateTime?
  createdAt: DateTime
  updatedAt: DateTime
}
```

### 不変条件
1. `email` は一意（同一メールで複数ユーザー不可）
2. `passwordHash` は平文を保持しない（bcrypt等でハッシュ化）
3. `failedLoginAttempts` は非負整数
4. `status = LOCKED` の場合、`lockedUntil` が未来ならログイン不可

### 振る舞い
- `authenticate(password)`: パスワード検証、成功/失敗に応じて状態更新
- `lock(duration)`: アカウントをロック
- `unlock()`: ロック解除（手動または時間経過）

## 3.2 RefreshToken（エンティティ）

```
RefreshToken {
  id: RefreshTokenId (UUID)
  userId: UserId (FK)
  tokenHash: HashedToken (unique)
  expiresAt: DateTime
  revokedAt: DateTime?
  createdAt: DateTime
}
```

### 不変条件
1. `tokenHash` は一意
2. `expiresAt` は作成時点より未来
3. `revokedAt` が設定されたトークンは使用不可

### 振る舞い
- `isValid()`: 有効期限内かつ未失効か判定
- `revoke()`: トークンを失効させる

---

# 4. Context Map

```
                    ┌─────────────────────────────────┐
                    │              IAM                │
                    │  (Identity & Access Management) │
                    │                                 │
                    │  ・User管理                      │
                    │  ・Token発行/検証                │
                    │  ・Brute-force対策              │
                    └───────────┬─────────────────────┘
                                │
                                │ Supplier（認証情報提供）
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
          ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│     Booking     │   │     Payment     │   │      Audit      │
│                 │   │                 │   │                 │
│ AccessToken検証  │   │ AccessToken検証  │   │ ログイン監査    │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 関係性

| 関係 | 種別 | 説明 |
|------|------|------|
| IAM → Booking | Customer-Supplier | IAMがAccessTokenを提供、Bookingが検証して使用 |
| IAM → Payment | Customer-Supplier | IAMがAccessTokenを提供、Paymentが検証して使用 |
| IAM → Audit | Publisher-Subscriber | IAMがログインイベントを発行、Auditが購読して記録 |
| IAM → Notification | Publisher-Subscriber | IAMがセキュリティイベントを発行、Notificationが通知 |

## 統合パターン

- **Booking/Payment との統合**：
  - 共有カーネル：JWTの署名検証ロジック
  - 各コンテキストは `Authorization: Bearer <token>` ヘッダーからトークンを取得
  - トークンの `sub` クレームからユーザーIDを抽出

- **Audit との統合**：
  - ドメインイベント（UserLoggedIn, LoginFailed）を発行
  - Auditコンテキストがイベントを購読して記録

---

# 5. 永続化

## 5.1 users テーブル

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | UUID | PK | ユーザーID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | メールアドレス |
| password_hash | VARCHAR(255) | NOT NULL | bcryptハッシュ |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE/LOCKED/SUSPENDED |
| failed_login_attempts | INTEGER | NOT NULL, DEFAULT 0 | 連続失敗回数 |
| last_failed_login_at | TIMESTAMP | NULL | 最終失敗日時 |
| locked_until | TIMESTAMP | NULL | ロック解除予定日時 |
| created_at | TIMESTAMP | NOT NULL | 作成日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |

**インデックス：**
- `idx_users_email` ON users(email) - ログイン時の検索

## 5.2 refresh_tokens テーブル

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | UUID | PK | トークンID |
| user_id | UUID | FK(users), NOT NULL | ユーザーID |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | トークンハッシュ |
| expires_at | TIMESTAMP | NOT NULL | 有効期限 |
| revoked_at | TIMESTAMP | NULL | 失効日時 |
| created_at | TIMESTAMP | NOT NULL | 作成日時 |

**インデックス：**
- `idx_refresh_tokens_token_hash` ON refresh_tokens(token_hash) - リフレッシュ時の検索
- `idx_refresh_tokens_user_valid` ON refresh_tokens(user_id, revoked_at) WHERE revoked_at IS NULL - ユーザーの有効トークン検索

---

# 6. ドメインイベント

## 6.1 UserLoggedIn

ユーザーがログインに成功したときに発行。

```
UserLoggedIn {
  eventId: UUID
  aggregateId: UserId
  occurredAt: DateTime
  payload: {
    userId: UUID
    clientIp: String (masked: 192.168.1.***)
    userAgent: String
  }
}
```

**購読者：** Audit（監査記録）、Notification（セキュリティ通知、任意）

## 6.2 LoginFailed

ログインに失敗したときに発行。

```
LoginFailed {
  eventId: UUID
  aggregateId: UserId? (存在する場合)
  occurredAt: DateTime
  payload: {
    email: String (masked: u***@example.com)
    reason: INVALID_CREDENTIALS | ACCOUNT_LOCKED | RATE_LIMITED
    clientIp: String (masked)
  }
}
```

**購読者：** Audit（監査記録）

## 6.3 AccountLocked

アカウントがロックされたときに発行。

```
AccountLocked {
  eventId: UUID
  aggregateId: UserId
  occurredAt: DateTime
  payload: {
    userId: UUID
    reason: CONSECUTIVE_FAILURES | ADMIN_ACTION
    lockedUntil: DateTime
  }
}
```

**購読者：** Audit（監査記録）、Notification（ユーザーへの通知）

## 6.4 RefreshTokenRevoked

RefreshTokenが失効されたときに発行。

```
RefreshTokenRevoked {
  eventId: UUID
  aggregateId: RefreshTokenId
  occurredAt: DateTime
  payload: {
    userId: UUID
    reason: LOGOUT | TOKEN_ROTATION | ADMIN_ACTION
  }
}
```

**購読者：** Audit（監査記録）

---

# 7. 非機能（SLO/Obs/Sec）

## 7.1 SLO（Service Level Objectives）

| SLI | 目標値 | 測定方法 |
|-----|--------|----------|
| 可用性 | 99.9% | 成功レスポンス / 総リクエスト |
| レイテンシ（p99） | < 500ms | ログイン処理時間の99パーセンタイル |
| エラー率 | < 0.1% | 5xx エラー / 総リクエスト |

## 7.2 Observability

- **詳細**：`docs/design/observability.md`
- **主要メトリクス**：
  - `iam_login_total{status, reason}` - ログイン試行数
  - `iam_login_duration_seconds{status}` - ログイン処理時間
  - `iam_account_locked_total` - アカウントロック発生数
  - `iam_active_refresh_tokens` - 有効なRefreshToken数

## 7.3 Security

- **詳細**：`docs/design/security.md`
- **主要対策**：
  - パスワード：bcrypt (cost=12)
  - Brute-force：IP制限 + アカウントロック
  - トークン：署名検証（RS256推奨）
  - PII：メール/IPのマスキング必須

---

# 8. ADRリンク

| ADR | タイトル | 状態 |
|-----|----------|------|
| ADR-001 | JWT認証方式の採用 | 作成予定 |
| ADR-002 | RefreshTokenローテーション戦略 | 作成予定 |
| ADR-003 | Brute-force対策の閾値設定 | 作成予定 |

---

# 9. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| JWT形式 | RFC 7519, OWASP推奨 | ステートレス認証の標準 |
| bcrypt cost=12 | OWASP Password Storage Cheat Sheet | 2026年時点の推奨値（推論） |
| RS256署名 | JWT Best Practices | 公開鍵検証で鍵管理を分離 |
| RefreshTokenローテーション | OAuth 2.0 Security BCP | トークン漏洩時の被害軽減 |

---

# 10. 未決事項

| 項目 | 内容 | 優先度 | 担当 |
|------|------|--------|------|
| パスワードポリシー | 複雑性要件の詳細（大文字/小文字/数字/記号） | 中 | 未定 |
| MFA対応 | TOTP/SMS等の多要素認証 | 低 | Phase 2 |
| セッション管理 | 同時ログイン数の制限 | 低 | Phase 2 |
| ユーザー登録フロー | Onboardingの設計 | 中 | 別タスク |
