---
doc_type: "prd"
id: "prd-devex-ai"
version: "0.2"
last_updated: "2026-01-16"
status: "proposed"   # proposed | approved | archived
---

# PRD: DevEx AI（CI組み込み）

## 1. 目的
- AI支援を「工程」にする（再現可能・監査可能）
- 品質（テスト/互換性/セキュリティ）を落とさずに速度を上げる

## 2. 非目的（Out-of-Scope）
- 自律デプロイ（人間承認なし）
- Secrets/PIIを扱う（入力・出力・保存）
- 本番データへのアクセス

## 3. MVPユースケース
1. PR差分 → レビュー観点抽出（境界条件/例外/セキュリティ/互換性）
2. テストケース提案（property-based候補含む）
3. 依存関係変更リスク要約（CVE/破壊的変更）
4. ADR草案生成（トレードオフ整理）

## 4. ツール前提 / ゲート（Engineering Assumptions）
- SSOT：`docs/agent/rules.md`
- CI: GitHub Actions（`.github/workflows/agent-qa.yml`）
- 標準コマンド：
  - PRDゲート：`scripts/prd-gate.sh`
  - Evidence lint：`scripts/evidence-lint.sh`
  - テスト：`scripts/test-all.sh`
  - ADR作成：`scripts/new-adr.sh`
- セキュリティ（後続で実装）：SBOM/CVEゲート（`docs/security/sbom-cve-ops.md`）

## 5. Acceptance Criteria
- 出力に必ず **Evidence（根拠）** と **推論なら推論** が含まれる
- CIゲートを通らない変更はReject
- PII/Secretsを出力しない（違反はReject）

## 6. Approval Gate
- `status: approved` にしてからCIを厳格化する（ゲートをFailにする）
