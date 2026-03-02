package com.booking.booking.application.service;

import com.booking.booking.domain.model.TimeRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConflictDetector")
class ConflictDetectorTest {

    private final ConflictDetector detector = new ConflictDetector();

    @Test
    @DisplayName("hasConflict(range, range) returns true when ranges overlap")
    void hasConflictRangeRangeReturnsTrueWhenOverlap() {
        TimeRange left = TimeRange.fromPersisted(
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T12:00:00Z")
        );
        TimeRange right = TimeRange.fromPersisted(
                Instant.parse("2026-03-01T11:00:00Z"),
                Instant.parse("2026-03-01T13:00:00Z")
        );

        assertThat(detector.hasConflict(left, right)).isTrue();
    }

    @Test
    @DisplayName("hasConflict(range, ranges) returns false when only adjacent ranges exist")
    void hasConflictRangeRangesReturnsFalseWhenAdjacent() {
        TimeRange candidate = TimeRange.fromPersisted(
                Instant.parse("2026-03-01T12:00:00Z"),
                Instant.parse("2026-03-01T13:00:00Z")
        );
        List<TimeRange> existing = List.of(
                TimeRange.fromPersisted(
                        Instant.parse("2026-03-01T10:00:00Z"),
                        Instant.parse("2026-03-01T12:00:00Z")
                ),
                TimeRange.fromPersisted(
                        Instant.parse("2026-03-01T13:00:00Z"),
                        Instant.parse("2026-03-01T14:00:00Z")
                )
        );

        assertThat(detector.hasConflict(candidate, existing)).isFalse();
    }
}
