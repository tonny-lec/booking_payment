# 未完了タスク: テスト計画・観測性

## 概要
- 対象ファイル: `docs/test/test-plan.md`, `docs/design/observability.md`
- 状態: スケルトン/部分完了
- 優先度: **高**（Slice A検証基準）

---

## タスク一覧

### TEST-1: テスト計画の具体化
- ファイル: `docs/test/test-plan.md`
- Slice: A
- 優先度: 高
- 現状: 重点項目のみ記載、詳細テストケース未記載
- 未完了セクション:
  - [ ] Unit Testの具体的テストケース
    - IAM: PasswordValidator, TokenGenerator, User集約
    - Booking: TimeRange, Booking集約, ConflictDetector
    - Payment: Money, Payment集約, IdempotencyKey
  - [ ] Integration Testの具体的テストケース
    - Repository層テスト
    - UseCase層テスト
  - [ ] Contract Testの設計
    - OpenAPIに対する契約テスト
    - リクエスト/レスポンス形式検証
  - [ ] E2E Testシナリオ
    - 認証フロー（login → refresh → logout）
    - 予約フロー（create → update → cancel）
    - 支払いフロー（create → capture → refund）
    - 冪等性検証
  - [ ] 境界値テスト一覧
    - TimeRange境界（隣接予約）
    - 金額境界（最小/最大）
    - Idempotency-Key有効期限
  - [ ] 権限テスト一覧
    - 所有者以外のアクセス拒否
    - 無効トークンでのアクセス拒否
  - [ ] 互換性テスト（v1/v2併走）

### OBS-1: 観測性設計の具体化
- ファイル: `docs/design/observability.md`
- Slice: A
- 優先度: 高
- 現状: 要件概要のみ
- 未完了セクション:
  - [ ] ログ設計
    - ログフォーマット（JSON構造化ログ）
    - 必須フィールド（traceId, spanId, timestamp, level, message）
    - PIIマスキングルール
  - [ ] メトリクス設計
    - REDメトリクス（Rate, Errors, Duration）
    - 各サービス固有メトリクス
    - Prometheus形式のメトリクス名
  - [ ] トレース設計
    - OpenTelemetry設定
    - Span命名規則
    - 必須属性一覧
    - サンプリング戦略
  - [ ] アラート設計
    - SLI/SLO定義
    - アラート閾値
    - エスカレーションルール
  - [ ] ダッシュボード設計
    - 主要KPI
    - サービス別ダッシュボード

---

## 完了条件（DoD）

### test-plan.md
- 境界/冪等/権限/互換性テストが含まれる
- 各テストレベル（Unit/Integration/Contract/E2E）のケースが具体的
- Slice Aのusecaseをカバー

### observability.md
- 必須属性/メトリクス/トレースが具体化されている
- PIIポリシーと整合
- SLI/SLO定義がある
