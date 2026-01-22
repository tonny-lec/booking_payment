---
doc_type: "adr"
id: "0007"
title: "冪等キー戦略の採用"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team"]
---

# ADR-007: 冪等キー戦略の採用

## ステータス

Accepted

## コンテキスト

決済処理は以下の理由から重複実行を防止する必要がある：

- ネットワーク障害によるクライアントのリトライ
- ユーザーの重複クリック（ダブルサブミット）
- システム障害からの復旧時の再実行

重複実行は二重課金という致命的な問題を引き起こすため、確実な冪等性の保証が必要である。

### 検討すべき要件

1. **安全性**: 二重課金を100%防止
2. **使いやすさ**: クライアント実装が容易
3. **パフォーマンス**: 冪等チェックのオーバーヘッド最小化
4. **運用性**: 期限切れキーのクリーンアップ

## 決定

**Idempotency-Keyヘッダー方式を採用する。**

クライアントがリクエストごとに一意のキーを生成し、サーバーが同一キーの重複リクエストを検出・処理する。

### 採用する構成

| 項目 | 決定事項 |
|------|----------|
| キー形式 | UUID v4（クライアント生成） |
| キー送信方法 | `Idempotency-Key` HTTPヘッダー |
| 有効期限 | 24時間 |
| 重複検出 | リクエストハッシュで同一性判定 |
| 重複時の動作 | 保存済みレスポンスを返却 |

### シーケンス

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │                    │ Payment  │                    │    DB    │
└────┬─────┘                    └────┬─────┘                    └────┬─────┘
     │                               │                               │
     │ POST /payments                │                               │
     │ Idempotency-Key: abc-123      │                               │
     │ {bookingId, amount, ...}      │                               │
     ├──────────────────────────────>│                               │
     │                               │                               │
     │                               │ SELECT FROM idempotency_records │
     │                               │ WHERE key = 'abc-123'         │
     │                               ├──────────────────────────────>│
     │                               │                               │
     │                               │ NOT FOUND                     │
     │                               │<──────────────────────────────┤
     │                               │                               │
     │                               │ 決済処理実行                   │
     │                               │                               │
     │                               │ INSERT idempotency_record     │
     │                               │ {key, hash, response}         │
     │                               ├──────────────────────────────>│
     │                               │                               │
     │ 201 Created                   │                               │
     │ {paymentId, ...}              │                               │
     │<──────────────────────────────┤                               │
     │                               │                               │
     │ === リトライ（同一キー） ===   │                               │
     │                               │                               │
     │ POST /payments                │                               │
     │ Idempotency-Key: abc-123      │                               │
     ├──────────────────────────────>│                               │
     │                               │ SELECT FROM idempotency_records │
     │                               ├──────────────────────────────>│
     │                               │ FOUND {hash, response}        │
     │                               │<──────────────────────────────┤
     │                               │                               │
     │                               │ ハッシュ比較 → 一致           │
     │                               │                               │
     │ 200 OK (保存済みレスポンス)   │                               │
     │ {paymentId, ...}              │                               │
     │<──────────────────────────────┤                               │
```

## 検討した選択肢

### 選択肢1: データベースユニーク制約のみ（不採用）

bookingIdにユニーク制約を設定。

**メリット:**
- 実装がシンプル
- クライアント側の対応不要

**デメリット:**
- 同一予約への再決済（失敗後のリトライ）ができない
- 正常なリトライが409 Conflictになる

### 選択肢2: Idempotency-Key（サーバー生成）（不採用）

最初のリクエストでサーバーがキーを発行し、クライアントが後続リクエストで使用。

**メリット:**
- キー形式をサーバーが制御

**デメリット:**
- 2往復必要（キー取得→実行）
- レイテンシ増加

### 選択肢3: Idempotency-Key（クライアント生成）（採用）

クライアントがUUID v4でキーを生成。

**メリット:**
- 1往復で完結
- 業界標準（Stripe, PayPal等）
- クライアントが再送制御を持つ

**デメリット:**
- クライアントの正しい実装に依存
- キー衝突の理論的可能性（UUID v4で実質ゼロ）

### 選択肢4: リクエストボディのハッシュのみ（不採用）

リクエスト内容のハッシュで重複判定。

**メリット:**
- クライアント側の対応不要

**デメリット:**
- 意図的な再送と偶発的な重複が区別できない
- 時間差のある同一リクエストを検出困難

## 結果

### 正の影響

1. **二重課金防止**: 同一キーは確実に1回のみ処理
2. **業界標準準拠**: Stripe等の主要決済APIと同じパターン
3. **リトライ安全性**: クライアントは安全にリトライ可能
4. **デバッグ容易性**: キーでリクエストを追跡可能

### 負の影響

1. **クライアント実装負荷**: キー生成・管理が必要
   - **緩和策**: SDKやドキュメントで実装例を提供
2. **ストレージコスト**: 冪等レコードの保存
   - **緩和策**: 24時間後に自動削除
3. **レースコンディション**: 並行リクエストでの競合
   - **緩和策**: DBロックで先着1件のみ処理

### 実装詳細

#### リクエストハッシュの計算

```java
public class IdempotencyService {
    public String calculateRequestHash(CreatePaymentRequest request) {
        String content = String.format("%s:%s:%s",
            request.getBookingId(),
            request.getAmount(),
            request.getCurrency()
        );
        return DigestUtils.sha256Hex(content);
    }
}
```

#### 冪等性チェックフロー

```java
@Service
public class IdempotencyService {

    @Transactional
    public <T> T executeIdempotent(
        UUID idempotencyKey,
        String requestHash,
        Supplier<T> operation,
        Class<T> responseType
    ) {
        // 1. 既存レコードをチェック（ロック付き）
        Optional<IdempotencyRecord> existing = repository
            .findByKeyForUpdate(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            // 2. ハッシュ比較
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                    "Request content mismatch for idempotency key"
                );
            }

            // 3. 保存済みレスポンスを返却
            return objectMapper.readValue(
                record.getResponseBody(),
                responseType
            );
        }

        // 4. 新規実行
        T result = operation.get();

        // 5. レコード保存
        repository.save(new IdempotencyRecord(
            idempotencyKey,
            requestHash,
            objectMapper.writeValueAsString(result),
            Instant.now().plus(Duration.ofHours(24))
        ));

        return result;
    }
}
```

#### HTTPレスポンス

| シナリオ | ステータス | 説明 |
|----------|------------|------|
| 初回リクエスト成功 | 201 Created | 新規作成 |
| 同一キー・同一内容 | 200 OK | 保存済みレスポンス |
| 同一キー・異なる内容 | 409 Conflict | ハッシュ不一致 |
| キー未指定 | 400 Bad Request | ヘッダー必須 |
| 期限切れキー再利用 | 201 Created | 新規として処理 |

### クリーンアップジョブ

```sql
-- 24時間以上経過した冪等レコードを削除
DELETE FROM idempotency_records
WHERE expires_at < NOW();
```

実行頻度：1時間ごと（バッチジョブ）

## 関連決定

- ADR-008: 外部決済ゲートウェイの抽象化（ACL）
- ADR-009: 支払いステータス遷移の設計
- ADR-010: タイムアウト時の状態管理

## 参考資料

- Stripe: Idempotent Requests
- PayPal: Idempotency
- IETF: The Idempotency-Key HTTP Header Field (draft)
