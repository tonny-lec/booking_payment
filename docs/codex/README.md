---
doc_type: "codex_intel_index"
id: "codex-intel-index"
version: "0.1"
last_updated: "2026-02-08"
status: "draft"
---

# Codex Intelligence Hub

## Purpose
- Codex の最新情報を、戦略/運用/セキュリティ/事例/時系列に分類して継続管理する。
- 公式と外部の情報を同列に扱い、主張ごとに根拠URLを紐付ける。

## Document Map
- `docs/codex/01-strategy-org.md`: 戦略・組織設計
- `docs/codex/02-operations-playbook.md`: 日常運用プレイブック
- `docs/codex/03-security-governance.md`: セキュリティ・ガバナンス
- `docs/codex/04-use-cases-lessons.md`: 活用事例と学び
- `docs/codex/05-whats-new-timeline.md`: 最新アップデート時系列
- `docs/codex/sources.yaml`: 収集対象URLの台帳（SSOT）

## Source Policy
- 重み付け: 公式/外部を同列で扱う。
- 確度: `high` / `medium` / `low` を明示。
- 日付: すべて絶対日付（`YYYY-MM-DD`）。
- 取得制限: 一部サイトが `403` を返す場合は `fetch degraded` と明示し、`sources.yaml` の `known_date` を補完値として使う。

## Update Flow
1. 収集: `python3 scripts/codex/fetch_sources.py --manifest docs/codex/sources.yaml --out .codex-intel/raw.json`
2. 分析: `python3 scripts/codex/build_digest.py --in .codex-intel/raw.json --out .codex-intel/digest.json`
3. 反映: `python3 scripts/codex/update_docs.py --digest .codex-intel/digest.json --docs-root docs/codex`
4. 検証: `CHECK_LINKS=1 bash scripts/codex/validate.sh`

## Automation
- 週次更新 Workflow: `.github/workflows/codex-intel-refresh.yml`
- 手動更新: `workflow_dispatch` で `full` / `incremental` を選択可能。
