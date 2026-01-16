---
doc_type: "agent_rules"
id: "rules"
version: "0.2"
last_updated: "2026-01-16"
status: "stable"
---

# ルール（System FixのSSOT）

## Must（絶対）
1. **PRD-First**：承認済みPRD（`status: approved`）なしに実装（コード）変更しない
2. **Evidence-First**：提案/変更/レビューには必ず根拠（差分/ログ/計測/仕様）を付ける
3. **Small Changes**：変更は最小。分割して各分割ごとに検証
4. **Tests Gate**：テストが落ちる変更はReject（例外なし）
5. **No Secrets / No PII**：Secrets/PIIを出力・埋め込みしない（SSOT：`docs/security/pii-policy.md`）

## Must Not（禁止）
- SSOTを無視してコードだけ変更
- 推論を事実として断定（不確かなら推論と明記）
- 外部送信設定（webhook等）を追加（明示許可なし）

## System Fix（更新先）
- 重要失敗は Postmortem を作り、**この rules.md を更新**してから修正する。
