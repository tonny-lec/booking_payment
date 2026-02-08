package com.booking.booking.domain.model;

import com.booking.shared.exception.BusinessRuleViolationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a booking time range.
 *
 * <p>Base invariants:
 * <ul>
 *   <li>startAt must be before endAt</li>
 * </ul>
 *
 * <p>Creation-time business rule:
 * <ul>
 *   <li>startAt must not be in the past (validated in {@link #of})</li>
 * </ul>
 */
public record TimeRange(Instant startAt, Instant endAt) {

    public TimeRange {
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
    }

    /**
     * Creates a time range and enforces booking creation rule (no past start time).
     *
     * @param startAt start time
     * @param endAt end time
     * @return TimeRange
     */
    public static TimeRange of(Instant startAt, Instant endAt) {
        return of(startAt, endAt, Clock.systemUTC());
    }

    /**
     * Creates a time range with explicit clock for deterministic tests.
     *
     * @param startAt start time
     * @param endAt end time
     * @param clock clock for "now" reference
     * @return TimeRange
     */
    public static TimeRange of(Instant startAt, Instant endAt, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        TimeRange range = new TimeRange(startAt, endAt);
        Instant now = Instant.now(clock);
        if (range.startAt.isBefore(now)) {
            throw new BusinessRuleViolationException(
                    "booking_in_past",
                    "Booking startAt must not be in the past"
            );
        }
        return range;
    }

    /**
     * Reconstructs a time range from persisted data without "past start" validation.
     *
     * @param startAt start time
     * @param endAt end time
     * @return TimeRange
     */
    public static TimeRange fromPersisted(Instant startAt, Instant endAt) {
        return new TimeRange(startAt, endAt);
    }

    /**
     * Returns whether this range overlaps with another range.
     *
     * <p>overlaps(a, b) = a.startAt &lt; b.endAt AND b.startAt &lt; a.endAt
     *
     * @param other another time range
     * @return true if ranges overlap
     */
    public boolean overlaps(TimeRange other) {
        Objects.requireNonNull(other, "other must not be null");
        return startAt.isBefore(other.endAt) && other.startAt.isBefore(endAt);
    }

    /**
     * Returns whether this range contains the given point in time.
     *
     * <p>Range is treated as [startAt, endAt), inclusive start and exclusive end.
     *
     * @param point instant to check
     * @return true if point is inside the range
     */
    public boolean contains(Instant point) {
        Objects.requireNonNull(point, "point must not be null");
        return !point.isBefore(startAt) && point.isBefore(endAt);
    }

    /**
     * Returns duration between startAt and endAt.
     *
     * @return duration
     */
    public Duration duration() {
        return Duration.between(startAt, endAt);
    }
}
