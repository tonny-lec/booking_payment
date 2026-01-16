# 未完了タスク: セキュリティ関連

## 概要
- 対象フォルダ: `docs/security/`, `docs/design/security.md`
- 状態: 全てスケルトン状態
- 優先度: **中**

---

## タスク一覧

### SEC-1: セキュリティ設計の具体化
- ファイル: `docs/design/security.md`
- Slice: A
- 優先度: 中
- 現状: SSOT参照のみ
- 未完了セクション:
  - [ ] 認証設計
    - JWT構造（header, payload, signature）
    - トークン有効期限設定
    - 署名アルゴリズム（RS256推奨）
  - [ ] 認可設計
    - RBAC/ABACの方針
    - リソース所有権チェック
  - [ ] セッション管理
    - RefreshTokenローテーション
    - 同時ログイン制限（検討）
  - [ ] 入力検証
    - バリデーションルール
    - サニタイズ方針

### SEC-2: 脅威モデルの具体化
- ファイル: `docs/security/threat-model.md`
- Slice: B
- 優先度: 中
- 現状: スケルトン
- 未完了セクション:
  - [ ] 資産の特定
  - [ ] 脅威の特定（STRIDE）
  - [ ] 脅威への対策
  - [ ] リスク評価

### SEC-3: PIIポリシーの具体化
- ファイル: `docs/security/pii-policy.md`
- Slice: A
- 優先度: 高
- 現状: スケルトン
- 未完了セクション:
  - [ ] PII定義（メールアドレス、IPアドレス等）
  - [ ] マスキングルール
    - メール: `u***@example.com`
    - IP: `192.168.1.***`
  - [ ] ログ出力禁止項目（パスワード、カード情報）
  - [ ] 保持期間と削除方針

### SEC-4: シークレット管理の具体化
- ファイル: `docs/security/secrets.md`
- Slice: A
- 優先度: 中
- 現状: スケルトン
- 未完了セクション:
  - [ ] シークレット一覧
    - DB接続情報
    - JWT署名鍵
    - 外部API鍵
  - [ ] 管理方針
    - 環境変数 vs Vault
    - ローテーション方針
  - [ ] 漏洩時の対応手順

### SEC-5: SBOM/CVE運用の具体化
- ファイル: `docs/security/sbom-cve-ops.md`
- Slice: C
- 優先度: 低
- 現状: スケルトン
- 未完了セクション:
  - [ ] SBOM生成方針
  - [ ] CVEスキャン設定
  - [ ] 脆弱性対応フロー

---

## 完了条件（DoD）
- OWASP Top 10相当の脅威を考慮
- PIIマスキングルールが明確
- シークレット管理方針が定義
- `docs/agent/rules.md` と整合（No Secrets / No PII）
