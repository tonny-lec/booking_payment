# Booking & Payment Platform

予約・決済を扱う基盤システムのリファレンス実装です。DDD と契約ファースト開発を前提に、ドキュメントと Java/Spring の実装を同じリポジトリで管理しています。

## 概要

このプロジェクトは、予約と支払いを扱う複数の境界づけられたコンテキストを、Hexagonal Architecture で分離したマルチモジュール構成のサンプルです。設計ドキュメント、OpenAPI 契約、Spring Boot 実装、テスト、運用補助スクリプトを一体で整備しています。

## 主な機能

| 機能 | 説明 |
|------|------|
| **予約管理** | リソースの時間帯予約、衝突検出、楽観的ロック |
| **決済処理** | 与信・キャプチャ・返金、冪等性保証 |
| **認証・認可** | JWT認証、RefreshTokenローテーション、Brute-force対策 |
| **通知 / 監査** | 重要イベントの通知・監査記録を後続スライスで強化 |
| **観測性** | traceId 相関、HTTP メトリクス、OpenTelemetry 連携 |

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語・フレームワーク | Java 25 / Spring Boot 4 |
| データベース | PostgreSQL |
| API仕様 | OpenAPI 3.0 |
| 観測性 | Micrometer / OpenTelemetry |
| セキュリティ | Spring Security / JWT |
| ビルド | Gradle multi-module |
| テスト | JUnit 5 / Mockito / Testcontainers / ArchUnit |

## モジュール構成

このリポジトリは単一 `src/` 構成ではなく、責務ごとに分けた Gradle multi-module 構成です。

| モジュール | 役割 |
|-----------|------|
| `domain` | ドメインモデル、値オブジェクト、ドメインイベント |
| `application` | ユースケース、アプリケーションサービス、ポート |
| `adapter-web` | REST API、入力検証、例外変換、セキュリティ |
| `adapter-persistence` | JPA エンティティ、リポジトリ実装 |
| `bootstrap` | Spring Boot 起動、設定、Flyway、監視配線 |

## アーキテクチャ

### Bounded Contexts

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

### Hexagonal layering

各コンテキストは概ね次の責務分割で実装されています。

```text
domain -> application -> adapter-web / adapter-persistence -> bootstrap
```

## ドキュメント構成

| パス | 内容 |
|------|------|
| `docs/design/contexts/` | コンテキスト設計（IAM, Booking, Payment 等） |
| `docs/design/usecases/` | ユースケース設計 |
| `docs/api/openapi/` | OpenAPI仕様（契約） |
| `docs/domain/glossary.md` | ドメイン用語集 |
| `docs/security/` | セキュリティポリシー |
| `docs/test/` | テスト計画 |
| `docs/adr/` | アーキテクチャ決定記録 |

## 開発者向け情報

- エージェント向けガイド: [AGENTS.md](./AGENTS.md), [CLAUDE.md](./CLAUDE.md)
- プロジェクトルール: [agents/rules.md](./agents/rules.md)
- Platform PRD: [docs/prd-platform.md](./docs/prd-platform.md)
- DevEx AI PRD: [docs/prd-devex-ai.md](./docs/prd-devex-ai.md)

## 現在の状態

設計ドキュメント中心のリポジトリとして始まっていますが、現在は Booking / IAM を中心に Java 実装とテストが入っています。README の情報だけでなく、`docs/` の設計と各モジュールのコードを合わせて確認してください。
