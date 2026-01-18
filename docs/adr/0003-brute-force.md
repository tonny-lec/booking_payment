---
doc_type: "adr"
id: "0003"
title: "Brute-force対策の閾値設定"
status: "accepted"
date: "2026-01-18"
deciders: ["Architecture Team", "Security Team"]
---

# ADR-003: Brute-force対策の閾値設定

## ステータス

Accepted

## コンテキスト

認証システムはBrute-force攻撃（総当たり攻撃）の標的となりやすい。攻撃者はパスワードリストや辞書攻撃を用いて、有効な認証情報を取得しようとする。

一方で、過度に厳しい制限は正当なユーザーの利便性を損なう。セキュリティとユーザビリティのバランスを取った閾値設定が必要である。

### 検討すべき要件

1. **セキュリティ**: 自動化された攻撃を効果的に阻止
2. **ユーザビリティ**: 正当なユーザーへの影響を最小化
3. **可観測性**: 攻撃の検知とモニタリング
4. **回復性**: ロックからの復旧手段

## 決定

**多層防御アプローチを採用し、以下の閾値を設定する。**

### 採用する構成

```
┌─────────────────────────────────────────────────────────────┐
│                    多層防御アーキテクチャ                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Layer 1: IP Rate Limiting (WAF/API Gateway)                 │
│  └─ 10 req/min/IP for /auth/login                           │
│                                                              │
│  Layer 2: Account Rate Limiting (Application)                │
│  └─ 5 req/min per account                                    │
│                                                              │
│  Layer 3: Account Lockout (Application)                      │
│  └─ 5 consecutive failures → 15min lock                      │
│                                                              │
│  Layer 4: Progressive Delay (Application)                    │
│  └─ Increasing response delay after failures                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 閾値一覧

| レイヤー | 項目 | 閾値 | 根拠 |
|----------|------|------|------|
| Layer 1 | IP Rate Limit | 10 req/min | 正当なユーザーは1分に10回もログインしない |
| Layer 2 | Account Rate Limit | 5 req/min | 同上 |
| Layer 3 | 連続失敗閾値 | 5回 | NIST SP 800-63B推奨範囲 |
| Layer 3 | 初回ロック期間 | 15分 | ユーザビリティとセキュリティのバランス |
| Layer 3 | 最大ロック期間 | 24時間 | 累進的ロックの上限 |
| Layer 4 | 遅延開始 | 3回目から | 2回までは遅延なし |

## 検討した選択肢

### 選択肢1: Rate Limitingのみ（不採用）

IPベースまたはアカウントベースのレート制限のみを実装。

```
攻撃者 ─── Rate Limit ───> 429 Too Many Requests
```

**メリット:**
- 実装がシンプル
- サーバー負荷の軽減

**デメリット:**
- 分散攻撃（複数IP）に対して脆弱
- アカウント特定攻撃に対応困難

### 選択肢2: Account Lockoutのみ（不採用）

連続失敗回数に基づくアカウントロック。

**メリット:**
- アカウント特定攻撃に有効

**デメリット:**
- DoS攻撃として悪用可能（他人のアカウントをロック）
- Rate Limitingがないとサーバー負荷が高い

### 選択肢3: CAPTCHA導入（部分採用・将来）

一定回数失敗後にCAPTCHAを要求。

**メリット:**
- 自動化攻撃の阻止に効果的
- ロックなしで防御可能

**デメリット:**
- ユーザー体験の低下
- アクセシビリティの問題
- CAPTCHA解決サービスによる回避

**結論:** Phase 2で検討。現時点ではLayer 1-4で対応。

### 選択肢4: 多層防御（採用）

複数の防御レイヤーを組み合わせる。

**メリット:**
- 単一の対策の弱点を相互補完
- 段階的なエスカレーション
- 攻撃パターンに応じた対応

**デメリット:**
- 実装・運用の複雑さ

## 結果

### 正の影響

1. **効果的な攻撃阻止**: 複数レイヤーで攻撃を段階的にブロック
2. **ユーザビリティ維持**: 正当なユーザーは通常影響を受けない
3. **可観測性**: 各レイヤーでメトリクスを取得可能
4. **柔軟性**: 状況に応じて閾値を調整可能

### 負の影響

1. **実装複雑性**: 4つのレイヤーの実装と統合
   - **緩和策**: 各レイヤーを独立したコンポーネントとして設計
2. **設定管理**: 複数の閾値を管理する必要
   - **緩和策**: 設定ファイルで一元管理
3. **誤検知リスク**: 正当なユーザーがロックされる可能性
   - **緩和策**: 自己解除機能とサポートフロー

### 詳細設計

#### Layer 3: Account Lockout（累進的ロック）

```java
public class AccountLockoutPolicy {
    // ロック期間の累進計算
    // lockoutCount: 1 → 15min
    // lockoutCount: 2 → 30min
    // lockoutCount: 3 → 60min
    // lockoutCount: 4 → 120min
    // lockoutCount: 5+ → 24h (上限)

    public Duration calculateLockoutDuration(int lockoutCount) {
        long baseMinutes = 15;
        long minutes = baseMinutes * (long) Math.pow(2, lockoutCount - 1);
        return Duration.ofMinutes(Math.min(minutes, 24 * 60));
    }
}
```

#### Layer 4: Progressive Delay

| 連続失敗回数 | 追加遅延 | 累積最大遅延 |
|--------------|----------|--------------|
| 1-2回 | 0秒 | 0秒 |
| 3回 | 1秒 | 1秒 |
| 4回 | 2秒 | 3秒 |
| 5回 | → ロックへ移行 | - |

```java
public class ProgressiveDelayPolicy {
    public Duration calculateDelay(int failedAttempts) {
        if (failedAttempts <= 2) {
            return Duration.ZERO;
        }
        int delaySeconds = failedAttempts - 2; // 3回目→1秒, 4回目→2秒
        return Duration.ofSeconds(delaySeconds);
    }
}
```

### モニタリングとアラート

| メトリクス | アラート閾値 | アクション |
|------------|--------------|------------|
| `iam_login_failed_total` | >100/min | Slack通知 |
| `iam_account_locked_total` | >10/min | Slack通知 + オンコール |
| `iam_rate_limited_total` | >50/min | ダッシュボード確認 |
| 単一IPからの失敗 | >50/hour | WAFでIPブロック検討 |

### ユーザー復旧フロー

```
┌────────────────────────────────────────────────────────────┐
│                    アカウントロック復旧                     │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 自動解除（時間経過）                                    │
│     └─ ロック期間終了後、自動的に解除                       │
│                                                             │
│  2. パスワードリセット                                      │
│     └─ メール経由でパスワードリセット→ロック解除            │
│                                                             │
│  3. サポート問い合わせ                                      │
│     └─ 本人確認後、管理者が手動解除                         │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

## 関連決定

- ADR-001: JWT認証方式の採用
- ADR-002: RefreshTokenローテーション戦略

## 参考資料

- NIST SP 800-63B: Digital Identity Guidelines (Authentication)
- OWASP Authentication Cheat Sheet
- OWASP Blocking Brute Force Attacks
