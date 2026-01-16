# 未完了タスク: その他ドキュメント

## 概要
- 対象: マイグレーション、Runbook、設計概要
- 状態: スケルトン状態
- 優先度: **低〜中**

---

## タスク一覧

### DOC-1: API マイグレーションガイド
- ファイル: `docs/api/migration/v1-to-v2.md`
- Slice: C
- 優先度: 低
- 現状: 要件概要のみ
- 未完了セクション:
  - [ ] v1/v2 併走の方針
  - [ ] 破壊的変更の一覧
  - [ ] 移行手順
  - [ ] 廃止スケジュール
- 備考: v2が存在しない現時点では優先度低

### DOC-2: Runbook
- ファイル: `docs/runbook/README.md`
- Slice: C
- 優先度: 中
- 現状: "incident-* を追加していく" のみ
- 未完了セクション:
  - [ ] インシデント対応フロー概要
  - [ ] 共通手順（ログ確認、再起動、ロールバック）
  - [ ] サービス別Runbookへのリンク
- 今後追加するファイル:
  - [ ] `runbook/incident-db-connection.md`
  - [ ] `runbook/incident-payment-gateway.md`
  - [ ] `runbook/incident-high-latency.md`

### DOC-3: 設計概要
- ファイル: `docs/design/overview.md`
- Slice: A
- 優先度: 中
- 現状: 構成説明のみ
- 未完了セクション:
  - [ ] システム全体アーキテクチャ図
  - [ ] Bounded Context間の関係図
  - [ ] データフロー図
  - [ ] 技術スタック概要

### DOC-4: ADR（Architecture Decision Records）
- フォルダ: `docs/adr/`（未作成）
- Slice: A〜
- 優先度: 中
- 作成予定:
  - [ ] ADR-001: JWT認証方式の採用
  - [ ] ADR-002: RefreshTokenローテーション戦略
  - [ ] ADR-003: Brute-force対策の閾値設定
  - [ ] ADR-004: 予約衝突検出戦略
  - [ ] ADR-005: 楽観的ロックの採用理由
  - [ ] ADR-006: 予約ステータス遷移の設計
  - [ ] ADR-007: 冪等キー戦略の採用
  - [ ] ADR-008: 外部決済ゲートウェイの抽象化（ACL）
  - [ ] ADR-009: 支払いステータス遷移の設計
  - [ ] ADR-010: タイムアウト時の状態管理
- 備考: `scripts/new-adr.sh` で作成可能

---

## 完了条件（DoD）
- 各ファイルでスケルトンが解消されている
- 実運用に必要な情報が含まれている
- 他のドキュメントと整合性がある
