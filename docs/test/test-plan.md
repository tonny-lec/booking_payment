---
doc_type: "test_plan"
id: "test-plan"
version: "0.3"
last_updated: "2026-01-18"
status: "draft"
---

# テスト計画

## 1. テスト戦略

### 1.1 テストピラミッド

```
         ┌─────────────┐
         │   E2E Test  │  ← 少数：重要フロー
         │    (10%)    │
         ├─────────────┤
         │ Integration │  ← 中程度：外部依存
         │    (20%)    │
         ├─────────────┤
         │  Unit Test  │  ← 多数：ビジネスロジック
         │    (70%)    │
         └─────────────┘
```

### 1.2 重点テスト領域

- 二重予約、境界時間
- 冪等性欠落による二重課金
- タイムアウト/リトライでの重複処理
- 権限チェック漏れ
- 互換性破壊
- traceId欠落

### 1.3 Property-Based Testing 候補

- TimeRange（start < end）
- Idempotency（同一入力→同一出力）
- 状態遷移（許可されない遷移は拒否）

---

## 2. Booking テスト計画

### 2.1 TimeRange値オブジェクト（BK-TEST-01）

TimeRange値オブジェクトの不変条件と振る舞いのユニットテスト。

#### テストケース一覧

| ID | テストケース | 入力 | 期待結果 |
|----|-------------|------|----------|
| TR-001 | 有効なTimeRange作成 | start=10:00, end=11:00 | 正常作成 |
| TR-002 | 無効：start >= end | start=11:00, end=10:00 | IllegalArgumentException |
| TR-003 | 無効：start == end | start=10:00, end=10:00 | IllegalArgumentException |
| TR-004 | 無効：過去のstart | start=昨日, end=明日 | IllegalArgumentException |
| TR-005 | 境界：現在時刻のstart | start=now, end=now+1h | 正常作成（許容） |
| TR-006 | 重複判定：完全重複 | A=[10:00-12:00], B=[10:00-12:00] | overlaps=true |
| TR-007 | 重複判定：部分重複（前半） | A=[10:00-12:00], B=[09:00-11:00] | overlaps=true |
| TR-008 | 重複判定：部分重複（後半） | A=[10:00-12:00], B=[11:00-13:00] | overlaps=true |
| TR-009 | 重複判定：包含（AがBを含む） | A=[09:00-13:00], B=[10:00-12:00] | overlaps=true |
| TR-010 | 重複判定：包含（BがAを含む） | A=[10:00-12:00], B=[09:00-13:00] | overlaps=true |
| TR-011 | 重複判定：隣接（衝突しない） | A=[10:00-11:00], B=[11:00-12:00] | overlaps=false |
| TR-012 | 重複判定：離散（衝突しない） | A=[10:00-11:00], B=[12:00-13:00] | overlaps=false |
| TR-013 | duration計算 | start=10:00, end=12:30 | 2時間30分 |
| TR-014 | contains判定：範囲内 | range=[10:00-12:00], point=11:00 | true |
| TR-015 | contains判定：範囲外 | range=[10:00-12:00], point=13:00 | false |
| TR-016 | contains判定：境界（start） | range=[10:00-12:00], point=10:00 | true |
| TR-017 | contains判定：境界（end） | range=[10:00-12:00], point=12:00 | false（半開区間） |

#### 実装例

```java
@Nested
@DisplayName("TimeRange Unit Tests (BK-TEST-01)")
class TimeRangeTest {

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T09:00:00Z"),
            ZoneOffset.UTC
        );
    }

    @Test
    @DisplayName("TR-001: 有効なTimeRangeを作成できる")
    void validTimeRange_IsCreated() {
        Instant start = Instant.parse("2026-01-18T10:00:00Z");
        Instant end = Instant.parse("2026-01-18T11:00:00Z");

        TimeRange range = TimeRange.of(start, end, fixedClock);

        assertThat(range.startAt()).isEqualTo(start);
        assertThat(range.endAt()).isEqualTo(end);
    }

    @Test
    @DisplayName("TR-002: start >= end は IllegalArgumentException をスローする")
    void invalidTimeRange_StartAfterEnd_ThrowsException() {
        Instant start = Instant.parse("2026-01-18T11:00:00Z");
        Instant end = Instant.parse("2026-01-18T10:00:00Z");

        assertThatThrownBy(() -> TimeRange.of(start, end, fixedClock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("start must be before end");
    }

    @Test
    @DisplayName("TR-004: 過去のstartは IllegalArgumentException をスローする")
    void pastTimeRange_ThrowsException() {
        Instant start = Instant.parse("2026-01-17T10:00:00Z"); // 昨日
        Instant end = Instant.parse("2026-01-18T10:00:00Z");

        assertThatThrownBy(() -> TimeRange.of(start, end, fixedClock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("start must not be in the past");
    }

    @Test
    @DisplayName("TR-011: 隣接するTimeRangeは重複しない（半開区間）")
    void adjacentRanges_DoNotOverlap() {
        TimeRange a = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        TimeRange b = TimeRange.of(
            Instant.parse("2026-01-18T11:00:00Z"),
            Instant.parse("2026-01-18T12:00:00Z"),
            fixedClock
        );

        assertThat(a.overlaps(b)).isFalse();
        assertThat(b.overlaps(a)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "10:00, 12:00, 09:00, 11:00, true",  // TR-007: 部分重複（前半）
        "10:00, 12:00, 11:00, 13:00, true",  // TR-008: 部分重複（後半）
        "09:00, 13:00, 10:00, 12:00, true",  // TR-009: 包含（AがBを含む）
        "10:00, 12:00, 09:00, 13:00, true",  // TR-010: 包含（BがAを含む）
        "10:00, 11:00, 11:00, 12:00, false", // TR-011: 隣接
        "10:00, 11:00, 12:00, 13:00, false"  // TR-012: 離散
    })
    @DisplayName("TimeRange重複判定のパラメタライズドテスト")
    void overlapsParameterized(
        String aStart, String aEnd,
        String bStart, String bEnd,
        boolean expectedOverlap
    ) {
        String datePrefix = "2026-01-18T";
        TimeRange a = TimeRange.of(
            Instant.parse(datePrefix + aStart + ":00Z"),
            Instant.parse(datePrefix + aEnd + ":00Z"),
            fixedClock
        );
        TimeRange b = TimeRange.of(
            Instant.parse(datePrefix + bStart + ":00Z"),
            Instant.parse(datePrefix + bEnd + ":00Z"),
            fixedClock
        );

        assertThat(a.overlaps(b)).isEqualTo(expectedOverlap);
    }

    @Test
    @DisplayName("TR-013: durationが正しく計算される")
    void duration_IsCalculatedCorrectly() {
        TimeRange range = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T12:30:00Z"),
            fixedClock
        );

        assertThat(range.duration()).isEqualTo(Duration.ofHours(2).plusMinutes(30));
    }
}
```

### 2.2 Booking集約（BK-TEST-02）

Booking集約のドメインロジックと状態遷移のユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| BK-001 | 予約作成（正常） | 有効な入力 | PENDING状態で作成 + BookingCreatedイベント |
| BK-002 | 予約作成：note付き | note指定あり | noteが保存される |
| BK-003 | 予約作成：note最大長 | 500文字のnote | 正常保存 |
| BK-004 | 予約作成：note超過 | 501文字のnote | IllegalArgumentException |
| BK-005 | 時間更新（正常） | PENDING状態 + 正しいversion | 更新成功 + BookingUpdatedイベント |
| BK-006 | 時間更新：バージョン不一致 | expectedVersion != 現在version | OptimisticLockException |
| BK-007 | 時間更新：CONFIRMED状態 | status=CONFIRMED | IllegalStateException |
| BK-008 | 時間更新：CANCELLED状態 | status=CANCELLED | IllegalStateException |
| BK-009 | 確定（正常） | PENDING → CONFIRMED | 遷移成功 + BookingConfirmedイベント |
| BK-010 | 確定：既にCONFIRMED | status=CONFIRMED | IllegalStateException |
| BK-011 | キャンセル（PENDING） | PENDING → CANCELLED | 遷移成功 + BookingCancelledイベント |
| BK-012 | キャンセル（CONFIRMED） | CONFIRMED → CANCELLED | 遷移成功 + BookingCancelledイベント |
| BK-013 | キャンセル：既にCANCELLED | status=CANCELLED | IllegalStateException |
| BK-014 | キャンセル理由保存 | reason指定 | cancelReasonが保存される |
| BK-015 | versionインクリメント | 更新操作 | version + 1 |

#### 実装例

```java
@Nested
@DisplayName("Booking Aggregate Unit Tests (BK-TEST-02)")
class BookingAggregateTest {

    private Clock fixedClock;
    private UserId userId;
    private ResourceId resourceId;
    private TimeRange validTimeRange;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T09:00:00Z"),
            ZoneOffset.UTC
        );
        userId = UserId.of("550e8400-e29b-41d4-a716-446655440000");
        resourceId = ResourceId.of("resource-001");
        validTimeRange = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
    }

    @Test
    @DisplayName("BK-001: 予約がPENDING状態で作成される")
    void create_CreatesBookingInPendingStatus() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getUserId()).isEqualTo(userId);
        assertThat(booking.getResourceId()).isEqualTo(resourceId);
        assertThat(booking.getTimeRange()).isEqualTo(validTimeRange);
        assertThat(booking.getVersion()).isEqualTo(1);
        assertThat(booking.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(BookingCreated.class);
    }

    @Test
    @DisplayName("BK-004: noteが500文字を超えるとIllegalArgumentExceptionをスローする")
    void create_WithNoteTooLong_ThrowsException() {
        String longNote = "a".repeat(501);

        assertThatThrownBy(() ->
            Booking.create(userId, resourceId, validTimeRange, longNote, fixedClock)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("note must not exceed 500 characters");
    }

    @Test
    @DisplayName("BK-005: PENDING状態で時間更新が成功する")
    void updateTimeRange_WhenPending_Succeeds() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );
        booking.clearEvents();

        TimeRange newTimeRange = TimeRange.of(
            Instant.parse("2026-01-18T14:00:00Z"),
            Instant.parse("2026-01-18T15:00:00Z"),
            fixedClock
        );

        booking.updateTimeRange(newTimeRange, 1, fixedClock);

        assertThat(booking.getTimeRange()).isEqualTo(newTimeRange);
        assertThat(booking.getVersion()).isEqualTo(2);
        assertThat(booking.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(BookingUpdated.class);
    }

    @Test
    @DisplayName("BK-006: バージョン不一致でOptimisticLockExceptionをスローする")
    void updateTimeRange_WithVersionMismatch_ThrowsException() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );

        TimeRange newTimeRange = TimeRange.of(
            Instant.parse("2026-01-18T14:00:00Z"),
            Instant.parse("2026-01-18T15:00:00Z"),
            fixedClock
        );

        assertThatThrownBy(() ->
            booking.updateTimeRange(newTimeRange, 0, fixedClock) // 間違ったversion
        ).isInstanceOf(OptimisticLockException.class)
         .hasMessageContaining("version mismatch");
    }

    @Test
    @DisplayName("BK-009: PENDING状態からCONFIRMEDに遷移できる")
    void confirm_WhenPending_Succeeds() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );
        booking.clearEvents();

        booking.confirm(fixedClock);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(BookingConfirmed.class);
    }

    @Test
    @DisplayName("BK-011: PENDING状態からキャンセルできる")
    void cancel_WhenPending_Succeeds() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );
        booking.clearEvents();

        booking.cancel("User requested", fixedClock);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelReason()).isEqualTo("User requested");
        assertThat(booking.getCancelledAt()).isNotNull();
        assertThat(booking.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(BookingCancelled.class);
    }

    @Test
    @DisplayName("BK-012: CONFIRMED状態からキャンセルできる")
    void cancel_WhenConfirmed_Succeeds() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );
        booking.confirm(fixedClock);
        booking.clearEvents();

        booking.cancel("Schedule change", fixedClock);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        BookingCancelled event = (BookingCancelled) booking.getDomainEvents().get(0);
        assertThat(event.getPayload().previousStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("BK-013: CANCELLED状態からはキャンセルできない")
    void cancel_WhenAlreadyCancelled_ThrowsException() {
        Booking booking = Booking.create(
            userId, resourceId, validTimeRange, null, fixedClock
        );
        booking.cancel("First cancel", fixedClock);

        assertThatThrownBy(() ->
            booking.cancel("Second cancel", fixedClock)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("already cancelled");
    }
}
```

### 2.3 ConflictDetector（BK-TEST-03）

予約衝突検出ロジックのユニットテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| CD-001 | 衝突なし：予約なし | 既存予約0件 | conflict=false |
| CD-002 | 衝突なし：別リソース | 同時間帯だが別resource_id | conflict=false |
| CD-003 | 衝突なし：隣接 | A.endAt == B.startAt | conflict=false |
| CD-004 | 衝突あり：完全重複（PENDING） | 同一時間帯 + status=PENDING | conflict=true |
| CD-005 | 衝突あり：完全重複（CONFIRMED） | 同一時間帯 + status=CONFIRMED | conflict=true |
| CD-006 | 衝突なし：CANCELLED予約 | 同一時間帯 + status=CANCELLED | conflict=false |
| CD-007 | 衝突あり：部分重複 | 時間が一部重なる | conflict=true |
| CD-008 | 自己除外：更新時 | 更新対象予約を除外 | conflict=false |
| CD-009 | 複数衝突 | 複数の既存予約と衝突 | 全衝突をリスト |

#### 実装例

```java
@Nested
@DisplayName("ConflictDetector Unit Tests (BK-TEST-03)")
class ConflictDetectorTest {

    private ConflictDetector detector;
    private BookingRepository mockRepository;
    private Clock fixedClock;
    private ResourceId resourceId;

    @BeforeEach
    void setUp() {
        mockRepository = mock(BookingRepository.class);
        detector = new ConflictDetector(mockRepository);
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T09:00:00Z"),
            ZoneOffset.UTC
        );
        resourceId = ResourceId.of("resource-001");
    }

    @Test
    @DisplayName("CD-001: 既存予約がなければ衝突なし")
    void detectConflict_WithNoExistingBookings_ReturnsNoConflict() {
        TimeRange newRange = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );

        when(mockRepository.findActiveByResourceAndTimeRange(
            eq(resourceId), any(Instant.class), any(Instant.class)
        )).thenReturn(List.of());

        ConflictResult result = detector.detectConflict(resourceId, newRange, null);

        assertThat(result.hasConflict()).isFalse();
        assertThat(result.getConflictingBookings()).isEmpty();
    }

    @Test
    @DisplayName("CD-004: PENDING予約との重複は衝突")
    void detectConflict_WithOverlappingPendingBooking_ReturnsConflict() {
        TimeRange existingRange = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking existingBooking = createBooking(existingRange, BookingStatus.PENDING);

        TimeRange newRange = TimeRange.of(
            Instant.parse("2026-01-18T10:30:00Z"),
            Instant.parse("2026-01-18T11:30:00Z"),
            fixedClock
        );

        when(mockRepository.findActiveByResourceAndTimeRange(
            eq(resourceId), any(Instant.class), any(Instant.class)
        )).thenReturn(List.of(existingBooking));

        ConflictResult result = detector.detectConflict(resourceId, newRange, null);

        assertThat(result.hasConflict()).isTrue();
        assertThat(result.getConflictingBookings()).containsExactly(existingBooking);
    }

    @Test
    @DisplayName("CD-003: 隣接する予約は衝突しない")
    void detectConflict_WithAdjacentBooking_ReturnsNoConflict() {
        TimeRange existingRange = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking existingBooking = createBooking(existingRange, BookingStatus.CONFIRMED);

        TimeRange newRange = TimeRange.of(
            Instant.parse("2026-01-18T11:00:00Z"), // 既存の終了時刻と一致
            Instant.parse("2026-01-18T12:00:00Z"),
            fixedClock
        );

        when(mockRepository.findActiveByResourceAndTimeRange(
            eq(resourceId), any(Instant.class), any(Instant.class)
        )).thenReturn(List.of()); // 重複検出クエリで除外済み

        ConflictResult result = detector.detectConflict(resourceId, newRange, null);

        assertThat(result.hasConflict()).isFalse();
    }

    @Test
    @DisplayName("CD-008: 更新時は自身を除外する")
    void detectConflict_WhenUpdating_ExcludesSelf() {
        BookingId bookingId = BookingId.of("booking-001");
        TimeRange existingRange = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking existingBooking = createBookingWithId(bookingId, existingRange, BookingStatus.PENDING);

        TimeRange newRange = TimeRange.of(
            Instant.parse("2026-01-18T10:30:00Z"),
            Instant.parse("2026-01-18T11:30:00Z"),
            fixedClock
        );

        when(mockRepository.findActiveByResourceAndTimeRange(
            eq(resourceId), any(Instant.class), any(Instant.class)
        )).thenReturn(List.of(existingBooking));

        // 自身のIDを除外パラメータとして渡す
        ConflictResult result = detector.detectConflict(resourceId, newRange, bookingId);

        assertThat(result.hasConflict()).isFalse();
    }

    private Booking createBooking(TimeRange range, BookingStatus status) {
        return createBookingWithId(BookingId.generate(), range, status);
    }

    private Booking createBookingWithId(BookingId id, TimeRange range, BookingStatus status) {
        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(id);
        when(booking.getResourceId()).thenReturn(resourceId);
        when(booking.getTimeRange()).thenReturn(range);
        when(booking.getStatus()).thenReturn(status);
        return booking;
    }
}
```

### 2.4 BookingRepository 統合テスト（BK-TEST-04）

データベースとの統合テスト。

#### テストケース一覧

| ID | テストケース | 操作 | 期待結果 |
|----|-------------|------|----------|
| BR-001 | 予約保存 | save(booking) | DBに永続化 |
| BR-002 | ID検索 | findById(existingId) | Optional.of(booking) |
| BR-003 | ユーザー検索 | findByUserId(userId) | ユーザーの予約一覧 |
| BR-004 | 衝突検索：重複あり | findActiveByResourceAndTimeRange | 重複予約を返却 |
| BR-005 | 衝突検索：CANCELLED除外 | status=CANCELLED | 結果に含まれない |
| BR-006 | 楽観的ロック成功 | save(updatedBooking) | 保存成功 + version++ |
| BR-007 | 楽観的ロック失敗 | 競合更新 | OptimisticLockingFailureException |
| BR-008 | ステータスフィルタ | findByUserIdAndStatus | 指定ステータスのみ |

#### 実装例

```java
@SpringBootTest
@Transactional
@DisplayName("BookingRepository Integration Tests (BK-TEST-04)")
class BookingRepositoryIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Clock fixedClock;
    private UserId userId;
    private ResourceId resourceId;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T09:00:00Z"),
            ZoneOffset.UTC
        );
        userId = UserId.of("user-001");
        resourceId = ResourceId.of("resource-001");
    }

    @Test
    @DisplayName("BR-001: 予約をDBに保存できる")
    void save_PersistsBookingToDatabase() {
        TimeRange range = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking booking = Booking.create(userId, resourceId, range, "Test note", fixedClock);

        Booking saved = bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNote()).isEqualTo("Test note");
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("BR-004: 重複する予約を検索できる")
    void findActiveByResourceAndTimeRange_ReturnsOverlappingBookings() {
        // 既存予約: 10:00-11:00
        TimeRange existingRange = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking existingBooking = Booking.create(userId, resourceId, existingRange, null, fixedClock);
        bookingRepository.save(existingBooking);
        entityManager.flush();
        entityManager.clear();

        // 10:30-11:30 で検索（重複する）
        List<Booking> conflicts = bookingRepository.findActiveByResourceAndTimeRange(
            resourceId,
            Instant.parse("2026-01-18T10:30:00Z"),
            Instant.parse("2026-01-18T11:30:00Z")
        );

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(existingBooking.getId());
    }

    @Test
    @DisplayName("BR-005: CANCELLEDの予約は衝突検索から除外される")
    void findActiveByResourceAndTimeRange_ExcludesCancelledBookings() {
        TimeRange range = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking cancelledBooking = Booking.create(userId, resourceId, range, null, fixedClock);
        cancelledBooking.cancel("Test cancel", fixedClock);
        bookingRepository.save(cancelledBooking);
        entityManager.flush();
        entityManager.clear();

        List<Booking> conflicts = bookingRepository.findActiveByResourceAndTimeRange(
            resourceId,
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z")
        );

        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("BR-007: 楽観的ロック競合でOptimisticLockingFailureExceptionをスローする")
    void save_WithVersionConflict_ThrowsException() {
        TimeRange range = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking booking = Booking.create(userId, resourceId, range, null, fixedClock);
        Booking saved = bookingRepository.save(booking);
        entityManager.flush();

        // 別トランザクションで更新をシミュレート
        entityManager.getEntityManager()
            .createNativeQuery("UPDATE bookings SET version = version + 1 WHERE id = :id")
            .setParameter("id", saved.getId().value())
            .executeUpdate();

        TimeRange newRange = TimeRange.of(
            Instant.parse("2026-01-18T14:00:00Z"),
            Instant.parse("2026-01-18T15:00:00Z"),
            fixedClock
        );
        saved.updateTimeRange(newRange, 1, fixedClock);

        assertThatThrownBy(() -> {
            bookingRepository.save(saved);
            entityManager.flush();
        }).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
```

### 2.5 境界値テスト：TimeRange境界（BK-TEST-05）

隣接予約と境界条件の特化テスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| BND-001 | 隣接予約：A.end == B.start | 連続予約 | 両方作成可能 |
| BND-002 | 1ms重複 | A.end = B.start + 1ms | 衝突 |
| BND-003 | 同一時刻開始 | A.start == B.start | 衝突 |
| BND-004 | 同一時刻終了 | A.end == B.end | 衝突（start次第） |
| BND-005 | 最小期間予約 | duration=1分 | 作成可能 |
| BND-006 | 日跨ぎ予約 | 23:00-01:00（翌日） | 作成可能 |
| BND-007 | 深夜0時境界 | 00:00-01:00 | 正常動作 |
| BND-008 | 夏時間境界（該当地域） | DST切り替え時刻 | 正しく処理 |

#### 実装例

```java
@Nested
@DisplayName("Boundary Value Tests for TimeRange (BK-TEST-05)")
class TimeRangeBoundaryTest {

    private Clock fixedClock;
    private ConflictDetector detector;
    private BookingRepository mockRepository;
    private ResourceId resourceId;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
            Instant.parse("2026-01-18T09:00:00Z"),
            ZoneOffset.UTC
        );
        mockRepository = mock(BookingRepository.class);
        detector = new ConflictDetector(mockRepository);
        resourceId = ResourceId.of("resource-001");
    }

    @Test
    @DisplayName("BND-001: 隣接する予約は両方作成可能")
    void adjacentBookings_CanBothBeCreated() {
        // 既存予約: 10:00-11:00
        TimeRange existing = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            fixedClock
        );
        Booking existingBooking = createBooking(existing, BookingStatus.CONFIRMED);

        // 新規予約: 11:00-12:00（隣接）
        TimeRange adjacent = TimeRange.of(
            Instant.parse("2026-01-18T11:00:00Z"),
            Instant.parse("2026-01-18T12:00:00Z"),
            fixedClock
        );

        when(mockRepository.findActiveByResourceAndTimeRange(
            eq(resourceId), any(), any()
        )).thenReturn(List.of()); // 重複なし

        ConflictResult result = detector.detectConflict(resourceId, adjacent, null);

        assertThat(result.hasConflict()).isFalse();
    }

    @Test
    @DisplayName("BND-002: 1ミリ秒でも重複すれば衝突")
    void oneMillisecondOverlap_IsConflict() {
        // 既存予約: 10:00:00.000-11:00:00.000
        TimeRange existing = TimeRange.of(
            Instant.parse("2026-01-18T10:00:00.000Z"),
            Instant.parse("2026-01-18T11:00:00.000Z"),
            fixedClock
        );
        Booking existingBooking = createBooking(existing, BookingStatus.CONFIRMED);

        // 新規予約: 10:59:59.999-12:00:00（1ms重複）
        TimeRange overlapping = TimeRange.of(
            Instant.parse("2026-01-18T10:59:59.999Z"),
            Instant.parse("2026-01-18T12:00:00.000Z"),
            fixedClock
        );

        when(mockRepository.findActiveByResourceAndTimeRange(
            eq(resourceId), any(), any()
        )).thenReturn(List.of(existingBooking));

        ConflictResult result = detector.detectConflict(resourceId, overlapping, null);

        assertThat(result.hasConflict()).isTrue();
    }

    @Test
    @DisplayName("BND-006: 日跨ぎ予約が正しく処理される")
    void overnightBooking_IsProcessedCorrectly() {
        // 23:00-翌01:00の予約
        TimeRange overnight = TimeRange.of(
            Instant.parse("2026-01-18T23:00:00Z"),
            Instant.parse("2026-01-19T01:00:00Z"),
            fixedClock
        );

        assertThat(overnight.startAt()).isBefore(overnight.endAt());
        assertThat(overnight.duration()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    @DisplayName("BND-007: 深夜0時開始の予約が正しく処理される")
    void midnightStartBooking_IsProcessedCorrectly() {
        TimeRange midnight = TimeRange.of(
            Instant.parse("2026-01-19T00:00:00Z"),
            Instant.parse("2026-01-19T01:00:00Z"),
            fixedClock
        );

        assertThat(midnight.duration()).isEqualTo(Duration.ofHours(1));
    }

    private Booking createBooking(TimeRange range, BookingStatus status) {
        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(BookingId.generate());
        when(booking.getResourceId()).thenReturn(resourceId);
        when(booking.getTimeRange()).thenReturn(range);
        when(booking.getStatus()).thenReturn(status);
        return booking;
    }
}
```

### 2.6 E2E テスト：create→update→cancel フロー（BK-TEST-06）

予約ライフサイクル全体のE2Eテスト。

#### テストシナリオ

```
シナリオ: 予約の作成→更新→キャンセル
  Given ログイン済みユーザーがいる
  When POST /bookings で予約を作成する
  Then 201 Created + PENDING状態の予約

  When PUT /bookings/{id} で時間を変更する
  Then 200 OK + 更新された予約

  When DELETE /bookings/{id} でキャンセルする
  Then 200 OK + CANCELLED状態

  When 同じIDで再度操作を試みる
  Then 409 Conflict（キャンセル済み）
```

#### テストケース一覧

| ID | テストケース | 期待結果 |
|----|-------------|----------|
| E2E-BK-001 | 正常フロー：create→update→cancel | 各ステップで期待レスポンス |
| E2E-BK-002 | 衝突エラー：既存予約と重複 | 409 Conflict |
| E2E-BK-003 | バージョンエラー：同時更新 | 409 Conflict + version_mismatch |
| E2E-BK-004 | 予約確定フロー | PENDING→CONFIRMED |
| E2E-BK-005 | 確定済み予約のキャンセル | CONFIRMED→CANCELLED + 返金トリガー |
| E2E-BK-006 | 一覧取得 | GET /bookings でユーザーの予約一覧 |
| E2E-BK-007 | 詳細取得 | GET /bookings/{id} で予約詳細 |

#### 実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Booking E2E Tests (BK-TEST-06)")
class BookingE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    private String accessToken;
    private UserId testUserId;

    @BeforeEach
    void setUp() {
        // テストユーザーでログインしてアクセストークンを取得
        accessToken = authenticateAndGetToken("test@example.com", "Password123!");
        testUserId = UserId.of("test-user-id");
    }

    @Test
    @DisplayName("E2E-BK-001: 予約の作成→更新→キャンセルフロー")
    void fullBookingLifecycle_Succeeds() {
        // Step 1: 予約作成
        CreateBookingRequest createRequest = new CreateBookingRequest(
            "resource-001",
            "2026-01-20T10:00:00Z",
            "2026-01-20T11:00:00Z",
            "Meeting room booking"
        );

        ResponseEntity<BookingResponse> createResponse = restTemplate.exchange(
            "/bookings",
            HttpMethod.POST,
            createRequest(createRequest, accessToken),
            BookingResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BookingResponse createdBooking = createResponse.getBody();
        assertThat(createdBooking.status()).isEqualTo("PENDING");
        assertThat(createdBooking.version()).isEqualTo(1);
        String bookingId = createdBooking.id();

        // Step 2: 時間変更
        UpdateBookingRequest updateRequest = new UpdateBookingRequest(
            "2026-01-20T14:00:00Z",
            "2026-01-20T15:00:00Z",
            "Updated meeting",
            1 // expectedVersion
        );

        ResponseEntity<BookingResponse> updateResponse = restTemplate.exchange(
            "/bookings/" + bookingId,
            HttpMethod.PUT,
            createRequest(updateRequest, accessToken),
            BookingResponse.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookingResponse updatedBooking = updateResponse.getBody();
        assertThat(updatedBooking.startAt()).isEqualTo("2026-01-20T14:00:00Z");
        assertThat(updatedBooking.version()).isEqualTo(2);

        // Step 3: キャンセル
        CancelBookingRequest cancelRequest = new CancelBookingRequest("No longer needed");

        ResponseEntity<BookingResponse> cancelResponse = restTemplate.exchange(
            "/bookings/" + bookingId,
            HttpMethod.DELETE,
            createRequest(cancelRequest, accessToken),
            BookingResponse.class
        );

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody().status()).isEqualTo("CANCELLED");

        // Step 4: キャンセル済み予約の再キャンセル（失敗するべき）
        ResponseEntity<ErrorResponse> reCancel = restTemplate.exchange(
            "/bookings/" + bookingId,
            HttpMethod.DELETE,
            createRequest(cancelRequest, accessToken),
            ErrorResponse.class
        );

        assertThat(reCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reCancel.getBody().error()).isEqualTo("already_cancelled");
    }

    @Test
    @DisplayName("E2E-BK-002: 既存予約との重複で409 Conflictを返す")
    void createBooking_WithConflict_Returns409() {
        // 既存予約を作成
        CreateBookingRequest firstBooking = new CreateBookingRequest(
            "resource-001",
            "2026-01-20T10:00:00Z",
            "2026-01-20T11:00:00Z",
            null
        );
        restTemplate.exchange(
            "/bookings",
            HttpMethod.POST,
            createRequest(firstBooking, accessToken),
            BookingResponse.class
        );

        // 重複する予約を試みる
        CreateBookingRequest conflictingBooking = new CreateBookingRequest(
            "resource-001",
            "2026-01-20T10:30:00Z",
            "2026-01-20T11:30:00Z",
            null
        );

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings",
            HttpMethod.POST,
            createRequest(conflictingBooking, accessToken),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("time_range_conflict");
    }

    @Test
    @DisplayName("E2E-BK-003: バージョン不一致で409 Conflictを返す")
    void updateBooking_WithVersionMismatch_Returns409() {
        // 予約作成
        CreateBookingRequest createRequest = new CreateBookingRequest(
            "resource-001",
            "2026-01-20T10:00:00Z",
            "2026-01-20T11:00:00Z",
            null
        );
        ResponseEntity<BookingResponse> createResponse = restTemplate.exchange(
            "/bookings",
            HttpMethod.POST,
            createRequest(createRequest, accessToken),
            BookingResponse.class
        );
        String bookingId = createResponse.getBody().id();

        // 間違ったバージョンで更新
        UpdateBookingRequest updateRequest = new UpdateBookingRequest(
            "2026-01-20T14:00:00Z",
            "2026-01-20T15:00:00Z",
            null,
            999 // 間違ったバージョン
        );

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings/" + bookingId,
            HttpMethod.PUT,
            createRequest(updateRequest, accessToken),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("version_mismatch");
    }

    private <T> HttpEntity<T> createRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
```

### 2.7 権限テスト：所有者以外のアクセス拒否（BK-TEST-07）

認可チェックのテスト。

#### テストケース一覧

| ID | テストケース | 条件 | 期待結果 |
|----|-------------|------|----------|
| AUTHZ-BK-001 | 所有者による取得 | userId一致 | 200 OK |
| AUTHZ-BK-002 | 所有者による更新 | userId一致 | 200 OK |
| AUTHZ-BK-003 | 所有者によるキャンセル | userId一致 | 200 OK |
| AUTHZ-BK-004 | 他ユーザーによる取得 | userId不一致 | 403 Forbidden |
| AUTHZ-BK-005 | 他ユーザーによる更新 | userId不一致 | 403 Forbidden |
| AUTHZ-BK-006 | 他ユーザーによるキャンセル | userId不一致 | 403 Forbidden |
| AUTHZ-BK-007 | 未認証アクセス | トークンなし | 401 Unauthorized |
| AUTHZ-BK-008 | 管理者アクセス | ADMIN権限 | 200 OK（任意のユーザー予約） |

#### 実装例

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Booking Authorization Tests (BK-TEST-07)")
class BookingAuthorizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TokenGenerator tokenGenerator;

    private String userAToken;
    private String userBToken;
    private String adminToken;
    private Booking userABooking;

    @BeforeEach
    void setUp() {
        // ユーザーAとしてログイン
        userAToken = tokenGenerator.generateAccessToken(
            UserId.of("user-a"),
            List.of("USER")
        );

        // ユーザーBとしてログイン
        userBToken = tokenGenerator.generateAccessToken(
            UserId.of("user-b"),
            List.of("USER")
        );

        // 管理者としてログイン
        adminToken = tokenGenerator.generateAccessToken(
            UserId.of("admin-user"),
            List.of("ADMIN")
        );

        // ユーザーAの予約を作成
        userABooking = createBookingForUser(UserId.of("user-a"));
    }

    @Test
    @DisplayName("AUTHZ-BK-001: 所有者は自分の予約を取得できる")
    void getBooking_AsOwner_ReturnsBooking() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/bookings/" + userABooking.getId().value(),
            HttpMethod.GET,
            request,
            BookingResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(userABooking.getId().value());
    }

    @Test
    @DisplayName("AUTHZ-BK-004: 他ユーザーの予約取得は403を返す")
    void getBooking_AsNonOwner_Returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userBToken); // ユーザーBのトークン
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings/" + userABooking.getId().value(), // ユーザーAの予約
            HttpMethod.GET,
            request,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("access_denied");
    }

    @Test
    @DisplayName("AUTHZ-BK-006: 他ユーザーによるキャンセルは403を返す")
    void cancelBooking_AsNonOwner_Returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userBToken);
        CancelBookingRequest cancelRequest = new CancelBookingRequest("Trying to cancel");
        HttpEntity<CancelBookingRequest> request = new HttpEntity<>(cancelRequest, headers);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/bookings/" + userABooking.getId().value(),
            HttpMethod.DELETE,
            request,
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 予約がキャンセルされていないことを確認
        Booking booking = bookingRepository.findById(userABooking.getId()).orElseThrow();
        assertThat(booking.getStatus()).isNotEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("AUTHZ-BK-007: 未認証アクセスは401を返す")
    void getBooking_WithoutToken_Returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/bookings/" + userABooking.getId().value(),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AUTHZ-BK-008: 管理者は任意のユーザーの予約を取得できる")
    void getBooking_AsAdmin_ReturnsBooking() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/bookings/" + userABooking.getId().value(),
            HttpMethod.GET,
            request,
            BookingResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Booking createBookingForUser(UserId userId) {
        Clock clock = Clock.systemUTC();
        TimeRange range = TimeRange.of(
            Instant.now().plus(Duration.ofDays(1)),
            Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(1)),
            clock
        );
        Booking booking = Booking.create(
            userId,
            ResourceId.of("resource-001"),
            range,
            null,
            clock
        );
        return bookingRepository.save(booking);
    }
}
```

---

## 3. テスト環境

### 3.1 ユニットテスト

- **フレームワーク**: JUnit 5 + AssertJ + Mockito
- **実行**: Gradle `test` タスク
- **カバレッジ目標**: 80%以上（ドメインロジック）

### 3.2 統合テスト

- **フレームワーク**: Spring Boot Test + Testcontainers
- **データベース**: PostgreSQL（Testcontainers）
- **実行**: Gradle `integrationTest` タスク

### 3.3 E2Eテスト

- **フレームワーク**: Spring Boot Test + TestRestTemplate
- **環境**: 完全なアプリケーションコンテキスト
- **実行**: Gradle `e2eTest` タスク

---

## 4. テストデータ管理

### 4.1 テストデータ原則

1. **独立性**: 各テストは独自のテストデータを持つ
2. **再現性**: テストデータは固定値または明示的なシード
3. **クリーンアップ**: テスト終了後にデータをクリア
4. **PII回避**: テストデータにも本番類似のPIIを使用しない

### 4.2 テストフィクスチャ例

```java
public class BookingTestFixtures {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-01-18T09:00:00Z"),
        ZoneOffset.UTC
    );

    public static Booking createPendingBooking() {
        return Booking.create(
            UserId.of("test-user-001"),
            ResourceId.of("test-resource-001"),
            createDefaultTimeRange(),
            "Test booking",
            FIXED_CLOCK
        );
    }

    public static Booking createConfirmedBooking() {
        Booking booking = createPendingBooking();
        booking.confirm(FIXED_CLOCK);
        return booking;
    }

    public static Booking createCancelledBooking() {
        Booking booking = createPendingBooking();
        booking.cancel("Test cancel", FIXED_CLOCK);
        return booking;
    }

    public static TimeRange createDefaultTimeRange() {
        return TimeRange.of(
            Instant.parse("2026-01-18T10:00:00Z"),
            Instant.parse("2026-01-18T11:00:00Z"),
            FIXED_CLOCK
        );
    }

    public static TimeRange createTimeRange(String startTime, String endTime) {
        return TimeRange.of(
            Instant.parse("2026-01-18T" + startTime + ":00Z"),
            Instant.parse("2026-01-18T" + endTime + ":00Z"),
            FIXED_CLOCK
        );
    }
}
```
