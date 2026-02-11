---
description: Graphite (gt) commands workflow policy
---
// turbo-all

# Graphite Commands Workflow

このワークフローは、Graphite CLI（`gt`）を使った stacked PR 運用ポリシーを定義します。
GitHub PR は Graphite 経由で作成・更新します。

---

## Core Flow

### 作業開始時（必須手順）
```bash
# 1. trunk(main)に移動
gt checkout main

# 2. trunkとstackを同期
gt sync
```

### 変更単位
- 1つの論理変更ごとに1 branch / 1 PR を原則とする
- 依存する変更は上に積んで stack にする（stacked PR）

### コミット・ブランチ作成
```bash
# 変更をステージ
git add <files>

# branch + commit を同時作成
gt create --message "<type>: <summary>"
```

- type: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- 1コミット = 1つの論理的変更

### PR作成・更新
```bash
# 現在ブランチ（および必要なdownstack）を提出
gt submit --no-interactive
```

### レビュー指摘反映（mid-stack）
```bash
# 修正後に現在ブランチを更新
gt modify

# 再提出
gt submit --no-interactive
```

### stack整合と最新化
```bash
# trunk更新 + stack再配置
gt sync

# 必要時のみ手動restack
gt restack
```

---

## Safe/Restricted Operations

### ✅ 推奨コマンド
- `gt checkout`
- `gt sync`
- `gt create --message "..."`
- `gt modify`
- `gt restack`
- `gt submit --no-interactive`
- `gt up` / `gt down` / `gt top` / `gt bottom`
- `gt log` / `gt ls` / `gt state`

### ⚠️ 制限コマンド
- `git push origin main`（禁止）
- `git commit`（通常フローでは非推奨。`gt create`を使う）
- `git push`（通常フローでは非推奨。`gt submit`を使う）
- `git reset --hard` / `git push --force`（明示承認なし禁止）

---

## PR Title Format

```
<type>(<scope>): <summary>
```

例:
- `feat(iam): add refresh token rotation policy`
- `docs(booking): add cancellation rules use case`

---

## References

- `agents/rules.md`
- `AGENTS.md`
