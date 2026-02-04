---
doc_type: "template"
id: "pr-template"
version: "1.0"
last_updated: "2026-02-04"
status: "stable"
---

# Pull Request Template

このテンプレートは `.github/pull_request_template.md` と同期されています。
PRを作成する際は、このフォーマットに従ってください。

---

## テンプレート構造

### 1. Summary（必須）

変更内容を1-3個の箇条書きで要約します。

```markdown
## Summary

- Add `RefreshToken` entity per iam.md section 3.2 specification
- Add `HashedToken` value object for SHA-256 token hashes
- Update task list: IAM-D-06 marked as completed
```

### 2. Changes（必須）

#### New Files（新規ファイルがある場合）

```markdown
### New Files

| File | Description |
|------|-------------|
| `domain/.../HashedToken.java` | SHA-256 token hash value object |
| `domain/.../RefreshToken.java` | Refresh token entity |
```

#### Modified Files（変更ファイルがある場合）

```markdown
### Modified Files

| File | Change |
|------|--------|
| `docs/tasks/implementation-slice-a.md` | Mark IAM-D-06 as completed |
```

#### Key Implementation Details（実装の詳細）

重要な実装の詳細を説明します：
- コード構造とデザインパターン
- 主要なクラス/インターフェースの説明（コードスニペット付き）
- 設定変更

```markdown
### Key Implementation Details

#### DomainEvent Interface

Base interface providing common properties:

\`\`\`java
public interface DomainEvent {
    UUID eventId();        // Unique event identifier
    UUID aggregateId();    // Source aggregate identifier
    Instant occurredAt();  // When the event occurred
}
\`\`\`

#### IP Address Masking

| Type | Input | Output |
|------|-------|--------|
| IPv4 | `192.168.1.100` | `192.168.1.***` |
| IPv6 | `2001:0db8:...` | `2001:0db8:85a3:***` |
```

### 3. Design Decisions（設計判断がある場合）

なぜその設計を選んだかを説明します：

```markdown
## Design Decisions

1. **Record-based immutable event** - Events are facts; immutability ensures integrity
2. **IP masking at creation time** - Privacy by design; masked value stored
3. **Separate `of()` factory** - Allows reconstruction from persistence
```

### 4. Test Coverage（必須）

テスト内容をリストします：

```markdown
## Test Coverage

- Factory methods (create, of)
- Validation (null checks, format validation)
- Domain behavior (business logic)
- Value object semantics (equals, hashCode)
- Edge cases
```

### 5. Test plan（必須）

検証チェックリスト：

```markdown
## Test plan

- [x] `HashedTokenTest` - 15 test cases
- [x] `RefreshTokenTest` - 20 test cases
- [x] All tests pass (`./gradlew :domain:test`)
```

### 6. Related（必須）

関連タスクと仕様へのリンク：

```markdown
## Related

- Task: IAM-D-06
- Spec: `docs/design/contexts/iam.md` section 3.2
```

---

## 記述ガイドライン

### Summary
- **簡潔に**：1-3個の箇条書き
- **What + Why**：何を変更し、なぜ必要か
- **動詞で始める**：Add, Implement, Fix, Update, Remove

### Changes
- **ファイルパスは省略形OK**：`domain/.../model/User.java`
- **コードスニペット**：重要な構造を示す場合に使用
- **テーブル形式**：ファイル一覧には表を使用

### Design Decisions
- **番号付きリスト**：決定事項を明確に
- **太字でタイトル**：決定内容を強調
- **理由を説明**：なぜその選択をしたか

### Test Coverage / Test plan
- **具体的に**：テストケース数、カバー範囲
- **チェックボックス**：完了状態を示す
- **コマンドを記載**：実行方法を明示

---

## PRタイトル形式

```
<type>(<scope>): <summary>
```

### Type
| type | 用途 |
|------|------|
| `feat` | 新機能 |
| `fix` | バグ修正 |
| `docs` | ドキュメントのみ |
| `refactor` | リファクタリング |
| `test` | テスト追加・修正 |
| `chore` | 設定・ツール変更 |

### 例
- `feat(iam): add RefreshToken entity`
- `fix(booking): resolve time range overlap detection`
- `docs: add observability design document`
- `refactor(payment): extract Money value object`

---

## 参照

- `docs/agent/rules.md` - PR作成ルール
- `.agent/workflows/git-commands.md` - Git操作ワークフロー
- `.github/pull_request_template.md` - GitHubテンプレート（本ファイルと同期）
