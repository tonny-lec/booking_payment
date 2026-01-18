---
doc_type: "context"
id: "audit"
bounded_context: "Audit"
version: "1.0"
last_updated: "2026-01-18"
status: "draft"
---

# 1. 目的

Auditコンテキストは、**システム全体の操作履歴を記録し、監査証跡を提供**する。

## 責務

- 各コンテキストからのドメインイベントを購読し、監査ログを記録
- 監査ログの改ざん防止（追記専用、削除不可）
- 監査ログの検索・閲覧機能の提供
- 法的要件に基づく保持期間の管理
- コンプライアンス監査のためのエクスポート機能

## スコープ外

- アプリケーションログ（エラーログ、デバッグログ）：別システム
- メトリクス・トレース：Observabilityシステムが担当
- リアルタイムアラート：監視システムが担当

---

# 2. 用語

- SSOT：`docs/domain/glossary.md`
- 主要用語：
  - **AuditLog**：監査ログエントリを表す集約（追記専用）
  - **Actor**：操作を実行した主体（ユーザー、システム）
  - **Action**：実行された操作（CREATE, UPDATE, DELETE, READ等）
  - **Resource**：操作対象のリソース（予約、支払い等）
  - **AuditTrail**：関連する監査ログの時系列的な追跡

---

# 3. 集約一覧（Aggregate Catalog）

## 3.1 AuditLog（集約ルート、追記専用）

```
AuditLog (Aggregate Root, Append-Only) {
  id: AuditLogId (UUID)
  eventId: UUID (元イベントのID)
  eventType: String (イベント種別)
  occurredAt: DateTime (イベント発生日時)
  recordedAt: DateTime (記録日時)

  // Actor情報
  actorId: UUID? (ユーザーID、システムの場合はnull)
  actorType: ActorType (USER | SYSTEM | ADMIN)
  actorIp: String? (IPアドレス、マスク済み)

  // Action情報
  action: Action (CREATE | UPDATE | DELETE | READ | LOGIN | LOGOUT | etc.)
  actionCategory: ActionCategory (DATA | AUTH | CONFIG | ADMIN)

  // Resource情報
  resourceType: String (Booking, Payment, User, etc.)
  resourceId: UUID
  resourceContext: String (bounded_context名)

  // 変更詳細
  changes: Map<String, ChangeDetail>? (変更前後の値)
  metadata: Map<String, String> (追加情報)

  // 整合性
  checksum: String (SHA-256ハッシュ)
  previousLogId: AuditLogId? (連鎖ハッシュ用)
}
```

### 不変条件

1. **追記専用**：作成後の更新・削除は不可
2. `eventId` は一意（重複イベントの防止）
3. `checksum` は内容から計算され、改ざん検知に使用
4. `occurredAt <= recordedAt`（記録は発生以降）

### 振る舞い

- `record(event)`: イベントから監査ログを作成（作成のみ、更新なし）
- `verify()`: checksumを検証し、改ざんを検知

## 3.2 ChangeDetail（値オブジェクト）

```
ChangeDetail {
  field: String (変更フィールド名)
  oldValue: String? (変更前、マスク済み)
  newValue: String? (変更後、マスク済み)
  changeType: ChangeType (ADDED | MODIFIED | REMOVED)
}
```

### PIIマスキングルール

| フィールド | マスキング例 |
|------------|-------------|
| email | u***@example.com |
| phone | ***-****-1234 |
| ip_address | 192.168.***.*** |
| name | 田*** |

---

# 4. Context Map

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│       IAM       │     │     Booking     │     │     Payment     │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │ UserLoggedIn          │ BookingCreated        │ PaymentCreated
         │ LoginFailed           │ BookingUpdated        │ PaymentCaptured
         │ AccountLocked         │ BookingCancelled      │ PaymentRefunded
         │ RefreshTokenRevoked   │ BookingConfirmed      │ PaymentFailed
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────────────────┐
                    │              Audit                   │
                    │                                      │
                    │  ・イベント購読                       │
                    │  ・監査ログ記録                       │
                    │  ・改ざん検知                        │
                    │  ・検索・閲覧                        │
                    └────────┬────────────────────────────┘
                             │
                             │ AuditLogRecorded
                             ▼
                    ┌─────────────────┐
                    │   Storage       │
                    │   (PostgreSQL)  │
                    │   追記専用      │
                    └─────────────────┘

         ┌─────────────────┐
         │  Notification   │
         └────────┬────────┘
                  │
                  │ NotificationSent
                  │ NotificationFailed
                  │
                  └──────────────────────────────┐
                                                 ▼
                                        ┌───────────────┐
                                        │     Audit     │
                                        └───────────────┘
```

## 関係性

| 関係 | 種別 | 説明 |
|------|------|------|
| IAM → Audit | Publisher-Subscriber | 認証イベントを購読して記録 |
| Booking → Audit | Publisher-Subscriber | 予約イベントを購読して記録 |
| Payment → Audit | Publisher-Subscriber | 決済イベントを購読して記録 |
| Notification → Audit | Publisher-Subscriber | 通知イベントを購読して記録 |

## 統合パターン

- **イベント購読**：
  - 各コンテキストからのドメインイベントをメッセージキュー経由で受信
  - イベントを標準化された監査ログ形式に変換
  - 重複イベント検知（eventIdでの重複排除）

- **ログ記録の保証**：
  - At-least-once配信を前提とした冪等な記録
  - 記録失敗時はデッドレターキューへ退避

---

# 5. 永続化

## 5.1 audit_logs テーブル（追記専用）

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | UUID | PK | 監査ログID |
| event_id | UUID | UNIQUE, NOT NULL | 元イベントID |
| event_type | VARCHAR(100) | NOT NULL | イベント種別 |
| occurred_at | TIMESTAMP | NOT NULL | イベント発生日時 |
| recorded_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 記録日時 |
| actor_id | UUID | NULL | 実行者ID |
| actor_type | VARCHAR(20) | NOT NULL | USER/SYSTEM/ADMIN |
| actor_ip | VARCHAR(45) | NULL | IPアドレス（マスク済み） |
| action | VARCHAR(50) | NOT NULL | 操作種別 |
| action_category | VARCHAR(20) | NOT NULL | 操作カテゴリ |
| resource_type | VARCHAR(50) | NOT NULL | リソース種別 |
| resource_id | UUID | NOT NULL | リソースID |
| resource_context | VARCHAR(50) | NOT NULL | コンテキスト名 |
| changes | JSONB | NULL | 変更詳細 |
| metadata | JSONB | NULL | メタデータ |
| checksum | VARCHAR(64) | NOT NULL | SHA-256ハッシュ |
| previous_log_id | UUID | NULL | 前のログID（連鎖用） |

**インデックス：**
- `idx_audit_logs_event_id` ON audit_logs(event_id) - 重複チェック
- `idx_audit_logs_occurred_at` ON audit_logs(occurred_at) - 時系列検索
- `idx_audit_logs_actor_id` ON audit_logs(actor_id) WHERE actor_id IS NOT NULL - ユーザー別検索
- `idx_audit_logs_resource` ON audit_logs(resource_type, resource_id) - リソース別検索
- `idx_audit_logs_action` ON audit_logs(action, occurred_at) - 操作別検索

**制約：**
- `CHECK (actor_type IN ('USER', 'SYSTEM', 'ADMIN'))`
- `CHECK (action_category IN ('DATA', 'AUTH', 'CONFIG', 'ADMIN'))`

**追記専用の強制（PostgreSQL）：**

```sql
-- 更新を禁止するトリガー
CREATE OR REPLACE FUNCTION prevent_audit_update()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'Audit logs are immutable. Updates are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_update();

-- 削除を禁止するトリガー
CREATE OR REPLACE FUNCTION prevent_audit_delete()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'Audit logs are immutable. Deletions are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_delete
BEFORE DELETE ON audit_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_delete();
```

## 5.2 パーティショニング（大規模対応）

```sql
-- 月次パーティション
CREATE TABLE audit_logs (
  ...
) PARTITION BY RANGE (occurred_at);

CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs
  FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE audit_logs_2026_02 PARTITION OF audit_logs
  FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
```

---

# 6. ドメインイベント

## 6.1 AuditLogRecorded

監査ログが記録されたときに発行（主に監視用）。

```
AuditLogRecorded {
  eventId: UUID
  aggregateId: AuditLogId
  occurredAt: DateTime
  payload: {
    auditLogId: UUID
    eventType: String
    actorId: UUID?
    actorType: String
    action: String
    resourceType: String
    resourceId: UUID
    recordedAt: DateTime
  }
}
```

**購読者：** 監視システム（異常検知）

---

# 7. 非機能（SLO/Obs/Sec）

## 7.1 SLO（Service Level Objectives）

| SLI | 目標値 | 測定方法 |
|-----|--------|----------|
| 記録成功率 | 99.99% | 受信イベント数 / 記録成功数 |
| 記録レイテンシ（p99） | < 1秒 | イベント受信から記録完了まで |
| 可用性 | 99.9% | 検索API成功率 |
| データ整合性 | 100% | checksum検証成功率 |

## 7.2 Observability

- **詳細**：`docs/design/observability.md`
- **主要メトリクス**：
  - `audit_log_recorded_total{event_type, resource_type}` - 記録数
  - `audit_log_record_duration_seconds` - 記録処理時間
  - `audit_log_duplicate_total` - 重複イベント検知数
  - `audit_log_checksum_failure_total` - 改ざん検知数
  - `audit_log_storage_size_bytes` - ストレージ使用量
  - `audit_log_query_duration_seconds{query_type}` - 検索処理時間

## 7.3 Security

- **詳細**：`docs/design/security.md`
- **主要対策**：
  - **改ざん防止**：追記専用テーブル、checksumによる検証
  - **アクセス制御**：管理者のみ閲覧可能
  - **PII保護**：マスキング処理必須
  - **暗号化**：保存時暗号化（TDE）
  - **監査の監査**：管理者のログ閲覧も記録

---

# 8. 改ざん検知

## 8.1 Checksum計算

```java
public class AuditLogChecksumCalculator {

    public String calculate(AuditLog log) {
        String content = String.join("|",
            log.getEventId().toString(),
            log.getEventType(),
            log.getOccurredAt().toString(),
            log.getActorId() != null ? log.getActorId().toString() : "",
            log.getActorType().name(),
            log.getAction().name(),
            log.getResourceType(),
            log.getResourceId().toString(),
            log.getPreviousLogId() != null ? log.getPreviousLogId().toString() : "",
            objectMapper.writeValueAsString(log.getChanges()),
            objectMapper.writeValueAsString(log.getMetadata())
        );
        return DigestUtils.sha256Hex(content);
    }

    public boolean verify(AuditLog log) {
        String calculated = calculate(log);
        return calculated.equals(log.getChecksum());
    }
}
```

## 8.2 連鎖ハッシュ（Chain Hash）

```
Log[n].checksum = SHA256(Log[n].content + Log[n-1].checksum)
```

連鎖ハッシュにより、中間のログが改ざんされた場合、以降のすべてのchecksumが無効になる。

## 8.3 定期検証ジョブ

```java
@Scheduled(cron = "0 0 3 * * *")  // 毎日3:00
public void verifyIntegrity() {
    List<AuditLog> logs = auditLogRepository.findByRecordedAtAfter(
        Instant.now().minus(Duration.ofDays(1))
    );

    for (AuditLog log : logs) {
        if (!checksumCalculator.verify(log)) {
            alertService.sendAlert(
                AlertLevel.CRITICAL,
                "Audit log integrity check failed",
                Map.of("auditLogId", log.getId())
            );
        }
    }
}
```

---

# 9. 保持期間とアーカイブ

## 9.1 保持ポリシー

| カテゴリ | 保持期間 | 根拠 |
|----------|----------|------|
| 認証ログ（AUTH） | 5年 | セキュリティ監査要件 |
| データ操作（DATA） | 7年 | 会計監査要件（推論） |
| 設定変更（CONFIG） | 3年 | 運用要件 |
| 管理操作（ADMIN） | 10年 | コンプライアンス要件 |

## 9.2 アーカイブフロー

```
┌─────────────────────────────────────────────────────────────────┐
│                     Archive Flow                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Active Table (PostgreSQL)                                       │
│  └─ 直近1年のログ                                                │
│                                                                  │
│      │                                                           │
│      │ 月次アーカイブジョブ                                       │
│      ▼                                                           │
│                                                                  │
│  Cold Storage (S3 Glacier等)                                     │
│  └─ 1年以上前のログ（圧縮・暗号化）                               │
│                                                                  │
│      │                                                           │
│      │ 保持期間経過後                                            │
│      ▼                                                           │
│                                                                  │
│  削除（保持期間に応じて）                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

# 10. ADRリンク

| ADR | タイトル | 状態 |
|-----|----------|------|
| ADR-013 | 監査ログの改ざん防止戦略 | 作成予定 |
| ADR-014 | 監査ログの保持期間とアーカイブ | 作成予定 |

---

# 11. Evidence（根拠）

| 項目 | 根拠 | 備考 |
|------|------|------|
| 追記専用テーブル | 監査ログの改ざん防止の標準パターン | 業界標準 |
| SHA-256 checksum | NIST推奨のハッシュアルゴリズム | セキュリティ標準 |
| 7年保持 | 会計監査の一般的要件 | 法的要件は要確認 |

---

# 12. 未決事項

| 項目 | 内容 | 優先度 | 担当 |
|------|------|--------|------|
| 法的保持期間 | 各国・業界の法的要件の確認 | 高 | 法務 |
| アーカイブストレージ | S3 Glacier vs Azure Archive等の選定 | 中 | インフラ |
| 検索機能 | 全文検索（Elasticsearch等）の導入 | 中 | Phase 2 |
| エクスポート形式 | CSV / JSON / PDF等の対応形式 | 低 | Phase 2 |
| ブロックチェーン連携 | 改ざん防止の強化（外部証跡） | 低 | Phase 3 |
