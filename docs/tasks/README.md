# タスク一覧

このフォルダには、プロジェクトのタスクがカテゴリ別にまとめられています。

## 推奨: 機能別タスク一覧

**[by-feature.md](./by-feature.md)** - 機能（Bounded Context）ごとに細分化されたタスク一覧

- IAM / Booking / Payment / Notification / Audit / Ledger 別に整理
- 各タスクにID付与（例: IAM-CTX-01, BK-UC-UPDATE-03）
- 優先度・Slice・完了状態を明示
- 詳細タスク（01-07）への参照リンク付き

---

## 実装タスク（Slice A）

**[implementation-slice-a.md](./implementation-slice-a.md)** - Slice Aの実装タスク一覧

- 実装レイヤー別（Domain / Application / Infrastructure / Web / Test）
- 参照リンクで設計/契約/テストに接続

---

## ファイル一覧（ドキュメント種別）

| ファイル | カテゴリ | 優先度 | Slice |
|----------|----------|--------|-------|
| [01-contexts.md](./01-contexts.md) | コンテキスト設計 | 高 | A, B, D |
| [02-usecases.md](./02-usecases.md) | ユースケース設計 | 高〜中 | A, B, D |
| [03-openapi.md](./03-openapi.md) | OpenAPI仕様 | 中 | A, B, D |
| [04-test-observability.md](./04-test-observability.md) | テスト・観測性 | 高 | A |
| [05-security.md](./05-security.md) | セキュリティ | 中 | A, B, C |
| [06-prd-approval.md](./06-prd-approval.md) | PRD承認 | 高 | Gate |
| [07-other-docs.md](./07-other-docs.md) | その他 | 低〜中 | A, C |

---

## Slice別タスク概要

### Slice A（最小MVP）- 高優先度
- [x] コンテキスト設計: iam/booking/payment
- [x] ユースケース: booking-update, booking-cancel
- [x] テスト計画: test-plan.md 具体化
- [x] 観測性: observability.md 具体化
- [x] セキュリティ: security.md, pii-policy.md
- [x] PRD承認: prd-platform.md（DevEx AIは別ゲート）

### Slice B（E2E成立）- 中優先度
- [x] コンテキスト設計: audit.md, notification.md
- [x] ユースケース: payment-capture, payment-refund, notification-send, audit-record
- [x] OpenAPI: audit.yaml, notification.yaml
- [x] セキュリティ: threat-model.md

### Slice C（互換性と運用）- 低優先度
- [ ] マイグレーション: v1-to-v2.md
- [ ] Runbook: incident対応手順
- [ ] セキュリティ: sbom-cve-ops.md

### Slice D（イベント駆動 + Ledger）- 低優先度
- [ ] コンテキスト設計: ledger.md
- [ ] ユースケース: ledger-project.md
- [ ] OpenAPI: ledger.yaml（新規）

---

## 推奨する作業順序

1. **PRD承認** - 実装開始のゲート
2. **コンテキスト設計（iam/booking/payment）** - Slice A基盤
3. **テスト計画・観測性** - Slice A検証基準
4. **ユースケース（booking-update/cancel）** - Slice A完成
5. **セキュリティ（pii-policy）** - ログ出力ルール
6. **Slice B以降** - 順次対応

---

## 進捗管理

各タスクファイル内のチェックボックスを使用して進捗を管理してください。

```markdown
- [ ] 未完了
- [x] 完了
```
