---
doc_type: "tech_stack"
id: "tech-stack"
version: "0.1"
last_updated: "2026-01-16"
status: "draft"
---

# Tech Stack & Constraints（SSOT）

## Runtime / Language
- Java: 21+（Loom/Virtual Threadsの検証対象）
- Build: Gradle（推奨）または Maven（代替）

## Framework / Libraries（推奨）
- Spring Boot（Web）
- Spring Security（AuthN/AuthZ）
- OpenTelemetry（トレース/メトリクス/ログ相関）
- OpenAPI 3.0（契約ファースト）

## Data
- PostgreSQL（RDB。履歴・監査・整合性を学べる）
- Redis（任意：キャッシュ/レート制限/分散ロック）

## Messaging（任意）
- Kafka / RabbitMQ / NATS（どれか1つを選ぶ）
- at-least-once を前提に dedup（重複排除）を設計

## Infra / Delivery（最小）
- Docker（ローカル/CI）
- Kubernetes（基礎：Pod/Deployment/Service/Ingress）
- CI: GitHub Actions
- IaC: Terraform（任意）

## Architectural Constraints
- DDD（Bounded Context / Aggregate / Domain Event）
- Hexagonal（Ports & Adapters）
- 依存方向：domain → adapter/spring は禁止（`docs/plan/file-map.md` 参照）
- API互換性：v1/v2併走を前提（`docs/api/migration/`）

## Security Constraints
- OWASP Top 10 相当の脅威を考慮
- PII / Secrets をログ・トレース・AI出力に含めない（`docs/security/*`）
- サプライチェーン：SBOM/CVEゲートを後続で導入（`docs/security/sbom-cve-ops.md`）
