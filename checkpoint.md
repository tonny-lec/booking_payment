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
