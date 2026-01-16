# 未完了タスク: OpenAPI仕様

## 概要
- 対象フォルダ: `docs/api/openapi/`
- 状態: 4ファイルが空（paths: {}）、3ファイルは完了済み
- 優先度: **中**

---

## 完了済み
- [x] `iam.yaml` - Slice A（login/refresh/logout）
- [x] `booking.yaml` - Slice A（CRUD + 衝突検出）
- [x] `payment.yaml` - Slice A（create/capture/refund + 冪等性）

---

## タスク一覧

### API-1: Audit API
- ファイル: `docs/api/openapi/audit.yaml`
- Slice: B
- 優先度: 中
- 現状: `paths: {}` （空）
- 必要なエンドポイント:
  - [ ] GET /audit-logs - 監査ログ一覧取得
  - [ ] GET /audit-logs/{id} - 監査ログ詳細取得
- 必要なスキーマ:
  - [ ] AuditLog
  - [ ] AuditLogListResponse
  - [ ] ProblemDetail（共通）
- セキュリティ:
  - [ ] Bearer認証
  - [ ] 管理者権限のみ閲覧可能

### API-2: Notification API
- ファイル: `docs/api/openapi/notification.yaml`
- Slice: B
- 優先度: 中
- 現状: `paths: {}` （空）
- 必要なエンドポイント:
  - [ ] POST /notifications - 通知送信（内部用）
  - [ ] GET /notifications - 通知履歴一覧
  - [ ] GET /notifications/{id} - 通知詳細
- 必要なスキーマ:
  - [ ] Notification
  - [ ] NotificationStatus
  - [ ] NotificationListResponse
  - [ ] ProblemDetail（共通）
- セキュリティ:
  - [ ] Bearer認証
  - [ ] 所有者のみ閲覧可能

### API-3: Gateway API
- ファイル: `docs/api/openapi/gateway.yaml`
- Slice: 共通
- 優先度: 低
- 現状: `paths: {}` （空）
- 必要なエンドポイント:
  - [ ] API Gatewayルーティング定義（検討中）
- 備考: 各サービスAPIの集約ポイント、必要性を要検討

### API-4: Ledger API（未作成）
- ファイル: `docs/api/openapi/ledger.yaml`（新規作成が必要）
- Slice: D
- 優先度: 低
- 必要なエンドポイント:
  - [ ] GET /ledger/entries - 台帳エントリ一覧
  - [ ] GET /ledger/balance - 残高照会
- 備考: Slice D対象、現時点では作成不要

---

## 完了条件（DoD）
- paths が空でない
- request/response schema が定義されている
- エラー設計（RFC 7807 ProblemDetail）
- Security Scheme（Bearer JWT）
- 互換性の方針がコメントに記載
