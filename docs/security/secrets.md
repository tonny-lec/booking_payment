---
doc_type: "security_policy"
id: "secrets"
version: "1.1"
last_updated: "2026-01-25"
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

## 4. シークレット漏洩対応手順

### 4.1 概要

シークレット漏洩インシデントは、システムセキュリティに対する重大な脅威です。本セクションでは、漏洩の検知から復旧、再発防止までの詳細な対応手順を定義します。

### 4.2 漏洩検知

#### 検知経路

| 検知経路 | 内容 | 初動対応 |
|----------|------|----------|
| **自動検知** | git-secrets、Gitleaks、AWS GuardDuty等のツールによる検知 | 自動アラート → インシデント対応開始 |
| **外部通報** | セキュリティ研究者、バグバウンティプログラム経由 | 通報内容の検証 → インシデント対応開始 |
| **内部報告** | 開発者・運用者からの報告 | 報告内容の検証 → インシデント対応開始 |
| **異常検知** | 不審なアクセスパターン、権限外アクセス試行 | ログ分析 → 漏洩の可能性判断 |
| **第三者経由** | GitHub等でのパブリックリポジトリスキャン | 即座に対応開始 |

#### 検知ツール設定

```yaml
# AWS GuardDuty設定例（Terraform）
resource "aws_guardduty_detector" "main" {
  enable = true
  finding_publishing_frequency = "FIFTEEN_MINUTES"
}

# CloudWatch Alarmsによる異常検知
resource "aws_cloudwatch_metric_alarm" "secret_access_anomaly" {
  alarm_name          = "SecretAccessAnomaly"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "SecretAccess"
  namespace           = "AWS/SecretsManager"
  period              = 300
  statistic           = "Sum"
  threshold           = 100
  alarm_description   = "Unusual number of secret access attempts"
  alarm_actions       = [aws_sns_topic.security_alerts.arn]
}
```

### 4.3 対応フロー

#### Phase 1: インシデント宣言と初動対応（0〜15分）

```
┌─────────────────────────────────────────────────────────────────┐
│  Phase 1: インシデント宣言と初動対応（目標: 15分以内）            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Step 1.1: インシデント宣言                                       │
│  ├─ インシデント管理システムにチケット作成                         │
│  ├─ 重要度判定（Critical / High / Medium）                       │
│  └─ インシデントコマンダーの任命                                  │
│                                                                  │
│  Step 1.2: 即時通知                                              │
│  ├─ セキュリティチーム（Slack: #security-incidents）             │
│  ├─ オンコールエンジニア（PagerDuty）                            │
│  ├─ 経営層（Critical の場合）                                    │
│  └─ 法務チーム（個人情報関連の場合）                              │
│                                                                  │
│  Step 1.3: 影響範囲の初期評価                                    │
│  ├─ 漏洩したシークレットの種別特定                                │
│  ├─ 影響を受けるシステム・サービスの特定                          │
│  └─ 悪用の有無の初期調査                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Phase 2: 封じ込め（15分〜1時間）

```
┌─────────────────────────────────────────────────────────────────┐
│  Phase 2: 封じ込め（目標: 1時間以内）                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Step 2.1: 漏洩シークレットの無効化                               │
│                                                                  │
│  [JWT署名鍵の場合]                                               │
│  ├─ 新しい鍵ペアを生成                                           │
│  ├─ Secrets Managerに新鍵を登録                                  │
│  ├─ アプリケーションを緊急デプロイ（新鍵で署名開始）              │
│  └─ 旧鍵での検証を即座に無効化（全セッション強制ログアウト）       │
│                                                                  │
│  [DBパスワードの場合]                                            │
│  ├─ 新パスワードを生成                                           │
│  ├─ DBで ALTER USER を実行                                       │
│  ├─ Secrets Managerを更新                                        │
│  └─ アプリケーションを再起動                                      │
│                                                                  │
│  [外部APIキーの場合]                                             │
│  ├─ ベンダーのダッシュボードで即座にキーを無効化                  │
│  ├─ 新キーを発行                                                 │
│  ├─ Secrets Managerを更新                                        │
│  └─ アプリケーションを再デプロイ                                  │
│                                                                  │
│  Step 2.2: アクセスログの保全                                    │
│  ├─ CloudTrailログのスナップショット取得                         │
│  ├─ アプリケーションログの保全                                    │
│  └─ DBアクセスログの保全                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Phase 3: 根絶と復旧（1時間〜24時間）

```
┌─────────────────────────────────────────────────────────────────┐
│  Phase 3: 根絶と復旧（目標: 24時間以内）                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Step 3.1: 漏洩経路の特定                                        │
│  ├─ Gitリポジトリ履歴の調査                                      │
│  ├─ CI/CDパイプラインログの調査                                  │
│  ├─ 開発者端末・アクセス権限の調査                                │
│  └─ サードパーティ連携の調査                                      │
│                                                                  │
│  Step 3.2: 悪用調査                                              │
│  ├─ 不正アクセスの痕跡調査                                       │
│  ├─ データ流出の有無確認                                         │
│  ├─ 不正な操作（作成・変更・削除）の確認                          │
│  └─ 横展開の可能性調査                                           │
│                                                                  │
│  Step 3.3: 追加の封じ込め措置                                    │
│  ├─ 関連するシークレットの予防的ローテーション                     │
│  ├─ 影響を受けた可能性のあるユーザーへの通知                      │
│  └─ 必要に応じてサービスの一時停止                                │
│                                                                  │
│  Step 3.4: システム復旧                                          │
│  ├─ 新シークレットでの正常稼働確認                                │
│  ├─ 監視強化（一時的な閾値引き下げ）                              │
│  └─ 段階的なサービス復旧                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Phase 4: 事後対応（24時間〜1週間）

```
┌─────────────────────────────────────────────────────────────────┐
│  Phase 4: 事後対応（1週間以内）                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Step 4.1: インシデントレポート作成                               │
│  ├─ タイムライン（検知〜復旧）                                    │
│  ├─ 影響範囲と被害状況                                           │
│  ├─ 根本原因分析（RCA）                                          │
│  └─ 対応の評価（良かった点・改善点）                              │
│                                                                  │
│  Step 4.2: 再発防止策の実施                                      │
│  ├─ 技術的対策（検知強化、アクセス制御強化等）                    │
│  ├─ プロセス改善（レビュー強化、教育等）                          │
│  └─ ドキュメント更新（本ドキュメント、Runbook等）                 │
│                                                                  │
│  Step 4.3: 報告・通知                                            │
│  ├─ 経営層への最終報告                                           │
│  ├─ 影響を受けたユーザーへの通知（必要な場合）                    │
│  ├─ 規制当局への報告（必要な場合）                                │
│  └─ 保険会社への通知（サイバー保険適用の場合）                    │
│                                                                  │
│  Step 4.4: フォローアップ                                        │
│  ├─ 再発防止策の効果検証                                         │
│  ├─ 追加の監視・監査                                             │
│  └─ 定期的なレビュー（3ヶ月、6ヶ月、1年後）                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 4.4 シークレット種別ごとの対応詳細

#### JWT署名鍵漏洩時

```bash
#!/bin/bash
# jwt-key-rotation-emergency.sh
# 緊急JWT鍵ローテーションスクリプト

set -euo pipefail

# 1. 新しい鍵ペアを生成
echo "Generating new RSA key pair..."
openssl genrsa -out /tmp/new-private.pem 2048
openssl rsa -in /tmp/new-private.pem -pubout -out /tmp/new-public.pem

# 2. 新しいKey IDを生成
NEW_KEY_ID=$(uuidgen)
echo "New Key ID: ${NEW_KEY_ID}"

# 3. Secrets Managerを更新
echo "Updating Secrets Manager..."
aws secretsmanager update-secret \
    --secret-id "booking-payment/prod/jwt" \
    --secret-string "{
        \"JWT_PRIVATE_KEY\": \"$(cat /tmp/new-private.pem | base64 -w0)\",
        \"JWT_PUBLIC_KEY\": \"$(cat /tmp/new-public.pem | base64 -w0)\",
        \"JWT_KEY_ID\": \"${NEW_KEY_ID}\"
    }"

# 4. アプリケーションの再デプロイをトリガー
echo "Triggering deployment..."
# kubectl rollout restart deployment/iam-service -n booking-payment

# 5. 後処理
rm -f /tmp/new-private.pem /tmp/new-public.pem
echo "Emergency rotation completed. All existing tokens are now invalid."
```

**ユーザー影響**
- 全ユーザーが強制ログアウトされる
- 再ログインが必要

**通知テンプレート**
```
件名: [重要] セキュリティ更新のお知らせ

セキュリティ強化のため、システムの認証情報を更新いたしました。
お手数ですが、再度ログインをお願いいたします。

ご不明な点がございましたら、サポートまでお問い合わせください。
```

#### DBパスワード漏洩時

```bash
#!/bin/bash
# db-password-rotation-emergency.sh
# 緊急DBパスワードローテーションスクリプト

set -euo pipefail

DB_HOST="${DB_HOST}"
DB_NAME="${DB_NAME}"
DB_USERNAME="${DB_USERNAME}"

# 1. 新しいパスワードを生成
NEW_PASSWORD=$(openssl rand -base64 32)

# 2. DBで新パスワードを設定
echo "Updating database password..."
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U postgres -d "${DB_NAME}" \
    -c "ALTER USER ${DB_USERNAME} WITH PASSWORD '${NEW_PASSWORD}';"

# 3. Secrets Managerを更新
echo "Updating Secrets Manager..."
aws secretsmanager update-secret \
    --secret-id "booking-payment/prod/database" \
    --secret-string "{
        \"DB_HOST\": \"${DB_HOST}\",
        \"DB_PORT\": \"5432\",
        \"DB_NAME\": \"${DB_NAME}\",
        \"DB_USERNAME\": \"${DB_USERNAME}\",
        \"DB_PASSWORD\": \"${NEW_PASSWORD}\"
    }"

# 4. アプリケーションの再起動
echo "Restarting applications..."
# kubectl rollout restart deployment -l app.kubernetes.io/part-of=booking-payment -n booking-payment

echo "Emergency rotation completed."
```

**ユーザー影響**
- 一時的な接続エラー（数秒〜数分）
- 進行中のトランザクションの失敗

#### 外部APIキー漏洩時

| ベンダー | 無効化手順 | 新キー発行 | 確認方法 |
|----------|-----------|-----------|----------|
| Payment Gateway | ダッシュボードで「Revoke」 | ダッシュボードで発行 | テストAPIで動作確認 |
| Notification Service | API経由で無効化 | ダッシュボードで発行 | ヘルスチェックAPI |

### 4.5 エスカレーションマトリクス

| 重要度 | 定義 | 対応時間目標 | 通知先 |
|--------|------|-------------|--------|
| **Critical** | 本番環境のDB/JWT鍵、決済APIキーの漏洩 | 即時対応、15分以内に封じ込め開始 | CTO、CISO、法務、オンコール全員 |
| **High** | 本番環境のその他シークレット漏洩 | 1時間以内に対応開始 | セキュリティチームリード、オンコール |
| **Medium** | ステージング環境のシークレット漏洩 | 4時間以内に対応 | セキュリティチーム |
| **Low** | 開発環境のシークレット漏洩 | 24時間以内に対応 | 担当開発者 |

### 4.6 連絡先一覧

| 役割 | 連絡先 | 連絡方法 |
|------|--------|----------|
| セキュリティチームリード | security-lead@example.com | Slack, Email |
| オンコールエンジニア | - | PagerDuty |
| CTO | cto@example.com | 電話, Slack |
| 法務チーム | legal@example.com | Email |
| Payment Gatewayサポート | support@payment-vendor.com | 電話（緊急時） |

### 4.7 チェックリスト

#### 検知時チェックリスト

- [ ] インシデントチケットを作成した
- [ ] 重要度を判定した
- [ ] インシデントコマンダーを任命した
- [ ] 必要な関係者に通知した
- [ ] 漏洩したシークレットを特定した

#### 封じ込めチェックリスト

- [ ] 漏洩シークレットを無効化した
- [ ] 新シークレットを生成・設定した
- [ ] アプリケーションを再デプロイした
- [ ] 正常稼働を確認した
- [ ] アクセスログを保全した

#### 事後対応チェックリスト

- [ ] 漏洩経路を特定した
- [ ] 悪用の有無を確認した
- [ ] インシデントレポートを作成した
- [ ] 再発防止策を実施した
- [ ] 関係者に最終報告した
- [ ] ドキュメントを更新した

---

## 5. 開発環境でのシークレット管理

### 5.1 .env.local の使用

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

### 5.2 開発用シークレットの生成

```bash
# JWT鍵ペアの生成
openssl genrsa -out dev-private.pem 2048
openssl rsa -in dev-private.pem -pubout -out dev-public.pem

# ランダムシークレットの生成
openssl rand -base64 32  # 256bit
```

### 5.3 CI/CD用シークレット

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

## 6. 禁止事項

### 6.1 絶対禁止

| 禁止事項 | 理由 | 検出方法 |
|----------|------|----------|
| シークレットのGitコミット | 履歴に残り、復元可能 | git-secrets、pre-commit hook |
| ログへのシークレット出力 | 監視システムで露出 | ログスキャン |
| エラーメッセージへの含有 | クライアントに露出 | セキュリティテスト |
| チャット/メールでの共有 | 履歴に残る | セキュリティ教育 |
| ハードコーディング | 変更困難、履歴に残る | 静的解析 |

### 6.2 検出ツール設定

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

### 6.3 .gitignore設定

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

## 7. 監査とモニタリング

### 7.1 監査ログ

| イベント | ログ項目 | 保持期間 |
|----------|----------|----------|
| シークレットアクセス | 誰が、いつ、どのシークレットに | 1年 |
| シークレット変更 | 変更者、変更日時、変更種別 | 7年 |
| ローテーション実行 | 実行者、対象、結果 | 7年 |
| アクセス拒否 | 試行者、対象、拒否理由 | 1年 |

### 7.2 アラート設定

| 条件 | 重要度 | 通知先 |
|------|--------|--------|
| 通常時間外のシークレットアクセス | 警告 | セキュリティチーム |
| 短時間での大量アクセス | 緊急 | セキュリティチーム + オンコール |
| 権限のないアクセス試行 | 緊急 | セキュリティチーム |
| ローテーション失敗 | 警告 | 運用チーム |

---

## 8. 関連ドキュメント

| ドキュメント | 内容 |
|--------------|------|
| `docs/security/pii-policy.md` | PII保護ポリシー |
| `docs/design/contexts/iam.md` | IAMコンテキスト設計 |
| `docs/design/contexts/payment.md` | Paymentコンテキスト設計 |
| 本ドキュメント セクション4 | シークレット漏洩対応手順 |

---

## 9. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| ローテーション周期 | NIST SP 800-63B、PCI DSS | 業界標準 |
| JWT鍵方式（RS256） | RFC 7518、OAuth 2.0 Best Practices | 非対称鍵による安全な検証 |
| Secrets Manager | AWS Well-Architected Framework | クラウドベストプラクティス |
| 監査ログ保持期間 | 電子帳簿保存法、SOC 2 | コンプライアンス要件 |

---

## 10. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| Vault導入 | HashiCorp Vaultの導入時期・要否 | 中（スケール時） |
| 鍵管理サービス | AWS KMS vs CloudHSM | 中 |
| シークレットの暗号化 | 保存時の追加暗号化要否 | 低 |
| マルチリージョン | シークレットのリージョン間レプリケーション | 低 |
