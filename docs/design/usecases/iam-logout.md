---
doc_type: "usecase"
id: "iam-logout"
bounded_context: "IAM"
related_features: ["security", "contract-first", "observability"]
related_skills: ["authentication", "jwt"]
status: "draft"
---

# 1. 目的 / 背景

## 目的
ユーザーがログアウトできるようにし、RefreshTokenを失効させて以後の再発行を防ぐ。

## 背景
- AccessTokenは短命のJWTであり、サーバー側での失効管理は行わない
- RefreshTokenを失効させることで継続セッションを無効化できる
- 失効操作はリトライ前提のため、**Idempotent** とする

---

# 2. ユビキタス言語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **AccessToken**：短命のJWT（API認証用）
  - **RefreshToken**：長命のトークン（AccessToken再発行用）
  - **Logout**：RefreshTokenの失効操作

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

## Command: LogoutCommand
```
LogoutCommand {
  refreshToken: String (required)
}
```

## Response
- `204 No Content`

## Domain Event: RefreshTokenRevoked
```
RefreshTokenRevoked {
  eventId: UUID
  aggregateId: RefreshTokenId
  occurredAt: DateTime
}
```

---

# 5. ドメインモデル（集約/不変条件）

- 対象集約：`RefreshToken`
- 振る舞い：`revoke()` により `revokedAt` を設定し、以降使用不可

---

# 6. API（OpenAPI参照）

- SSOT：`docs/api/openapi/iam.yaml`
- エンドポイント：`POST /auth/logout`
- レスポンス：`204 No Content`

---

# 7. 永続化

- `refresh_tokens` テーブルの `revoked_at` を更新
- `refresh_tokens.token_hash` で検索（生トークンは保存しない）

---

# 8. 失敗モードとリカバリ（timeout/retry/idempotency）

## 失敗モード一覧

| 失敗モード | HTTPステータス | 原因 | リカバリ |
|------------|----------------|------|----------|
| UNAUTHORIZED | 401 | Bearer未認証 | 再ログイン |
| FORBIDDEN | 403 | BearerのsubとRefreshTokenのuser_id不一致 | 正しいユーザーで再試行 |
| INTERNAL_ERROR | 500 | サーバー内部エラー | 指数バックオフでリトライ |

## Idempotency

- **Idempotent**：RefreshTokenが存在しない/失効済みでも `204` を返す

### メリット
- リトライ安全（ネットワーク不安定時でも再送可能）
- トークン存在有無の露出を抑止（情報漏洩リスク低減）
- UX向上（ログアウト操作の成功率が高い）

### デメリット
- 監査上「失効対象が存在したか」が分かりにくい
- 誤ったトークン送信を検知しづらい

---

# 9. 観測性（logs/metrics/traces）

## ログ

| イベント | ログレベル | 必須フィールド | PIIポリシー |
|----------|------------|----------------|-------------|
| LogoutSucceeded | INFO | traceId, userId | refreshTokenはマスク |
| LogoutIgnored | INFO | traceId | refreshTokenはマスク |
| LogoutForbidden | WARN | traceId, userId | refreshTokenはマスク |

## メトリクス

| メトリクス名 | 型 | ラベル | 説明 |
|--------------|-----|--------|------|
| `iam_logout_total` | Counter | status=[success\|ignored\|forbidden] | ログアウト試行総数 |

## トレース

- Logout処理全体にtraceIdを付与

---

# 10. セキュリティ（authn/authz/audit/PII）

- **認証**：Bearer JWT 必須
- **認可**：Bearerの`sub`とRefreshTokenの`user_id`一致が必須
- **監査**：Logout成功/失敗を記録
- **PII**：RefreshTokenはマスクしてログ出力

---

# 11. テスト戦略（Unit/Integration/Contract/E2E）

## Unit Tests
- RefreshToken `revoke()` の挙動
- LogoutUseCase の Idempotent 動作

## Integration Tests
- RefreshTokenRepository での `revoked_at` 更新

## Contract Tests
- `POST /auth/logout` が `204` を返す

## E2E Tests
- login → logout → refresh の拒否確認

---

# 12. ADRリンク

- ADR-001: JWT認証方式の採用
- ADR-002: RefreshTokenローテーション戦略

---

# 13. Evidence（根拠：差分/ログ/計測/仕様）

| 項目 | 根拠 | 備考 |
|------|------|------|
| Logout API | `docs/api/openapi/iam.yaml` | 204 No Content |
| JWT構造 | `docs/design/security.md` | RS256採用 |
| RefreshToken | `docs/design/contexts/iam.md` | 失効モデル |

---

# 14. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| 監査ログの保持期間 | 法的要件に基づく設定 | 中 |
| ログアウトイベントの購読先 | Audit/Notificationの詳細 | 低 |
