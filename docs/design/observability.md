---
doc_type: "design"
id: "observability"
version: "1.0"
last_updated: "2026-01-17"
status: "stable"
---

# Observability設計（SSOT）

本ドキュメントは、予約・決済基盤の観測性（Observability）設計のSSSOTです。
ログ、メトリクス、トレースの3本柱に基づいて設計します。

---

# 1. ログ設計

## 1.1 ログフォーマット（JSON構造化ログ）

すべてのログはJSON形式で出力する。人間が読むためではなく、機械処理（検索、集計、アラート）を前提とする。

```json
{
  "timestamp": "2026-01-17T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.example.booking.application.CreateBookingUseCase",
  "message": "Booking created successfully",
  "traceId": "abc123def456",
  "spanId": "span789",
  "requestId": "req-001",
  "service": "booking-service",
  "version": "1.0.0",
  "environment": "production",
  "context": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "bookingId": "660e8400-e29b-41d4-a716-446655440001",
    "resourceId": "770e8400-e29b-41d4-a716-446655440002"
  }
}
```

## 1.2 必須フィールド

| フィールド | 型 | 説明 | 例 |
|-----------|-----|------|-----|
| `timestamp` | ISO 8601 | イベント発生時刻（UTC） | `2026-01-17T10:30:00.123Z` |
| `level` | String | ログレベル | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `logger` | String | ロガー名（クラス名） | `com.example.booking.CreateBookingUseCase` |
| `message` | String | ログメッセージ | `Booking created successfully` |
| `traceId` | String | 分散トレースID（OpenTelemetry） | `abc123def456` |
| `spanId` | String | スパンID（OpenTelemetry） | `span789` |
| `service` | String | サービス名 | `booking-service` |
| `version` | String | アプリケーションバージョン | `1.0.0` |
| `environment` | String | 環境名 | `production`, `staging`, `development` |

## 1.3 オプションフィールド

| フィールド | 型 | 説明 | 用途 |
|-----------|-----|------|------|
| `requestId` | String | リクエストID | API リクエストの追跡 |
| `userId` | UUID | ユーザーID | ユーザー操作の追跡 |
| `context` | Object | 追加コンテキスト | ドメイン固有の情報 |
| `error` | Object | エラー詳細 | スタックトレース、エラーコード |
| `duration_ms` | Number | 処理時間（ミリ秒） | パフォーマンス分析 |

## 1.4 ログレベル基準

| レベル | 用途 | 例 |
|--------|------|-----|
| `ERROR` | 即座に対応が必要なエラー | DB接続失敗、外部API障害 |
| `WARN` | 注意が必要だが即座の対応は不要 | リトライ発生、閾値超過 |
| `INFO` | 正常な業務イベント | 予約作成成功、支払い完了 |
| `DEBUG` | デバッグ情報（本番では通常無効） | メソッド引数、中間状態 |
| `TRACE` | 詳細なトレース情報（開発時のみ） | ループ内の値、詳細な実行パス |

## 1.5 PIIマスキングルール

### 出力禁止項目（絶対にログ出力しない）

| 項目 | 理由 |
|------|------|
| パスワード | 認証情報の漏洩防止 |
| クレジットカード番号 | PCI DSS準拠 |
| CVV/CVC | PCI DSS準拠 |
| 銀行口座番号 | 金融情報保護 |
| マイナンバー | 法令遵守 |
| APIキー/シークレット | セキュリティ |

### マスキング対象項目（出力時にマスキング）

| 項目 | マスキング方法 | 例 |
|------|---------------|-----|
| メールアドレス | ローカル部分を部分マスク | `t***@example.com` |
| 電話番号 | 末尾4桁以外をマスク | `***-****-1234` |
| IPアドレス | 最終オクテットをマスク | `192.168.1.***` |
| 氏名 | 姓のみ表示 | `山田 ***` |

### マスキング実装

```java
// ログフィルターでの自動マスキング
@Component
public class PiiMaskingFilter implements Filter {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("([a-zA-Z0-9])[a-zA-Z0-9.]*@");

    public String mask(String input) {
        return EMAIL_PATTERN.matcher(input)
            .replaceAll("$1***@");
    }
}
```

---

# 2. メトリクス設計

## 2.1 命名規則（Prometheus形式）

```
<namespace>_<subsystem>_<name>_<unit>
```

| 要素 | 説明 | 例 |
|------|------|-----|
| namespace | サービス/アプリ名 | `booking`, `payment`, `iam` |
| subsystem | サブシステム/コンポーネント | `api`, `db`, `gateway` |
| name | メトリクス名（snake_case） | `requests`, `duration`, `errors` |
| unit | 単位（省略可） | `seconds`, `bytes`, `total` |

### 良い例

```
booking_api_requests_total
booking_api_request_duration_seconds
payment_gateway_errors_total
```

### 避けるべき例

```
bookingApiRequests          # camelCase禁止
booking.api.requests        # ドット禁止
requests                    # namespace必須
```

## 2.2 REDメトリクス（サービス品質指標）

すべてのAPIエンドポイントで以下を計測する。

### Rate（リクエスト数）

```
# リクエスト総数
<service>_api_requests_total{method, endpoint, status}

# 例
booking_api_requests_total{method="POST", endpoint="/bookings", status="201"}
booking_api_requests_total{method="POST", endpoint="/bookings", status="409"}
```

### Errors（エラー数）

```
# エラー総数
<service>_api_errors_total{method, endpoint, error_type}

# 例
booking_api_errors_total{method="POST", endpoint="/bookings", error_type="conflict"}
booking_api_errors_total{method="PUT", endpoint="/bookings/{id}", error_type="version_mismatch"}
```

### Duration（処理時間）

```
# 処理時間ヒストグラム
<service>_api_request_duration_seconds{method, endpoint}

# バケット設定（秒）
buckets: [0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0]
```

## 2.3 サービス固有メトリクス

### Booking サービス

| メトリクス | 型 | ラベル | 説明 |
|-----------|-----|--------|------|
| `booking_create_total` | Counter | status | 予約作成試行数 |
| `booking_create_duration_seconds` | Histogram | status | 予約作成処理時間 |
| `booking_update_total` | Counter | status | 予約更新試行数 |
| `booking_cancel_total` | Counter | status, previous_status | 予約キャンセル試行数 |
| `booking_conflict_total` | Counter | resource_id | 衝突発生数 |
| `booking_version_mismatch_total` | Counter | - | バージョン不一致発生数 |
| `booking_active_count` | Gauge | status | アクティブ予約数 |

### Payment サービス

| メトリクス | 型 | ラベル | 説明 |
|-----------|-----|--------|------|
| `payment_create_total` | Counter | status | 支払い作成試行数 |
| `payment_create_duration_seconds` | Histogram | status | 支払い作成処理時間 |
| `payment_capture_total` | Counter | status | キャプチャ試行数 |
| `payment_refund_total` | Counter | status | 返金試行数 |
| `payment_gateway_latency_seconds` | Histogram | operation | Gateway応答時間 |
| `payment_gateway_errors_total` | Counter | error_type | Gatewayエラー数 |
| `payment_idempotency_hit_total` | Counter | - | 冪等キーヒット数 |

### IAM サービス

| メトリクス | 型 | ラベル | 説明 |
|-----------|-----|--------|------|
| `iam_login_total` | Counter | status | ログイン試行数 |
| `iam_login_duration_seconds` | Histogram | status | ログイン処理時間 |
| `iam_token_refresh_total` | Counter | status | トークンリフレッシュ数 |
| `iam_brute_force_blocked_total` | Counter | - | ブルートフォース検出数 |
| `iam_active_sessions_count` | Gauge | - | アクティブセッション数 |

## 2.4 インフラメトリクス

| メトリクス | 型 | 説明 |
|-----------|-----|------|
| `jvm_memory_used_bytes` | Gauge | JVMメモリ使用量 |
| `jvm_gc_pause_seconds` | Summary | GC停止時間 |
| `db_connections_active` | Gauge | アクティブDB接続数 |
| `db_connections_idle` | Gauge | アイドルDB接続数 |
| `db_query_duration_seconds` | Histogram | DBクエリ実行時間 |

---

# 3. トレース設計

## 3.1 OpenTelemetry設定方針

### SDK設定

```yaml
# otel-config.yaml
service:
  name: ${SERVICE_NAME}
  version: ${APP_VERSION}

exporters:
  otlp:
    endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT}
    headers:
      authorization: "Bearer ${OTEL_AUTH_TOKEN}"

processors:
  batch:
    timeout: 5s
    send_batch_size: 512

  # PIIを含む属性を削除
  attributes:
    actions:
      - key: user.email
        action: delete
      - key: user.phone
        action: delete
```

### 自動計装対象

| 対象 | 計装方法 |
|------|----------|
| HTTP Server | Spring WebMVC自動計装 |
| HTTP Client | RestTemplate/WebClient自動計装 |
| Database | JDBC自動計装 |
| Messaging | Kafka/RabbitMQ自動計装 |

## 3.2 Span命名規則

### 形式

```
<操作種別>.<リソース種別>
```

### 例

| Span名 | 説明 |
|--------|------|
| `Booking.create` | 予約作成 |
| `Booking.update` | 予約更新 |
| `Booking.cancel` | 予約キャンセル |
| `Payment.create` | 支払い作成 |
| `Payment.capture` | 支払いキャプチャ |
| `PaymentGateway.authorize` | Gateway与信 |
| `DB.query` | DBクエリ |
| `HTTP.request` | 外部HTTP呼び出し |

## 3.3 必須属性一覧

### 共通属性

| 属性 | 型 | 説明 |
|------|-----|------|
| `service.name` | String | サービス名 |
| `service.version` | String | サービスバージョン |
| `deployment.environment` | String | 環境名 |

### HTTP属性

| 属性 | 型 | 説明 |
|------|-----|------|
| `http.method` | String | HTTPメソッド |
| `http.url` | String | リクエストURL |
| `http.status_code` | Int | HTTPステータスコード |
| `http.request_content_length` | Int | リクエストボディサイズ |
| `http.response_content_length` | Int | レスポンスボディサイズ |

### ドメイン固有属性

| 属性 | 型 | 説明 |
|------|-----|------|
| `booking.id` | String | 予約ID |
| `booking.resource_id` | String | リソースID |
| `booking.status` | String | 予約ステータス |
| `payment.id` | String | 支払いID |
| `payment.amount` | Int | 支払い金額 |
| `payment.currency` | String | 通貨コード |
| `user.id` | String | ユーザーID（PIIではない） |

## 3.4 サンプリング戦略

### 基本方針

| 環境 | サンプリング率 | 理由 |
|------|---------------|------|
| Production | 10% | コスト削減、ストレージ節約 |
| Staging | 100% | 完全なデバッグ |
| Development | 100% | 完全なデバッグ |

### 優先サンプリング

以下の条件では100%サンプリングする：

- エラー発生時（`error=true`）
- SLO違反時（レイテンシ閾値超過）
- 特定の操作（支払い処理、キャンセル処理）

```java
// Tail-based sampling設定例
Sampler sampler = Sampler.parentBased(
    Sampler.traceIdRatioBased(0.1), // 基本10%
    Sampler.alwaysOn(),             // エラー時は100%
    Sampler.alwaysOn()              // リモート親がサンプリング済みなら継続
);
```

---

# 4. SLI/SLO定義

## 4.1 サービスレベル指標（SLI）

### 可用性

```
SLI = (成功レスポンス数) / (総リクエスト数)

成功レスポンス: HTTP 2xx, 4xx（クライアントエラーは成功扱い）
失敗レスポンス: HTTP 5xx
```

### レイテンシ

```
SLI = (閾値以内のレスポンス数) / (総リクエスト数)

閾値: p99 < 300ms
```

### エラー率

```
SLI = (5xxエラー数) / (総リクエスト数)
```

## 4.2 サービスレベル目標（SLO）

| サービス | SLI | SLO | 測定期間 |
|----------|-----|-----|----------|
| Booking API | 可用性 | 99.9% | 30日間 |
| Booking API | レイテンシ（p99） | < 300ms | 30日間 |
| Booking API | エラー率 | < 0.1% | 30日間 |
| Payment API | 可用性 | 99.9% | 30日間 |
| Payment API | レイテンシ（p99） | < 500ms | 30日間 |
| Payment API | エラー率 | < 0.1% | 30日間 |
| IAM API | 可用性 | 99.95% | 30日間 |
| IAM API | レイテンシ（p99） | < 200ms | 30日間 |

## 4.3 エラーバジェット

```
エラーバジェット = 1 - SLO

例：SLO 99.9% の場合
エラーバジェット = 0.1%
30日間で許容されるダウンタイム = 30日 × 24時間 × 60分 × 0.1% ≈ 43分
```

### バジェット消費時のアクション

| 消費率 | アクション |
|--------|----------|
| < 50% | 通常運用 |
| 50-80% | 新機能リリース抑制、安定性改善優先 |
| > 80% | 緊急対応、全変更凍結 |
| 100% | インシデント対応モード |

---

# 5. 相関IDの伝播

## 5.1 HTTPヘッダー

| ヘッダー | 用途 |
|---------|------|
| `X-Request-Id` | リクエストID（APIゲートウェイで生成） |
| `X-Correlation-Id` | 相関ID（ビジネスフロー追跡） |
| `traceparent` | W3C Trace Context（OpenTelemetry） |
| `tracestate` | W3C Trace Context State |

## 5.2 メッセージング（Kafka/RabbitMQ）

```java
// イベント発行時
message.setHeader("traceparent", currentTraceContext());
message.setHeader("X-Correlation-Id", correlationId);

// イベント受信時
Context extractedContext = propagator.extract(message.getHeaders());
try (Scope scope = extractedContext.makeCurrent()) {
    // 処理
}
```

---

# 6. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| JSON構造化ログ | ELK Stack、CloudWatch Logs等での検索効率 | 業界標準 |
| REDメトリクス | Google SRE本、Weave WorksのREDメソッド | 広く採用 |
| OpenTelemetry | CNCFプロジェクト、ベンダー中立 | 業界標準化 |
| W3C Trace Context | 分散トレースの相互運用性 | W3C勧告 |
| SLO 99.9% | 一般的なSaaSの目標値 | ビジネス要件により調整 |

---

# 7. 未決事項

| 項目 | 内容 | 優先度 |
|------|------|--------|
| ログ保持期間 | 本番ログの保持期間（30日? 90日?） | 中 |
| トレース保持期間 | トレースデータの保持期間 | 中 |
| アラート閾値 | 具体的なアラート閾値の設定 | 高（OBS-11で対応） |
| ダッシュボード | Grafanaダッシュボードの設計 | 中（OBS-12で対応） |
| コスト最適化 | ログ/メトリクス/トレースのコスト見積もり | 低 |
