---
doc_type: "plan"
id: "file-map"
version: "0.2"
last_updated: "2026-01-16"
status: "draft"
---

# File Map（DDD + Hexagonal）

## 例（モジュール別）
```text
src/main/java/.../<module>/
  domain/
  application/
  adapter/in/
  adapter/out/
  config/
```

## 禁止依存
- domain → adapter
- domain → spring
