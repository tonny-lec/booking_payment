---
doc_type: "security_policy"
id: "secrets"
version: "1.0"
last_updated: "2026-01-17"
status: "draft"
---

# シークレット管理（SSOT）

本ドキュメントは、予約・決済基盤におけるシークレット（機密情報）の一覧、管理方針、ローテーション戦略を定義します。

---

## 1. シークレット一覧

### 1.1 シークレット分類

| 分類 | 説明 | 例 |
|------|------|-----|
| **認証系** | 認証・認可に関するキー | JWT署名鍵、セッションシークレット |
| **データベース** | DB接続に必要な認証情報 | ユーザー名、パスワード |
| **外部API** | 外部サービス連携用のキー | Payment Gateway APIキー |
| **暗号化** | データ暗号化に使用するキー | AES鍵、マスターキー |
| **インフラ** | インフラアクセス用の認証情報 | Cloud Provider認証情報 |

### 1.2 シークレット詳細一覧

#### 認証系シークレット

| ID | シークレット名 | 用途 | 形式 | ローテーション周期 |
|----|---------------|------|------|-------------------|
| SEC-AUTH-001 | `JWT_PRIVATE_KEY` | AccessToken署名用RSA秘密鍵 | PEM (RS256) | 90日 |
| SEC-AUTH-002 | `JWT_PUBLIC_KEY` | AccessToken検証用RSA公開鍵 | PEM (RS256) | 秘密鍵と同期 |
| SEC-AUTH-003 | `JWT_KEY_ID` | JWTヘッダーのkid（キーID） | UUID | 秘密鍵と同期 |
| SEC-AUTH-004 | `SESSION_SECRET` | セッション暗号化シークレット | 256bit+ ランダム文字列 | 30日 |
| SEC-AUTH-005 | `REFRESH_TOKEN_SECRET` | RefreshTokenハッシュ用シークレット | 256bit+ ランダム文字列 | 90日 |

#### データベースシークレット

| ID | シークレット名 | 用途 | 形式 | ローテーション周期 |
|----|---------------|------|------|-------------------|
| SEC-DB-001 | `DB_HOST` | データベースホスト | ホスト名/IP | 固定（インフラ変更時） |
| SEC-DB-002 | `DB_PORT` | データベースポート | 整数 | 固定 |
| SEC-DB-003 | `DB_NAME` | データベース名 | 文字列 | 固定 |
| SEC-DB-004 | `DB_USERNAME` | データベースユーザー名 | 文字列 | 90日（推奨） |
| SEC-DB-005 | `DB_PASSWORD` | データベースパスワード | 256bit+ ランダム文字列 | 90日 |
| SEC-DB-006 | `DB_SSL_CERT` | SSL接続用証明書 | PEM | 年次 |

#### 外部APIシークレット

| ID | シークレット名 | 用途 | 形式 | ローテーション周期 |
|----|---------------|------|------|-------------------|
| SEC-EXT-001 | `PAYMENT_GATEWAY_API_KEY` | 決済ゲートウェイ認証キー | APIキー形式 | 年次または漏洩時 |
| SEC-EXT-002 | `PAYMENT_GATEWAY_SECRET` | 決済ゲートウェイシークレット | シークレット形式 | 年次または漏洩時 |
| SEC-EXT-003 | `PAYMENT_GATEWAY_WEBHOOK_SECRET` | Webhook署名検証シークレット | 256bit+ | 年次または漏洩時 |
| SEC-EXT-004 | `NOTIFICATION_SERVICE_API_KEY` | 通知サービスAPIキー | APIキー形式 | 年次 |

#### 暗号化キー

| ID | シークレット名 | 用途 | 形式 | ローテーション周期 |
|----|---------------|------|------|-------------------|
| SEC-ENC-001 | `DATA_ENCRYPTION_KEY` | 機密データ暗号化（AES-256-GCM） | 256bit | 年次 |
| SEC-ENC-002 | `KEY_ENCRYPTION_KEY` | データキー暗号化用マスターキー | 256bit | 2年 |

#### インフラシークレット

| ID | シークレット名 | 用途 | 形式 | ローテーション周期 |
|----|---------------|------|------|-------------------|
| SEC-INF-001 | `CLOUD_ACCESS_KEY_ID` | クラウドAPI認証（AWS等） | アクセスキーID | 90日 |
| SEC-INF-002 | `CLOUD_SECRET_ACCESS_KEY` | クラウドAPI認証シークレット | シークレットキー | 90日 |
| SEC-INF-003 | `CONTAINER_REGISTRY_TOKEN` | コンテナレジストリ認証 | トークン | 年次 |

---

## 2. シークレット管理方針

### 2.1 管理方式の比較

| 方式 | メリット | デメリット | 推奨用途 |
|------|----------|-----------|----------|
| **環境変数** | シンプル、フレームワーク対応 | ローテーション手動、監査困難 | 開発環境、シンプルな本番 |
| **Secrets Manager** | 自動ローテーション、監査ログ、アクセス制御 | コスト、複雑性 | 本番環境 |
| **Vault (HashiCorp)** | 動的シークレット、詳細なポリシー | 運用負荷 | 大規模本番環境 |
| **Kubernetes Secrets** | K8sネイティブ、Pod自動マウント | 暗号化なし（デフォルト）、単一クラスタ | K8s環境 |

### 2.2 環境別推奨方式

| 環境 | 推奨方式 | 理由 |
|------|----------|------|
| **ローカル開発** | 環境変数（.env.local） | シンプル、高速な開発サイクル |
| **CI/CD** | GitHub Secrets / GitLab CI Variables | パイプライン統合 |
| **ステージング** | Secrets Manager または環境変数 | 本番に近い構成 |
| **本番** | AWS Secrets Manager / HashiCorp Vault | 自動ローテーション、監査 |

### 2.3 本番環境：AWS Secrets Manager構成

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Secrets Manager                       │
├─────────────────────────────────────────────────────────────┤
│  booking-payment/prod/database                               │
│  ├─ DB_HOST                                                  │
│  ├─ DB_PORT                                                  │
│  ├─ DB_NAME                                                  │
│  ├─ DB_USERNAME                                              │
│  └─ DB_PASSWORD                                              │
├─────────────────────────────────────────────────────────────┤
│  booking-payment/prod/jwt                                    │
│  ├─ JWT_PRIVATE_KEY                                          │
│  ├─ JWT_PUBLIC_KEY                                           │
│  └─ JWT_KEY_ID                                               │
├─────────────────────────────────────────────────────────────┤
│  booking-payment/prod/payment-gateway                        │
│  ├─ PAYMENT_GATEWAY_API_KEY                                  │
│  ├─ PAYMENT_GATEWAY_SECRET                                   │
│  └─ PAYMENT_GATEWAY_WEBHOOK_SECRET                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 アクセス制御

| シークレット分類 | アクセス許可対象 | IAMポリシー例 |
|-----------------|-----------------|---------------|
| 認証系 | IAMサービス、認証サービス | `arn:aws:secretsmanager:*:*:secret:booking-payment/*/jwt*` |
| データベース | 全サービス | `arn:aws:secretsmanager:*:*:secret:booking-payment/*/database*` |
| 外部API | Paymentサービス | `arn:aws:secretsmanager:*:*:secret:booking-payment/*/payment-gateway*` |

---

## 3. ローテーション戦略

### 3.1 ローテーション周期一覧

| シークレット種別 | ローテーション周期 | 自動化 |
|-----------------|-------------------|--------|
| JWT署名鍵 | 90日 | 半自動（承認後実行） |
| DBパスワード | 90日 | 自動（Secrets Manager） |
| 外部APIキー | 年次 | 手動（ベンダー依存） |
| セッションシークレット | 30日 | 自動 |
| 暗号化キー | 年次 | 手動（承認必須） |

### 3.2 JWT鍵ローテーション手順

JWTの鍵ローテーションは、サービス停止なしで実行できる設計とします。

```
Phase 1: 新鍵の追加（デュアルキー状態）
┌─────────────────────────────────────────┐
│  1. 新しい鍵ペアを生成                    │
│  2. 新しいJWT_KEY_IDを割り当て            │
│  3. Secrets Managerに新鍵を追加           │
│  4. アプリケーションを再デプロイ           │
│     - 署名: 新鍵を使用                    │
│     - 検証: 新旧両方の鍵を許可            │
└─────────────────────────────────────────┘
          │
          ▼ (旧トークンの有効期限経過を待つ: 最大1時間)
          │
Phase 2: 旧鍵の削除
┌─────────────────────────────────────────┐
│  5. 旧鍵をSecrets Managerから削除         │
│  6. アプリケーションを再デプロイ           │
│     - 検証: 新鍵のみ許可                  │
└─────────────────────────────────────────┘
```

**実装例：複数鍵対応の検証ロジック**

```java
@Component
public class JwtKeyProvider {
    private final Map<String, PublicKey> publicKeys;

    public PublicKey getPublicKey(String keyId) {
        PublicKey key = publicKeys.get(keyId);
        if (key == null) {
            throw new InvalidKeyException("Unknown key ID: " + keyId);
        }
        return key;
    }

    public String getCurrentKeyId() {
        // 署名に使用する最新のキーIDを返す
        return currentKeyId;
    }
}
```

### 3.3 DBパスワードローテーション手順

AWS Secrets Managerの自動ローテーションを使用します。

```yaml
# CloudFormation / Terraform
RotationSchedule:
  RotateImmediatelyOnUpdate: true
  ScheduleExpression: "rate(90 days)"
  RotationLambdaARN: !Ref RotationLambda
```

**ローテーションフロー：**

```
1. Secrets Managerがローテーションをトリガー
2. Lambda関数が新パスワードを生成
3. DBに新パスワードを設定（ALTER USER）
4. Secrets Managerのシークレット値を更新
5. アプリケーションが次回接続時に新パスワードを取得
```

### 3.4 緊急ローテーション

シークレット漏洩が疑われる場合の緊急ローテーション手順：

```
1. インシデント宣言
   ├─ セキュリティチームに通知
   └─ 影響範囲の特定

2. 即座に新シークレットを生成・デプロイ
   ├─ JWT鍵: 全セッション無効化を許容
   ├─ DBパスワード: 接続断を許容
   └─ APIキー: ベンダーに連絡して即座に無効化

3. 旧シークレットの無効化
   ├─ Secrets Managerから削除
   └─ 外部サービスで失効処理

4. 事後分析
   ├─ 漏洩経路の特定
   └─ 再発防止策の実施
```

---

## 4. 開発環境でのシークレット管理

### 4.1 .env.local の使用

開発環境では `.env.local` ファイルを使用します。

```bash
# .env.local（.gitignoreに追加済み）

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=booking_payment_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password_12345

# JWT (開発用、本番と異なる鍵を使用)
JWT_PRIVATE_KEY_PATH=/path/to/dev-private.pem
JWT_PUBLIC_KEY_PATH=/path/to/dev-public.pem
JWT_KEY_ID=dev-key-001

# Payment Gateway (テストモード)
PAYMENT_GATEWAY_API_KEY=sk_test_xxxxxxxxxxxx
PAYMENT_GATEWAY_SECRET=whsec_test_xxxxxxxxxxxx
```

### 4.2 開発用シークレットの生成

```bash
# JWT鍵ペアの生成
openssl genrsa -out dev-private.pem 2048
openssl rsa -in dev-private.pem -pubout -out dev-public.pem

# ランダムシークレットの生成
openssl rand -base64 32  # 256bit
```

### 4.3 CI/CD用シークレット

GitHub Actionsでのシークレット設定：

```yaml
# .github/workflows/ci.yaml
jobs:
  test:
    runs-on: ubuntu-latest
    env:
      DB_PASSWORD: ${{ secrets.TEST_DB_PASSWORD }}
      JWT_PRIVATE_KEY: ${{ secrets.TEST_JWT_PRIVATE_KEY }}
```

---

## 5. 禁止事項

### 5.1 絶対禁止

| 禁止事項 | 理由 | 検出方法 |
|----------|------|----------|
| シークレットのGitコミット | 履歴に残り、復元可能 | git-secrets、pre-commit hook |
| ログへのシークレット出力 | 監視システムで露出 | ログスキャン |
| エラーメッセージへの含有 | クライアントに露出 | セキュリティテスト |
| チャット/メールでの共有 | 履歴に残る | セキュリティ教育 |
| ハードコーディング | 変更困難、履歴に残る | 静的解析 |

### 5.2 検出ツール設定

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/awslabs/git-secrets
    rev: master
    hooks:
      - id: git-secrets
        args: ['--scan']

  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.18.0
    hooks:
      - id: gitleaks
```

### 5.3 .gitignore設定

```gitignore
# Secrets
.env
.env.local
.env.*.local
*.pem
*.key
secrets/
credentials/
```

---

## 6. 監査とモニタリング

### 6.1 監査ログ

| イベント | ログ項目 | 保持期間 |
|----------|----------|----------|
| シークレットアクセス | 誰が、いつ、どのシークレットに | 1年 |
| シークレット変更 | 変更者、変更日時、変更種別 | 7年 |
| ローテーション実行 | 実行者、対象、結果 | 7年 |
| アクセス拒否 | 試行者、対象、拒否理由 | 1年 |

### 6.2 アラート設定

| 条件 | 重要度 | 通知先 |
|------|--------|--------|
| 通常時間外のシークレットアクセス | 警告 | セキュリティチーム |
| 短時間での大量アクセス | 緊急 | セキュリティチーム + オンコール |
| 権限のないアクセス試行 | 緊急 | セキュリティチーム |
| ローテーション失敗 | 警告 | 運用チーム |

---

## 7. 関連ドキュメント

| ドキュメント | 内容 |
|--------------|------|
| `docs/security/pii-policy.md` | PII保護ポリシー |
| `docs/design/contexts/iam.md` | IAMコンテキスト設計 |
| `docs/design/contexts/payment.md` | Paymentコンテキスト設計 |
| `docs/runbook/incident-secret-leak.md` | シークレット漏洩時の対応手順（作成予定） |

---

## 8. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| ローテーション周期 | NIST SP 800-63B、PCI DSS | 業界標準 |
| JWT鍵方式（RS256） | RFC 7518、OAuth 2.0 Best Practices | 非対称鍵による安全な検証 |
| Secrets Manager | AWS Well-Architected Framework | クラウドベストプラクティス |
| 監査ログ保持期間 | 電子帳簿保存法、SOC 2 | コンプライアンス要件 |

---

## 9. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| Vault導入 | HashiCorp Vaultの導入時期・要否 | 中（スケール時） |
| 鍵管理サービス | AWS KMS vs CloudHSM | 中 |
| シークレットの暗号化 | 保存時の追加暗号化要否 | 低 |
| マルチリージョン | シークレットのリージョン間レプリケーション | 低 |
