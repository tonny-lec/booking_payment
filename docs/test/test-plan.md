---
doc_type: "test_plan"
id: "test-plan"
version: "0.2"
last_updated: "2026-01-16"
status: "draft"
---

# テスト計画（ユースケース別）

## 重点
- 二重予約、境界時間
- 冪等性欠落による二重課金
- タイムアウト/リトライでの重複処理
- 権限チェック漏れ
- 互換性破壊
- traceId欠落

## property-based候補
- TimeRange（start < end）
- Idempotency（同一入力→同一出力）
- 状態遷移（許可されない遷移は拒否）
