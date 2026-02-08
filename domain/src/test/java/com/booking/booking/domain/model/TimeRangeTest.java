package com.booking.booking.domain.model;

import com.booking.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TimeRange")
class TimeRangeTest {

    private static final Instant NOW = Instant.parse("2026-02-08T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("of() should create range when startAt is now or future")
        void ofShouldCreateRangeWhenStartIsNowOrFuture() {
            TimeRange atNow = TimeRange.of(NOW, NOW.plusSeconds(3600), FIXED_CLOCK);
            TimeRange future = TimeRange.of(NOW.plusSeconds(60), NOW.plusSeconds(7200), FIXED_CLOCK);

            assertThat(atNow.startAt()).isEqualTo(NOW);
            assertThat(atNow.endAt()).isEqualTo(NOW.plusSeconds(3600));
            assertThat(future.startAt()).isEqualTo(NOW.plusSeconds(60));
        }

        @Test
        @DisplayName("of() should reject startAt in past")
        void ofShouldRejectPastStartAt() {
            assertThatThrownBy(() -> TimeRange.of(NOW.minusSeconds(1), NOW.plusSeconds(300), FIXED_CLOCK))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("must not be in the past");
        }

        @Test
        @DisplayName("fromPersisted() should allow past startAt")
        void fromPersistedShouldAllowPastStartAt() {
            TimeRange persisted = TimeRange.fromPersisted(NOW.minusSeconds(300), NOW.plusSeconds(300));

            assertThat(persisted.startAt()).isEqualTo(NOW.minusSeconds(300));
        }

        @Test
        @DisplayName("should reject when startAt equals endAt")
        void shouldRejectWhenStartEqualsEnd() {
            assertThatThrownBy(() -> TimeRange.fromPersisted(NOW, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("before");
        }

        @Test
        @DisplayName("should reject when startAt is after endAt")
        void shouldRejectWhenStartAfterEnd() {
            assertThatThrownBy(() -> TimeRange.fromPersisted(NOW.plusSeconds(10), NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("before");
        }
    }

    @Nested
    @DisplayName("Overlap")
    class Overlap {

        @Test
        @DisplayName("overlaps should return true for intersecting ranges")
        void overlapsShouldReturnTrueForIntersectingRanges() {
            TimeRange left = TimeRange.fromPersisted(NOW, NOW.plusSeconds(3600));
            TimeRange right = TimeRange.fromPersisted(NOW.plusSeconds(1800), NOW.plusSeconds(5400));

            assertThat(left.overlaps(right)).isTrue();
            assertThat(right.overlaps(left)).isTrue();
        }

        @Test
        @DisplayName("overlaps should return false for adjacent ranges")
        void overlapsShouldReturnFalseForAdjacentRanges() {
            TimeRange left = TimeRange.fromPersisted(NOW, NOW.plusSeconds(3600));
            TimeRange right = TimeRange.fromPersisted(NOW.plusSeconds(3600), NOW.plusSeconds(7200));

            assertThat(left.overlaps(right)).isFalse();
            assertThat(right.overlaps(left)).isFalse();
        }

        @Test
        @DisplayName("overlaps should return false for separated ranges")
        void overlapsShouldReturnFalseForSeparatedRanges() {
            TimeRange left = TimeRange.fromPersisted(NOW, NOW.plusSeconds(3600));
            TimeRange right = TimeRange.fromPersisted(NOW.plusSeconds(7200), NOW.plusSeconds(10800));

            assertThat(left.overlaps(right)).isFalse();
        }
    }

    @Nested
    @DisplayName("Containment and duration")
    class ContainmentAndDuration {

        @Test
        @DisplayName("contains should include start and exclude end")
        void containsShouldIncludeStartAndExcludeEnd() {
            TimeRange range = TimeRange.fromPersisted(NOW, NOW.plusSeconds(3600));

            assertThat(range.contains(NOW)).isTrue();
            assertThat(range.contains(NOW.plusSeconds(3599))).isTrue();
            assertThat(range.contains(NOW.plusSeconds(3600))).isFalse();
        }

        @Test
        @DisplayName("duration should return end minus start")
        void durationShouldReturnEndMinusStart() {
            TimeRange range = TimeRange.fromPersisted(NOW, NOW.plusSeconds(5400));

            assertThat(range.duration()).isEqualTo(Duration.ofMinutes(90));
        }
    }
}
