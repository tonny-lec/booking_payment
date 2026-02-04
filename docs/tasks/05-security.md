# 未完了タスク: セキュリティ関連

## 概要
- 対象フォルダ: `docs/security/`, `docs/design/security.md`
- 状態: 4完了 / 1未完了（SBOM/CVE）
- 優先度: **中**
- 関連: [by-feature.md](./by-feature.md)

---

## タスク一覧

<a id="SEC-1"></a>
### SEC-1: セキュリティ設計の具体化
- ファイル: `docs/design/security.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-IAM)

<a id="SEC-2"></a>
### SEC-2: 脅威モデルの具体化
- ファイル: `docs/security/threat-model.md`
- Slice: B
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-CROSS-SEC)

<a id="SEC-3"></a>
### SEC-3: PIIポリシーの具体化
- ファイル: `docs/security/pii-policy.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-CROSS-SEC)

<a id="SEC-4"></a>
### SEC-4: シークレット管理の具体化
- ファイル: `docs/security/secrets.md`
- Slice: A
- 状態: ✅ 完了
- 関連: [by-feature](./by-feature.md#BF-CROSS-SEC)

<a id="SEC-5"></a>
### SEC-5: SBOM/CVE運用の具体化
- ファイル: `docs/security/sbom-cve-ops.md`
- Slice: C
- 状態: ⬜ 未着手
- 関連: [by-feature](./by-feature.md#BF-CROSS-SEC)

未完了セクション:
- [ ] SBOM生成方針
- [ ] CVEスキャン設定
- [ ] 脆弱性対応フロー

---

## 完了条件（DoD）
- OWASP Top 10相当の脅威を考慮
- PIIマスキングルールが明確
- シークレット管理方針が定義
- `docs/agent/rules.md` と整合（No Secrets / No PII）
