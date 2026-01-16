# タスク一覧

このフォルダには、プロジェクトの未完了タスクがカテゴリ別にまとめられています。

## ファイル一覧

| ファイル | カテゴリ | 優先度 | Slice |
|----------|----------|--------|-------|
| [01-contexts.md](./01-contexts.md) | コンテキスト設計 | 高 | A, B, D |
| [02-usecases.md](./02-usecases.md) | ユースケース設計 | 高〜中 | A, B, D |
| [03-openapi.md](./03-openapi.md) | OpenAPI仕様 | 中 | B, D |
| [04-test-observability.md](./04-test-observability.md) | テスト・観測性 | 高 | A |
| [05-security.md](./05-security.md) | セキュリティ | 中 | A, B, C |
| [06-prd-approval.md](./06-prd-approval.md) | PRD承認 | 高 | Gate |
| [07-other-docs.md](./07-other-docs.md) | その他 | 低〜中 | A, C |

---

## Slice別タスク概要

### Slice A（最小MVP）- 高優先度
- [ ] コンテキスト設計: iam.md, booking.md, payment.md
- [ ] ユースケース: booking-update.md, booking-cancel.md
- [ ] テスト計画: test-plan.md 具体化
- [ ] 観測性: observability.md 具体化
- [ ] セキュリティ: security.md, pii-policy.md
- [ ] PRD承認: prd-platform.md → approved

### Slice B（E2E成立）- 中優先度
- [ ] コンテキスト設計: audit.md, notification.md
- [ ] ユースケース: payment-capture.md, payment-refund.md, notification-send.md, audit-record.md
- [ ] OpenAPI: audit.yaml, notification.yaml
- [ ] セキュリティ: threat-model.md

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
