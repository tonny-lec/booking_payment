package com.booking.booking.application.service;

import com.booking.booking.domain.model.TimeRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConflictDetector")
class ConflictDetectorTest {

    private final ConflictDetector conflictDetector = new ConflictDetector();

    @Test
    @DisplayName("hasConflict should detect overlapping ranges")
    void hasConflictShouldDetectOverlappingRanges() {
        TimeRange existing = range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z");
        TimeRange candidate = range("2026-03-10T10:30:00Z", "2026-03-10T11:30:00Z");

        assertThat(conflictDetector.hasConflict(existing, candidate)).isTrue();
    }

    @Test
    @DisplayName("hasConflict should treat adjacent ranges as non-overlapping")
    void hasConflictShouldTreatAdjacentRangesAsNonOverlapping() {
        TimeRange existing = range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z");
        TimeRange candidate = range("2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z");

        assertThat(conflictDetector.hasConflict(existing, candidate)).isFalse();
    }

    @Test
    @DisplayName("hasConflict should detect any existing overlapping range")
    void hasConflictShouldDetectAnyExistingOverlappingRange() {
        TimeRange candidate = range("2026-03-10T10:30:00Z", "2026-03-10T11:30:00Z");

        assertThat(conflictDetector.hasConflict(candidate, List.of(
                range("2026-03-10T08:00:00Z", "2026-03-10T09:00:00Z"),
                range("2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z"),
                range("2026-03-10T12:00:00Z", "2026-03-10T13:00:00Z")
        ))).isTrue();
    }

    @Test
    @DisplayName("hasConflict should return false for empty existing ranges")
    void hasConflictShouldReturnFalseForEmptyExistingRanges() {
        TimeRange candidate = range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z");

        assertThat(conflictDetector.hasConflict(candidate, List.of())).isFalse();
    }

    private static TimeRange range(String startAt, String endAt) {
        return TimeRange.fromPersisted(Instant.parse(startAt), Instant.parse(endAt));
    }
}
