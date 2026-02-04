# Booking & Payment Platform

予約・決済を扱う基盤システムのリファレンス実装です。

## 概要

このプロジェクトは、リソース予約と決済処理を統合したプラットフォームです。DDD（ドメイン駆動設計）と契約ファースト開発を採用し、観測性（Observability）とSLOを重視した設計になっています。

## 主な機能

| 機能 | 説明 |
|------|------|
| **予約管理** | リソースの時間帯予約、衝突検出、楽観的ロック |
| **決済処理** | 与信・キャプチャ・返金、冪等性保証 |
| **認証・認可** | JWT認証、RefreshTokenローテーション、Brute-force対策 |
| **通知** | イベント駆動の通知配信 |
| **監査** | 重要操作の監査ログ記録 |

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語・フレームワーク | Java 25 / Spring Boot |
| データベース | PostgreSQL |
| キャッシュ | Redis |
| メッセージング | Kafka |
| API仕様 | OpenAPI 3.0 |
| 観測性 | OpenTelemetry |
| IaC | Terraform |
| ビルド | Gradle |

## アーキテクチャ

### Bounded Contexts (DDD)

```
┌─────────┐     ┌─────────┐     ┌──────────────┐
│   IAM   │────▶│ Booking │────▶│   Payment    │
│ (認証)  │     │ (予約)  │     │   (決済)     │
└─────────┘     └────┬────┘     └──────┬───────┘
                     │                  │
                     ▼                  ▼
              ┌──────────────────────────────┐
              │    Notification / Audit      │
              │    (通知 / 監査ログ)          │
              └──────────────────────────────┘
```

### Hexagonal Architecture

各モジュールは Hexagonal Architecture（Ports & Adapters）を採用：

```
src/main/java/.../
  domain/       # ドメインモデル（ビジネスロジック）
  application/  # ユースケース
  adapter/in/   # 入力アダプタ（REST API）
  adapter/out/  # 出力アダプタ（DB、外部API）
```

## ドキュメント構成

| パス | 内容 |
|------|------|
| `docs/design/contexts/` | コンテキスト設計（IAM, Booking, Payment等） |
| `docs/design/usecases/` | ユースケース設計 |
| `docs/api/openapi/` | OpenAPI仕様（契約） |
| `docs/domain/glossary.md` | ドメイン用語集 |
| `docs/security/` | セキュリティポリシー |
| `docs/test/` | テスト計画 |
| `docs/adr/` | アーキテクチャ決定記録 |

## 開発者向け情報

- **エージェント向けガイド**: [AGENTS.md](./AGENTS.md), [CLAUDE.md](./CLAUDE.md)
- **プロジェクトルール**: [agents/rules.md](./agents/rules.md)
- **PRD**: [docs/prd-platform.md](./docs/prd-platform.md)

## ステータス

🚧 **現在はドキュメント設計フェーズ**（`src/` 未実装）

設計ドキュメントを整備中で、実装は今後のフェーズで進める予定です。
