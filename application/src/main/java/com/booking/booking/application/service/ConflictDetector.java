package com.booking.booking.application.service;

import com.booking.booking.domain.model.TimeRange;

import java.util.Collection;
import java.util.Objects;

/**
 * Application service for booking time-range conflict detection.
 */
public class ConflictDetector {

    /**
     * Returns true when two ranges overlap.
     *
     * @param left first range
     * @param right second range
     * @return true if ranges overlap
     */
    public boolean hasConflict(TimeRange left, TimeRange right) {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
        return left.overlaps(right);
    }

    /**
     * Returns true when candidate range overlaps with any existing range.
     *
     * @param candidate candidate range
     * @param existingRanges existing ranges
     * @return true if any overlap exists
     */
    public boolean hasConflict(TimeRange candidate, Collection<TimeRange> existingRanges) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(existingRanges, "existingRanges must not be null");
        return existingRanges.stream().anyMatch(candidate::overlaps);
    }
}
