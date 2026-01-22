---
doc_type: "security_policy"
id: "pii-policy"
version: "1.0"
last_updated: "2026-01-17"
status: "draft"
---

# PII（個人情報）ポリシー

本ドキュメントは、予約・決済基盤における個人情報（PII: Personally Identifiable Information）の定義、取り扱いルール、マスキング方針、ログ出力禁止項目、および保持期間を定義します。

---

## 1. PII定義

### 1.1 PII分類

PIIを以下の3カテゴリに分類します。

| カテゴリ | 説明 | 取り扱い |
|----------|------|----------|
| **禁止項目** | ログ・トレース・メトリクスへの出力を一切禁止 | 出力厳禁、違反時は即時対応 |
| **マスク必須項目** | 出力時は必ずマスキング処理を適用 | マスキング後のみ出力可 |
| **識別子** | そのままでは個人特定不可だが、他情報と組み合わせで特定可能 | 必要最小限の出力、アクセス制御 |

### 1.2 禁止項目（出力厳禁）

以下の情報は、いかなる状況でもログ、トレース、メトリクス、エラーレスポンスに出力してはなりません。

| 項目 | 説明 | 例 |
|------|------|-----|
| **パスワード** | 認証用パスワード（平文・ハッシュ含む） | `password`, `passwordHash` |
| **クレジットカード番号** | PAN（Primary Account Number） | `4111111111111111` |
| **CVV/CVC** | カードセキュリティコード | `123` |
| **カード有効期限** | カードの有効期限 | `12/25` |
| **銀行口座番号** | 銀行口座の番号 | `1234567` |
| **マイナンバー** | 個人番号（日本） | `123456789012` |
| **社会保障番号** | SSN（米国）等 | `123-45-6789` |
| **生体認証データ** | 指紋、顔認証データ等 | バイナリデータ |
| **暗号化キー/シークレット** | JWT秘密鍵、API Key等 | `sk_live_xxx...` |
| **RefreshToken** | 認証トークン（長命） | `eyJhbGciOi...` |

### 1.3 マスク必須項目

以下の情報は、デバッグ目的で出力が必要な場合、必ずマスキング処理を適用します。

| 項目 | 説明 | マスキング例 |
|------|------|-------------|
| **メールアドレス** | ユーザーのメールアドレス | `t***@example.com` |
| **電話番号** | 連絡先電話番号 | `090-****-5678` |
| **IPアドレス** | クライアントのIPアドレス | `192.168.***.***` または `192.168.0.0/16` |
| **氏名** | ユーザーの氏名 | `山***` |
| **住所** | 詳細住所 | `東京都***` |
| **AccessToken** | JWTアクセストークン | `eyJ***...***` （先頭3文字 + 末尾3文字のみ）|

### 1.4 識別子（内部ID）

以下の識別子は、単独では個人特定が困難ですが、他情報との組み合わせで特定可能なため、必要最小限の出力とアクセス制御を適用します。

| 項目 | 説明 | 出力可否 |
|------|------|----------|
| **UserId** | ユーザーを識別するUUID | ログ・トレースに出力可（必要時） |
| **BookingId** | 予約を識別するUUID | ログ・トレースに出力可 |
| **PaymentId** | 支払いを識別するUUID | ログ・トレースに出力可 |
| **SessionId** | セッションを識別するID | ログに出力可（ローテーション必須） |
| **RequestId** | リクエストを識別するID | ログ・トレースに出力可 |
| **TraceId/SpanId** | 分散トレース用ID | ログ・トレースに出力可 |

---

## 2. マスキングルール

### 2.1 マスキング方式

| 方式 | 説明 | 用途 |
|------|------|------|
| **部分マスク** | 一部を `*` で置換 | メールアドレス、電話番号 |
| **ネットワークマスク** | IPアドレスの一部をマスク | IPアドレス |
| **ハッシュ化** | 一方向ハッシュで置換 | 相関分析が必要な場合 |
| **完全置換** | 固定文字列で置換 | 禁止項目が誤って渡された場合 |

### 2.2 項目別マスキング実装

#### メールアドレス

```
入力: user@example.com
出力: u***@example.com

入力: john.doe@company.co.jp
出力: j***@company.co.jp
```

**ルール**：ローカルパート（@より前）の最初の1文字のみ表示、残りを `***` で置換。ドメイン部分は保持。

#### 電話番号

```
入力: 090-1234-5678
出力: 090-****-5678

入力: +81-90-1234-5678
出力: +81-90-****-5678
```

**ルール**：中間の4桁を `****` で置換。

#### IPアドレス

```
IPv4:
  入力: 192.168.1.100
  出力: 192.168.***.*** または 192.168.0.0/16

IPv6:
  入力: 2001:0db8:85a3:0000:0000:8a2e:0370:7334
  出力: 2001:0db8:***:***:***:***:***:***
```

**ルール**：IPv4は最後の2オクテットをマスク、IPv6は最初の2セグメント以外をマスク。

#### 氏名

```
入力: 山田太郎
出力: 山***

入力: John Doe
出力: J***
```

**ルール**：最初の1文字のみ表示。

#### AccessToken

```
入力: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOi...
出力: eyJ***...*** （トークンの存在のみ記録、内容は非表示）
```

**ルール**：先頭3文字のみ表示し、残りを省略。

### 2.3 マスキング実装例（Java）

```java
public class PiiMasker {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^(.)[^@]*(@.+)$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^(\\+?[0-9]{2,4}[-\\s]?[0-9]{2,3}[-\\s]?)([0-9]{4})([-\\s]?[0-9]{4})$");

    public static String maskEmail(String email) {
        if (email == null) return null;
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.matches()) {
            return matcher.group(1) + "***" + matcher.group(2);
        }
        return "***@***";
    }

    public static String maskPhone(String phone) {
        if (phone == null) return null;
        Matcher matcher = PHONE_PATTERN.matcher(phone);
        if (matcher.matches()) {
            return matcher.group(1) + "****" + matcher.group(3);
        }
        return "***-****-****";
    }

    public static String maskIpAddress(String ip) {
        if (ip == null) return null;
        if (ip.contains(":")) {
            // IPv6
            String[] parts = ip.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":***:***:***:***:***:***";
            }
        } else {
            // IPv4
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.***";
            }
        }
        return "***.***.***.***";
    }

    public static String maskToken(String token) {
        if (token == null) return null;
        if (token.length() <= 6) return "***";
        return token.substring(0, 3) + "***...***";
    }
}
```

### 2.4 Logback設定例

```xml
<configuration>
    <conversionRule conversionWord="maskedMsg"
                    converterClass="com.example.logging.PiiMaskingConverter" />

    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"booking-payment"}</customFields>
            <fieldNames>
                <message>message</message>
            </fieldNames>
            <!-- PIIマスキングを適用 -->
            <messagePattern>%maskedMsg</messagePattern>
        </encoder>
    </appender>
</configuration>
```

---

## 3. ログ出力禁止項目

### 3.1 絶対禁止項目

以下の情報は、**いかなるログレベルでも出力禁止**です。DEBUGレベルでも出力してはなりません。

| カテゴリ | 禁止項目 | 検出パターン |
|----------|----------|-------------|
| **認証情報** | パスワード（平文/ハッシュ） | `password`, `passwd`, `pwd`, `secret` |
| **認証情報** | RefreshToken | `refresh_token`, `refreshToken` |
| **認証情報** | APIキー/シークレット | `api_key`, `apiKey`, `secret_key`, `sk_live_`, `sk_test_` |
| **決済情報** | カード番号（PAN） | `card_number`, `cardNumber`, `pan`, 16桁数字 |
| **決済情報** | CVV/CVC | `cvv`, `cvc`, `security_code` |
| **決済情報** | 有効期限 | `expiry`, `exp_month`, `exp_year` |
| **個人番号** | マイナンバー | 12桁数字 |
| **個人番号** | SSN | `ssn`, XXX-XX-XXXX形式 |

### 3.2 検出と防止

#### 静的解析（CI/CD）

```yaml
# .github/workflows/pii-check.yaml
name: PII Check
on: [push, pull_request]
jobs:
  pii-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Scan for PII patterns
        run: |
          # 禁止パターンの検出
          grep -rn --include="*.java" -E \
            '(password|cardNumber|cvv|ssn)' src/ && exit 1 || exit 0
```

#### ランタイム検出（ログフィルター）

```java
public class PiiFilteringAppender extends AppenderBase<ILoggingEvent> {

    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
        Pattern.compile("password\\s*[=:]\\s*['\"]?[^'\"\\s]+"),
        Pattern.compile("\\b\\d{16}\\b"),  // カード番号
        Pattern.compile("\\b\\d{3,4}\\b(?=.*cvv)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("sk_(live|test)_[a-zA-Z0-9]+")  // Stripe APIキー
    );

    @Override
    protected void append(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(message).find()) {
                // 禁止パターンを検出した場合
                addError("PII detected in log message: " + event.getLoggerName());
                // マスク処理または出力抑制
                return;
            }
        }
        // 正常出力
    }
}
```

### 3.3 ログレベル別ガイドライン

| ログレベル | PII出力 | 用途 |
|------------|---------|------|
| ERROR | 禁止（マスクも不可） | 障害通知、アラート発報 |
| WARN | 禁止（マスクも不可） | 異常検知、閾値超過 |
| INFO | 禁止（マスクも不可） | 正常処理完了、監査ログ |
| DEBUG | 禁止（マスクも不可） | デバッグ情報（本番無効化） |
| TRACE | 禁止（マスクも不可） | 詳細トレース（本番無効化） |

**注意**：DEBUG/TRACEレベルでもPII出力は禁止です。開発環境でもPII出力の習慣を持たないことが重要です。

---

## 4. 保持期間と削除方針

### 4.1 データ種別ごとの保持期間

| データ種別 | 保持期間 | 削除方法 | 根拠 |
|------------|----------|----------|------|
| **アプリケーションログ** | 90日 | 自動削除 | 運用調査目的 |
| **監査ログ** | 7年 | 承認付き削除 | 法的要件（電子帳簿保存法等） |
| **トレースデータ** | 30日 | 自動削除 | パフォーマンス分析目的 |
| **メトリクスデータ** | 1年（集約後） | 自動ダウンサンプリング | 傾向分析目的 |
| **決済データ** | 7年 | 承認付き削除 | 法的要件（カード業界規制） |
| **ユーザーアカウント** | アカウント有効期間 + 30日 | ユーザーリクエストまたは自動 | GDPR等のプライバシー規制 |
| **予約データ** | 予約日から2年 | 自動匿名化 | サービス改善分析 |

### 4.2 削除方法

#### 4.2.1 自動削除

```yaml
# Elasticsearch ILM Policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_age": "7d",
            "max_size": "50gb"
          }
        }
      },
      "warm": {
        "min_age": "30d",
        "actions": {
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

#### 4.2.2 手動削除（承認付き）

監査ログや決済データの削除には、以下の承認プロセスが必要です。

```
1. 削除リクエスト作成（理由、対象範囲、削除者）
2. セキュリティチームによるレビュー
3. 法務/コンプライアンスチームによる承認
4. 削除実行（監査ログに記録）
5. 削除完了確認（復元不可の確認）
```

#### 4.2.3 匿名化

```sql
-- 予約データの匿名化（2年経過後）
UPDATE bookings
SET user_id = 'ANONYMIZED',
    note = NULL,
    updated_at = NOW()
WHERE start_at < NOW() - INTERVAL '2 years'
  AND user_id != 'ANONYMIZED';
```

### 4.3 GDPR対応：データ主体の権利

| 権利 | 対応方法 | SLA |
|------|----------|-----|
| **アクセス権** | データエクスポート機能 | 30日以内 |
| **訂正権** | ユーザープロファイル編集 | 即時 |
| **削除権（忘れられる権利）** | アカウント削除リクエスト | 30日以内 |
| **データポータビリティ** | JSON/CSVエクスポート | 30日以内 |
| **処理制限権** | アカウント一時停止 | 即時 |

### 4.4 削除確認チェックリスト

データ削除時には、以下の確認を実施します。

- [ ] 本番DB：対象レコードの削除完了
- [ ] レプリカDB：レプリケーション完了確認
- [ ] バックアップ：保持期間経過後のバックアップからの除外
- [ ] キャッシュ：Redis等のキャッシュからの削除
- [ ] 検索インデックス：Elasticsearch等からの削除
- [ ] ログ：ログファイルからの除外（または保持期間経過待ち）
- [ ] 監査記録：削除操作の監査ログ記録

---

## 5. 違反時の対応

### 5.1 インシデント分類

| レベル | 説明 | 対応時間 |
|--------|------|----------|
| **Critical** | 禁止項目（パスワード、カード情報）の漏洩 | 即時（1時間以内） |
| **High** | マスク必須項目のマスクなし出力 | 4時間以内 |
| **Medium** | 識別子の過剰出力 | 24時間以内 |
| **Low** | ポリシー違反の可能性（要調査） | 1週間以内 |

### 5.2 対応フロー

```
1. 検出・報告
   ├─ 自動検出（ログスキャン、CI/CD）
   └─ 手動報告（開発者、セキュリティチーム）

2. 初期対応（Critical/High）
   ├─ 該当ログの隔離・削除
   ├─ 影響範囲の特定
   └─ 関係者への通知

3. 原因調査
   ├─ コード変更履歴の確認
   ├─ ログ出力箇所の特定
   └─ 根本原因の分析

4. 是正措置
   ├─ コード修正
   ├─ テスト追加
   └─ デプロイ

5. 再発防止
   ├─ CI/CDチェック強化
   ├─ コードレビュー観点追加
   └─ ドキュメント更新
```

---

## 6. 関連ドキュメント

| ドキュメント | 内容 |
|--------------|------|
| `docs/design/observability.md` | ログ・メトリクス・トレース設計 |
| `docs/security/secrets.md` | シークレット管理方針 |
| `docs/security/threat-model.md` | 脅威モデル |
| `docs/domain/glossary.md` | ドメイン用語集 |

---

## 7. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| PII分類 | GDPR Article 4, 個人情報保護法 | 一般的な分類基準 |
| 保持期間 | 電子帳簿保存法、PCI DSS | 法的要件に基づく |
| マスキング方式 | OWASP Logging Cheat Sheet | 業界標準 |
| 削除プロセス | GDPR Article 17 | 忘れられる権利 |

---

## 8. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| ログ暗号化 | 保存時のログ暗号化要否 | 中 |
| 匿名化アルゴリズム | k-匿名性等の適用検討 | 低 |
| 国別対応 | 地域ごとの規制対応（CCPA等） | 中 |
| 監査ログの保持期間 | 7年以上の要件確認 | 高 |
