---
doc_type: "checkpoint"
version: "0.1"
last_updated: "2026-02-04"
status: "active"
---

# checkpoint.md（Context Reset：会話が長くなったらここへ要約）

## 現在のゴール
- DDDコンテキスト中心のパッケージ構成へ段階的に再編し、共有レイヤを整理する

## 決定事項（SSOTに反映済み）
- 方針: DDDコンテキスト中心（`com.booking.<context>.*`）で整理、共有は `com.booking.shared.*`
- 段階導入: IAM から開始し、Booking/Payment は枠のみ作成
- モジュール境界は維持（domain/application/adapter-*/bootstrap）
- 共有アダプタ（例外/メトリクス/OpenAPI検証）は shared 配下へ移動

## 未決事項 / 質問
- なし

## 次のタスク（ファイルパス + 検証）
- 完了（PR #57 で反映済み）

## 過去プラン（全文）

### タイトル
プロジェクト全体リファクタリング計画（DDDコンテキスト中心 / 段階導入）

### 概要
- 現在の「レイヤーモジュール構成（domain/application/adapter）」は維持しつつ、Javaパッケージ構成をBC中心へ再編する。
- 影響を最小化するため、IAM から段階的に移行し、以降 Booking/Payment に同じ規約を展開する。
- 併せて共通（shared）系の配置を明確化し、Springのスキャンや依存関係が自然に読める構成へ統一する。

### 重要な変更（公開API/インターフェース）
- Javaパッケージの移動により、公開クラスのFQCNが変わる（依存する外部コードがある場合は影響）。
- 例: `com.booking.domain.iam.model.User` → `com.booking.iam.domain.model.User`
- `MetricsPort` 等の共通インターフェースは `com.booking.shared.*` に移動予定。
- Springコンポーネントのスキャン対象は `com.booking` ルートのまま維持するため、`@SpringBootApplication` の変更は不要。

### フェーズ1: IAMコンテキストのパッケージ再設計
1. ターゲット構成を定義
   `com.booking.iam.domain.*`
   `com.booking.iam.application.*`
   `com.booking.iam.adapter.web.*`
   `com.booking.iam.adapter.persistence.*`
   `com.booking.shared.*`（横断系）
2. IAMドメインの移動
   `domain/src/main/java/com/booking/domain/iam/**` → `domain/src/main/java/com/booking/iam/domain/**`
   `domain/src/main/java/com/booking/domain/shared/**` → `domain/src/main/java/com/booking/shared/**`
3. アプリケーション層の移動
   `application/src/main/java/com/booking/application/shared/**` → `application/src/main/java/com/booking/shared/**`
   以降IAMユースケースが追加される想定で `application/src/main/java/com/booking/iam/application/**` を用意
4. アダプター層の移動（現状分）
   `adapter-web` 内を `com.booking.shared.adapter.web.*` と `com.booking.iam.adapter.web.*` に整理
   `OpenApiValidationConfig` は `com.booking.shared.adapter.web.openapi` に移動（BC横断）
5. import修正 / package宣言修正
   既存の参照先を新パッケージへ全置換
6. ビルド/テスト実行
   `./gradlew :domain:compileJava :application:compileJava :adapter-web:compileJava`
   `./gradlew :bootstrap:test` で最低限確認

### フェーズ2: Booking / Payment への展開準備
1. 空のパッケージ枠だけ作成（実装なし）
   `com.booking.booking.domain`
   `com.booking.payment.domain`
   `com.booking.booking.application`
   `com.booking.payment.application`
2. これにより以降の実装追加時に配置ルールが確定する

### フェーズ3: ディレクトリ構成の明文化
1. `docs/plan/file-map.md` に新パッケージ構成の配置ルールを追記
2. `docs/design/overview.md` に「BC中心のパッケージ構成」を図示・説明

### テストケース / シナリオ
- コンパイル確認
  `./gradlew :domain:compileJava`
  `./gradlew :application:compileJava`
  `./gradlew :adapter-web:compileJava`
- 簡易統合
  `./gradlew :bootstrap:test`
- 影響確認
  importの自動修正が漏れていないこと
  Springコンポーネントが検出されること（起動時Bean定義エラーがない）

### 前提・仮定
- Gradleモジュール構成（domain/application/adapter-*/bootstrap）は維持する。
- 外部に公開しているバイナリ/APIはない前提で進める（FQCN変更の影響は内部に限定）。
- 変更は IAM から開始し、Booking/Payment は空パッケージのみ作成する。
