# 機能別タスク一覧

このドキュメントは、タスクを**機能（Bounded Context）ごと**に細分化したものです。

---

## 凡例

- **Slice**: A=最小MVP, B=E2E成立, C=互換性/運用, D=イベント駆動
- **優先度**: 🔴高 / 🟡中 / 🟢低
- **状態**: ⬜未着手 / 🔄進行中 / ✅完了

---

## 1. IAM（認証・認可）

### 1.1 コンテキスト設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| IAM-CTX-01 | 目的・責務の定義 | `docs/design/contexts/iam.md` | A | 🔴 | ✅ |
| IAM-CTX-02 | 集約一覧（User, RefreshToken）の定義 | 同上 | A | 🔴 | ✅ |
| IAM-CTX-03 | Context Map（他BCへの認証提供関係）の定義 | 同上 | A | 🔴 | ✅ |
| IAM-CTX-04 | 永続化設計（users, refresh_tokens テーブル） | 同上 | A | 🔴 | ✅ |
| IAM-CTX-05 | ドメインイベント定義（UserLoggedIn, LoginFailed） | 同上 | A | 🟡 | ✅ |
| IAM-CTX-06 | 非機能要件（SLO/制約）の記載 | 同上 | A | 🟡 | ✅ |

### 1.2 ユースケース設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| IAM-UC-01 | ログイン目的・背景の記載 | `docs/design/usecases/iam-login.md` | A | 🔴 | ✅ |
| IAM-UC-02 | 入出力定義（LoginRequest→TokenIssued） | 同上 | A | 🔴 | ✅ |
| IAM-UC-03 | 集約・不変条件の定義 | 同上 | A | 🔴 | ✅ |
| IAM-UC-04 | 失敗モード定義（invalid_credentials, account_locked） | 同上 | A | 🔴 | ✅ |
| IAM-UC-05 | 観測性設計（メトリクス、ログ） | 同上 | A | 🔴 | ✅ |
| IAM-UC-06 | セキュリティ設計（brute-force対策） | 同上 | A | 🔴 | ✅ |

### 1.3 OpenAPI仕様
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| IAM-API-01 | POST /auth/login エンドポイント定義 | `docs/api/openapi/iam.yaml` | A | 🔴 | ✅ |
| IAM-API-02 | POST /auth/refresh エンドポイント定義 | 同上 | A | 🔴 | ✅ |
| IAM-API-03 | POST /auth/logout エンドポイント定義 | 同上 | A | 🔴 | ✅ |
| IAM-API-04 | Security Scheme（Bearer JWT）定義 | 同上 | A | 🔴 | ✅ |
| IAM-API-05 | エラーレスポンス（RFC 7807）定義 | 同上 | A | 🔴 | ✅ |

### 1.4 セキュリティ
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| IAM-SEC-01 | JWT構造（header/payload/signature）設計 | `docs/design/security.md` | A | 🟡 | ⬜ |
| IAM-SEC-02 | トークン有効期限設計（Access: 15min, Refresh: 7d等） | 同上 | A | 🟡 | ⬜ |
| IAM-SEC-03 | 署名アルゴリズム選定（RS256推奨） | 同上 | A | 🟡 | ⬜ |
| IAM-SEC-04 | RefreshTokenローテーション方針 | 同上 | A | 🟡 | ⬜ |
| IAM-SEC-05 | Brute-force対策の閾値設定 | 同上 | A | 🔴 | ⬜ |

### 1.5 テスト計画
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| IAM-TEST-01 | Unit Test: PasswordValidator | `docs/test/test-plan.md` | A | 🔴 | ⬜ |
| IAM-TEST-02 | Unit Test: TokenGenerator | 同上 | A | 🔴 | ⬜ |
| IAM-TEST-03 | Unit Test: User集約 | 同上 | A | 🔴 | ⬜ |
| IAM-TEST-04 | Integration Test: UserRepository | 同上 | A | 🟡 | ⬜ |
| IAM-TEST-05 | E2E Test: login→refresh→logout フロー | 同上 | A | 🔴 | ⬜ |
| IAM-TEST-06 | 権限テスト: 無効トークンでのアクセス拒否 | 同上 | A | 🔴 | ⬜ |

### 1.6 ADR
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| IAM-ADR-01 | ADR-001: JWT認証方式の採用 | `docs/adr/0001-jwt-auth.md` | A | 🟡 | ⬜ |
| IAM-ADR-02 | ADR-002: RefreshTokenローテーション戦略 | `docs/adr/0002-refresh-rotation.md` | A | 🟡 | ⬜ |
| IAM-ADR-03 | ADR-003: Brute-force対策の閾値設定 | `docs/adr/0003-brute-force.md` | A | 🟡 | ⬜ |

---

## 2. Booking（予約管理）

### 2.1 コンテキスト設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-CTX-01 | 目的・責務の定義 | `docs/design/contexts/booking.md` | A | 🔴 | ✅ |
| BK-CTX-02 | 集約一覧（Booking, TimeRange）の定義 | 同上 | A | 🔴 | ✅ |
| BK-CTX-03 | Context Map（IAM認証、Payment連携）の定義 | 同上 | A | 🔴 | ✅ |
| BK-CTX-04 | 永続化設計（bookings テーブル） | 同上 | A | 🔴 | ✅ |
| BK-CTX-05 | ドメインイベント定義（BookingCreated, BookingCancelled） | 同上 | A | 🟡 | ✅ |
| BK-CTX-06 | 非機能要件（SLO/制約）の記載 | 同上 | A | 🟡 | ✅ |

### 2.2 ユースケース設計

#### 2.2.1 予約作成
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-UC-CREATE-01 | 目的・背景の記載 | `docs/design/usecases/booking-create.md` | A | 🔴 | ✅ |
| BK-UC-CREATE-02 | 入出力定義（CreateBookingRequest→BookingCreated） | 同上 | A | 🔴 | ✅ |
| BK-UC-CREATE-03 | 集約・不変条件（TimeRange重複禁止）の定義 | 同上 | A | 🔴 | ✅ |
| BK-UC-CREATE-04 | 失敗モード定義（conflict_409, validation_error） | 同上 | A | 🔴 | ✅ |
| BK-UC-CREATE-05 | 観測性設計（booking_created メトリクス） | 同上 | A | 🔴 | ✅ |
| BK-UC-CREATE-06 | セキュリティ設計（認可：user_id一致） | 同上 | A | 🔴 | ✅ |

#### 2.2.2 予約更新
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-UC-UPDATE-01 | 目的・背景の記載 | `docs/design/usecases/booking-update.md` | A | 🔴 | ✅ |
| BK-UC-UPDATE-02 | 入出力定義（UpdateBookingCommand→BookingUpdated） | 同上 | A | 🔴 | ✅ |
| BK-UC-UPDATE-03 | 楽観的ロック・TimeRange再検証の定義 | 同上 | A | 🔴 | ✅ |
| BK-UC-UPDATE-04 | 失敗モード定義（conflict_409, version_mismatch） | 同上 | A | 🔴 | ✅ |
| BK-UC-UPDATE-05 | 観測性設計 | 同上 | A | 🟡 | ✅ |
| BK-UC-UPDATE-06 | セキュリティ設計（所有者のみ変更可） | 同上 | A | 🔴 | ✅ |

#### 2.2.3 予約キャンセル
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-UC-CANCEL-01 | 目的・背景の記載 | `docs/design/usecases/booking-cancel.md` | A | 🔴 | ✅ |
| BK-UC-CANCEL-02 | 入出力定義（CancelBookingCommand→BookingCancelled） | 同上 | A | 🔴 | ✅ |
| BK-UC-CANCEL-03 | 状態遷移（CONFIRMED→CANCELLED）の定義 | 同上 | A | 🔴 | ✅ |
| BK-UC-CANCEL-04 | 失敗モード定義（already_cancelled, invalid_state） | 同上 | A | 🔴 | ✅ |
| BK-UC-CANCEL-05 | 返金トリガー連携の設計 | 同上 | A | 🔴 | ✅ |
| BK-UC-CANCEL-06 | 観測性設計 | 同上 | A | 🟡 | ✅ |
| BK-UC-CANCEL-07 | セキュリティ設計（所有者のみキャンセル可） | 同上 | A | 🔴 | ✅ |

### 2.3 OpenAPI仕様
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-API-01 | POST /bookings エンドポイント定義 | `docs/api/openapi/booking.yaml` | A | 🔴 | ✅ |
| BK-API-02 | GET /bookings/{id} エンドポイント定義 | 同上 | A | 🔴 | ✅ |
| BK-API-03 | PUT /bookings/{id} エンドポイント定義 | 同上 | A | 🔴 | ✅ |
| BK-API-04 | DELETE /bookings/{id} エンドポイント定義 | 同上 | A | 🔴 | ✅ |
| BK-API-05 | 409 Conflict レスポンス設計 | 同上 | A | 🔴 | ✅ |
| BK-API-06 | Booking/TimeRange/Error スキーマ定義 | 同上 | A | 🔴 | ✅ |

### 2.4 テスト計画
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-TEST-01 | Unit Test: TimeRange値オブジェクト | `docs/test/test-plan.md` | A | 🔴 | ⬜ |
| BK-TEST-02 | Unit Test: Booking集約 | 同上 | A | 🔴 | ⬜ |
| BK-TEST-03 | Unit Test: ConflictDetector | 同上 | A | 🔴 | ⬜ |
| BK-TEST-04 | Integration Test: BookingRepository | 同上 | A | 🟡 | ⬜ |
| BK-TEST-05 | 境界値テスト: TimeRange境界（隣接予約） | 同上 | A | 🔴 | ⬜ |
| BK-TEST-06 | E2E Test: create→update→cancel フロー | 同上 | A | 🔴 | ⬜ |
| BK-TEST-07 | 権限テスト: 所有者以外のアクセス拒否 | 同上 | A | 🔴 | ⬜ |

### 2.5 ADR
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| BK-ADR-01 | ADR-004: 予約衝突検出戦略 | `docs/adr/0004-conflict-detection.md` | A | 🟡 | ⬜ |
| BK-ADR-02 | ADR-005: 楽観的ロックの採用理由 | `docs/adr/0005-optimistic-lock.md` | A | 🟡 | ⬜ |
| BK-ADR-03 | ADR-006: 予約ステータス遷移の設計 | `docs/adr/0006-booking-status.md` | A | 🟡 | ⬜ |

---

## 3. Payment（決済）

### 3.1 コンテキスト設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-CTX-01 | 目的・責務の定義 | `docs/design/contexts/payment.md` | A | 🔴 | ✅ |
| PAY-CTX-02 | 集約一覧（Payment, Money, IdempotencyKey）の定義 | 同上 | A | 🔴 | ✅ |
| PAY-CTX-03 | Context Map（Booking連携、外部Gateway）の定義 | 同上 | A | 🔴 | ✅ |
| PAY-CTX-04 | 永続化設計（payments, idempotency_records テーブル） | 同上 | A | 🔴 | ✅ |
| PAY-CTX-05 | ドメインイベント定義（PaymentCreated, PaymentCaptured, PaymentFailed） | 同上 | A | 🟡 | ✅ |
| PAY-CTX-06 | 非機能要件（SLO/制約）の記載 | 同上 | A | 🟡 | ✅ |

### 3.2 ユースケース設計

#### 3.2.1 支払い作成
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-UC-CREATE-01 | 目的・背景の記載 | `docs/design/usecases/payment-create.md` | A | 🔴 | ✅ |
| PAY-UC-CREATE-02 | 入出力定義（CreatePaymentRequest→PaymentCreated） | 同上 | A | 🔴 | ✅ |
| PAY-UC-CREATE-03 | 集約・不変条件（金額正値、冪等キー一意）の定義 | 同上 | A | 🔴 | ✅ |
| PAY-UC-CREATE-04 | 失敗モード定義（idempotency_conflict, gateway_timeout） | 同上 | A | 🔴 | ✅ |
| PAY-UC-CREATE-05 | 観測性設計（payment_created メトリクス） | 同上 | A | 🔴 | ✅ |
| PAY-UC-CREATE-06 | セキュリティ設計（booking所有者一致、PII非出力） | 同上 | A | 🔴 | ✅ |

#### 3.2.2 支払いキャプチャ
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-UC-CAPTURE-01 | 目的・背景の記載 | `docs/design/usecases/payment-capture.md` | B | 🟡 | ⬜ |
| PAY-UC-CAPTURE-02 | 入出力定義（CapturePaymentCommand→PaymentCaptured） | 同上 | B | 🟡 | ⬜ |
| PAY-UC-CAPTURE-03 | 状態遷移（AUTHORIZED→CAPTURED）の定義 | 同上 | B | 🟡 | ⬜ |
| PAY-UC-CAPTURE-04 | 失敗モード定義（gateway_error, invalid_state） | 同上 | B | 🟡 | ⬜ |
| PAY-UC-CAPTURE-05 | 観測性設計 | 同上 | B | 🟡 | ⬜ |
| PAY-UC-CAPTURE-06 | セキュリティ設計 | 同上 | B | 🟡 | ⬜ |

#### 3.2.3 返金
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-UC-REFUND-01 | 目的・背景の記載 | `docs/design/usecases/payment-refund.md` | B | 🟡 | ⬜ |
| PAY-UC-REFUND-02 | 入出力定義（RefundPaymentCommand→PaymentRefunded） | 同上 | B | 🟡 | ⬜ |
| PAY-UC-REFUND-03 | 全額/部分返金・返金額制約の定義 | 同上 | B | 🟡 | ⬜ |
| PAY-UC-REFUND-04 | 失敗モード定義（refund_exceeded, gateway_error） | 同上 | B | 🟡 | ⬜ |
| PAY-UC-REFUND-05 | 観測性設計 | 同上 | B | 🟡 | ⬜ |
| PAY-UC-REFUND-06 | セキュリティ設計 | 同上 | B | 🟡 | ⬜ |

### 3.3 OpenAPI仕様
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-API-01 | POST /payments エンドポイント定義 | `docs/api/openapi/payment.yaml` | A | 🔴 | ✅ |
| PAY-API-02 | GET /payments/{id} エンドポイント定義 | 同上 | A | 🔴 | ✅ |
| PAY-API-03 | POST /payments/{id}/capture エンドポイント定義 | 同上 | B | 🟡 | ⬜ |
| PAY-API-04 | POST /payments/{id}/refund エンドポイント定義 | 同上 | B | 🟡 | ⬜ |
| PAY-API-05 | Idempotency-Key ヘッダー設計 | 同上 | A | 🔴 | ✅ |
| PAY-API-06 | Payment/PaymentStatus スキーマ定義 | 同上 | A | 🔴 | ✅ |

### 3.4 テスト計画
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-TEST-01 | Unit Test: Money値オブジェクト | `docs/test/test-plan.md` | A | 🔴 | ⬜ |
| PAY-TEST-02 | Unit Test: Payment集約 | 同上 | A | 🔴 | ⬜ |
| PAY-TEST-03 | Unit Test: IdempotencyKey | 同上 | A | 🔴 | ⬜ |
| PAY-TEST-04 | Integration Test: PaymentRepository | 同上 | A | 🟡 | ⬜ |
| PAY-TEST-05 | 冪等性テスト: 同一Idempotency-Key再送 | 同上 | A | 🔴 | ⬜ |
| PAY-TEST-06 | 境界値テスト: 金額境界（最小/最大） | 同上 | A | 🔴 | ⬜ |
| PAY-TEST-07 | E2E Test: create→capture→refund フロー | 同上 | B | 🟡 | ⬜ |

### 3.5 ADR
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PAY-ADR-01 | ADR-007: 冪等キー戦略の採用 | `docs/adr/0007-idempotency-key.md` | A | 🟡 | ⬜ |
| PAY-ADR-02 | ADR-008: 外部決済ゲートウェイの抽象化（ACL） | `docs/adr/0008-payment-gateway-acl.md` | A | 🟡 | ⬜ |
| PAY-ADR-03 | ADR-009: 支払いステータス遷移の設計 | `docs/adr/0009-payment-status.md` | A | 🟡 | ⬜ |
| PAY-ADR-04 | ADR-010: タイムアウト時の状態管理 | `docs/adr/0010-timeout-handling.md` | A | 🟡 | ⬜ |

---

## 4. Notification（通知）

### 4.1 コンテキスト設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| NTF-CTX-01 | 目的・責務の定義 | `docs/design/contexts/notification.md` | B | 🟡 | ⬜ |
| NTF-CTX-02 | 集約一覧（Notification, NotificationStatus）の定義 | 同上 | B | 🟡 | ⬜ |
| NTF-CTX-03 | Context Map（イベント受信元BCの定義）の定義 | 同上 | B | 🟡 | ⬜ |
| NTF-CTX-04 | 永続化設計（notifications テーブル） | 同上 | B | 🟡 | ⬜ |
| NTF-CTX-05 | ドメインイベント定義（NotificationSent, NotificationFailed） | 同上 | B | 🟡 | ⬜ |
| NTF-CTX-06 | 非機能要件（リトライ戦略、配信保証）の記載 | 同上 | B | 🟡 | ⬜ |

### 4.2 ユースケース設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| NTF-UC-01 | 目的・背景の記載 | `docs/design/usecases/notification-send.md` | B | 🟡 | ⬜ |
| NTF-UC-02 | 入出力定義（SendNotificationCommand/イベント受信→NotificationSent） | 同上 | B | 🟡 | ⬜ |
| NTF-UC-03 | ドメインモデル（通知種別、テンプレート）の定義 | 同上 | B | 🟡 | ⬜ |
| NTF-UC-04 | 失敗モード定義（delivery_failed, retry戦略） | 同上 | B | 🟡 | ⬜ |
| NTF-UC-05 | 観測性設計（notification_sent/failed メトリクス） | 同上 | B | 🟡 | ⬜ |
| NTF-UC-06 | セキュリティ設計（PII非出力） | 同上 | B | 🟡 | ⬜ |

### 4.3 OpenAPI仕様
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| NTF-API-01 | POST /notifications エンドポイント定義（内部用） | `docs/api/openapi/notification.yaml` | B | 🟡 | ⬜ |
| NTF-API-02 | GET /notifications エンドポイント定義 | 同上 | B | 🟡 | ⬜ |
| NTF-API-03 | GET /notifications/{id} エンドポイント定義 | 同上 | B | 🟡 | ⬜ |
| NTF-API-04 | Notification/NotificationStatus スキーマ定義 | 同上 | B | 🟡 | ⬜ |

---

## 5. Audit（監査）

### 5.1 コンテキスト設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| AUD-CTX-01 | 目的・責務の定義 | `docs/design/contexts/audit.md` | B | 🟡 | ⬜ |
| AUD-CTX-02 | 集約一覧（AuditLog）の定義 | 同上 | B | 🟡 | ⬜ |
| AUD-CTX-03 | Context Map（全BCからのイベント受信）の定義 | 同上 | B | 🟡 | ⬜ |
| AUD-CTX-04 | 永続化設計（audit_logs テーブル、追記専用） | 同上 | B | 🟡 | ⬜ |
| AUD-CTX-05 | ドメインイベント定義（AuditLogRecorded） | 同上 | B | 🟡 | ⬜ |
| AUD-CTX-06 | 非機能要件（改ざん防止、保持期間）の記載 | 同上 | B | 🟡 | ⬜ |

### 5.2 ユースケース設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| AUD-UC-01 | 目的・背景の記載 | `docs/design/usecases/audit-record.md` | B | 🟡 | ⬜ |
| AUD-UC-02 | 入出力定義（RecordAuditCommand/イベント受信→AuditLogRecorded） | 同上 | B | 🟡 | ⬜ |
| AUD-UC-03 | ドメインモデル（監査対象操作、actor/action/resource構造）の定義 | 同上 | B | 🟡 | ⬜ |
| AUD-UC-04 | 失敗モード定義（ストレージ障害時の対応） | 同上 | B | 🟡 | ⬜ |
| AUD-UC-05 | 観測性設計（audit_log_recorded メトリクス） | 同上 | B | 🟡 | ⬜ |
| AUD-UC-06 | セキュリティ設計（改ざん防止、管理者権限） | 同上 | B | 🟡 | ⬜ |

### 5.3 OpenAPI仕様
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| AUD-API-01 | GET /audit-logs エンドポイント定義 | `docs/api/openapi/audit.yaml` | B | 🟡 | ⬜ |
| AUD-API-02 | GET /audit-logs/{id} エンドポイント定義 | 同上 | B | 🟡 | ⬜ |
| AUD-API-03 | AuditLog スキーマ定義 | 同上 | B | 🟡 | ⬜ |
| AUD-API-04 | 管理者権限による認可設計 | 同上 | B | 🟡 | ⬜ |

---

## 6. Ledger（台帳・投影）

### 6.1 コンテキスト設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| LDG-CTX-01 | 目的・責務の定義 | `docs/design/contexts/ledger.md` | D | 🟢 | ⬜ |
| LDG-CTX-02 | 集約一覧（LedgerEntry, Balance）の定義 | 同上 | D | 🟢 | ⬜ |
| LDG-CTX-03 | Context Map（Payment/Bookingイベント受信）の定義 | 同上 | D | 🟢 | ⬜ |
| LDG-CTX-04 | 永続化設計（ledger_entries, projections テーブル） | 同上 | D | 🟢 | ⬜ |
| LDG-CTX-05 | イベントソーシング/CQRS設計の記載 | 同上 | D | 🟢 | ⬜ |
| LDG-CTX-06 | 非機能要件（再構築戦略、一貫性）の記載 | 同上 | D | 🟢 | ⬜ |

### 6.2 ユースケース設計
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| LDG-UC-01 | 目的・背景の記載 | `docs/design/usecases/ledger-project.md` | D | 🟢 | ⬜ |
| LDG-UC-02 | 入出力定義（イベント受信→Projection更新） | 同上 | D | 🟢 | ⬜ |
| LDG-UC-03 | ドメインモデル（エントリ、残高計算）の定義 | 同上 | D | 🟢 | ⬜ |
| LDG-UC-04 | 失敗モード定義（再構築戦略、順序保証） | 同上 | D | 🟢 | ⬜ |
| LDG-UC-05 | 観測性設計（projection_lag メトリクス） | 同上 | D | 🟢 | ⬜ |
| LDG-UC-06 | セキュリティ設計（読取専用API） | 同上 | D | 🟢 | ⬜ |

### 6.3 OpenAPI仕様
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| LDG-API-01 | GET /ledger/entries エンドポイント定義 | `docs/api/openapi/ledger.yaml`（新規） | D | 🟢 | ⬜ |
| LDG-API-02 | GET /ledger/balance エンドポイント定義 | 同上 | D | 🟢 | ⬜ |
| LDG-API-03 | LedgerEntry/Balance スキーマ定義 | 同上 | D | 🟢 | ⬜ |

---

## 7. 横断的関心事（Cross-Cutting Concerns）

### 7.1 観測性（Observability）
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| OBS-01 | ログフォーマット設計（JSON構造化ログ） | `docs/design/observability.md` | A | 🔴 | ✅ |
| OBS-02 | 必須フィールド定義（traceId, spanId, timestamp, level） | 同上 | A | 🔴 | ✅ |
| OBS-03 | PIIマスキングルールの具体化 | 同上 | A | 🔴 | ✅ |
| OBS-04 | REDメトリクス定義（Rate, Errors, Duration） | 同上 | A | 🔴 | ✅ |
| OBS-05 | サービス固有メトリクス定義 | 同上 | A | 🟡 | ✅ |
| OBS-06 | Prometheus形式のメトリクス命名規則 | 同上 | A | 🟡 | ✅ |
| OBS-07 | OpenTelemetry設定方針 | 同上 | A | 🔴 | ✅ |
| OBS-08 | Span命名規則・必須属性一覧 | 同上 | A | 🟡 | ✅ |
| OBS-09 | サンプリング戦略の定義 | 同上 | A | 🟡 | ✅ |
| OBS-10 | SLI/SLO定義 | 同上 | A | 🔴 | ✅ |
| OBS-11 | アラート閾値・エスカレーションルール | 同上 | C | 🟡 | ⬜ |
| OBS-12 | ダッシュボード設計（主要KPI） | 同上 | C | 🟡 | ⬜ |

### 7.2 セキュリティ（共通）
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| SEC-01 | PII定義（メールアドレス、IPアドレス等） | `docs/security/pii-policy.md` | A | 🔴 | ✅ |
| SEC-02 | マスキングルール具体化（email, IP） | 同上 | A | 🔴 | ✅ |
| SEC-03 | ログ出力禁止項目（パスワード、カード情報） | 同上 | A | 🔴 | ✅ |
| SEC-04 | 保持期間と削除方針 | 同上 | A | 🟡 | ✅ |
| SEC-05 | シークレット一覧（DB接続、JWT鍵、外部API） | `docs/security/secrets.md` | A | 🟡 | ⬜ |
| SEC-06 | シークレット管理方針（環境変数 vs Vault） | 同上 | A | 🟡 | ⬜ |
| SEC-07 | シークレット漏洩時の対応手順 | 同上 | B | 🟡 | ⬜ |
| SEC-08 | 脅威モデル：資産の特定 | `docs/security/threat-model.md` | B | 🟡 | ⬜ |
| SEC-09 | 脅威モデル：STRIDE分析 | 同上 | B | 🟡 | ⬜ |
| SEC-10 | 脅威モデル：対策とリスク評価 | 同上 | B | 🟡 | ⬜ |
| SEC-11 | SBOM生成方針 | `docs/security/sbom-cve-ops.md` | C | 🟢 | ⬜ |
| SEC-12 | CVEスキャン設定 | 同上 | C | 🟢 | ⬜ |
| SEC-13 | 脆弱性対応フロー | 同上 | C | 🟢 | ⬜ |

### 7.3 テスト（共通）
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| TEST-01 | Contract Testの設計方針 | `docs/test/test-plan.md` | A | 🔴 | ✅ |
| TEST-02 | 契約テスト：リクエスト/レスポンス形式検証 | 同上 | A | 🔴 | ✅ |
| TEST-03 | E2E Testシナリオ：認証→予約→支払い→通知→監査 | 同上 | B | 🟡 | ⬜ |
| TEST-04 | 互換性テスト：v1/v2併走 | 同上 | C | 🟢 | ⬜ |

### 7.4 運用ドキュメント
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| OPS-01 | 設計概要：システムアーキテクチャ図 | `docs/design/overview.md` | A | 🟡 | ✅ |
| OPS-02 | 設計概要：BC間関係図 | 同上 | A | 🟡 | ✅ |
| OPS-03 | 設計概要：データフロー図 | 同上 | A | 🟡 | ✅ |
| OPS-04 | Runbook：インシデント対応フロー概要 | `docs/runbook/README.md` | C | 🟡 | ⬜ |
| OPS-05 | Runbook：共通手順（ログ確認、再起動、ロールバック） | 同上 | C | 🟡 | ⬜ |
| OPS-06 | Runbook：DB接続障害対応 | `docs/runbook/incident-db-connection.md` | C | 🟢 | ⬜ |
| OPS-07 | Runbook：決済Gateway障害対応 | `docs/runbook/incident-payment-gateway.md` | C | 🟢 | ⬜ |
| OPS-08 | Runbook：高レイテンシ対応 | `docs/runbook/incident-high-latency.md` | C | 🟢 | ⬜ |
| OPS-09 | API Migration：v1/v2併走方針 | `docs/api/migration/v1-to-v2.md` | C | 🟢 | ⬜ |
| OPS-10 | API Migration：破壊的変更一覧 | 同上 | C | 🟢 | ⬜ |
| OPS-11 | API Migration：廃止スケジュール | 同上 | C | 🟢 | ⬜ |

### 7.5 ドメイン用語
| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| GLO-01 | TimeRangeの定義 | `docs/domain/glossary.md` | A | 🔴 | ✅ |
| GLO-02 | Idempotency Keyの定義 | 同上 | A | 🔴 | ✅ |
| GLO-03 | Payment状態（PENDING/AUTHORIZED/CAPTURED/REFUNDED）の定義 | 同上 | A | 🔴 | ✅ |
| GLO-04 | Booking状態（PENDING/CONFIRMED/CANCELLED）の定義 | 同上 | A | 🔴 | ✅ |
| GLO-05 | 認証トークン（AccessToken/RefreshToken）の定義 | 同上 | A | 🔴 | ✅ |

---

## 8. PRD承認ゲート

| ID | タスク | ファイル | Slice | 優先度 | 状態 |
|----|--------|----------|-------|--------|------|
| PRD-01 | Platform PRD レビュー | `docs/prd-platform.md` | Gate | 🔴 | ⬜ |
| PRD-02 | Platform PRD 承認（status: approved） | 同上 | Gate | 🔴 | ⬜ |
| PRD-03 | DevEx AI PRD レビュー | `docs/prd-devex-ai.md` | Gate | 🟡 | ⬜ |
| PRD-04 | DevEx AI PRD 承認（status: approved） | 同上 | Gate | 🟡 | ⬜ |

---

## サマリー

### Slice A（最小MVP）タスク数
| 機能 | コンテキスト | ユースケース | OpenAPI | テスト | セキュリティ | ADR | 合計 |
|------|-------------|-------------|---------|-------|-------------|-----|------|
| IAM | 6 | 6 ✅ | 5 ✅ | 6 | 5 | 3 | 31 |
| Booking | 6 | 13 | 6 | 7 | - | 3 | 35 |
| Payment | 6 | 6 ✅ | 6 | 7 | - | 4 | 29 |
| 横断 | - | - | - | 4 | 7 | - | 11 |
| 用語 | - | - | - | - | - | - | 5 |
| PRD | - | - | - | - | - | - | 2 |
| **合計** | 18 | 25 | 17 | 24 | 12 | 10 | **113** |

### 完了状況
- ✅ 完了済み: 約 **25タスク**（IAM/Booking/Paymentの主要ユースケース・OpenAPI）
- ⬜ 未着手: 約 **88タスク**

---

## 推奨作業順序（Slice A）

1. **PRD承認** (PRD-01, PRD-02)
2. **用語集** (GLO-01〜05)
3. **コンテキスト設計** (IAM→Booking→Payment)
4. **観測性基盤** (OBS-01〜04, 07, 10)
5. **セキュリティ基盤** (SEC-01〜04)
6. **テスト計画** (TEST-01〜02, 各機能テスト)
7. **ユースケース補完** (BK-UC-UPDATE, BK-UC-CANCEL)
8. **ADR作成** (必要に応じて)
