package com.booking.booking.domain.model;

import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a booking.
 */
public class Booking {

    public static final int MAX_NOTE_LENGTH = 500;
    public static final int MAX_CANCEL_REASON_LENGTH = 500;
    public static final int INITIAL_VERSION = 1;

    private final BookingId id;
    private final UserId userId;
    private final ResourceId resourceId;
    private TimeRange timeRange;
    private BookingStatus status;
    private String note;
    private int version;
    private Instant cancelledAt;
    private String cancelReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private Booking(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "booking id must not be null");
        this.userId = Objects.requireNonNull(builder.userId, "userId must not be null");
        this.resourceId = Objects.requireNonNull(builder.resourceId, "resourceId must not be null");
        this.timeRange = Objects.requireNonNull(builder.timeRange, "timeRange must not be null");
        this.status = builder.status != null ? builder.status : BookingStatus.PENDING;
        this.note = normalizeOptionalText(builder.note, MAX_NOTE_LENGTH, "note");
        this.version = builder.version > 0 ? builder.version : INITIAL_VERSION;
        this.cancelledAt = builder.cancelledAt;
        this.cancelReason = normalizeOptionalText(
                builder.cancelReason,
                MAX_CANCEL_REASON_LENGTH,
                "cancelReason"
        );
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;

        validateInvariants();
    }

    /**
     * Creates a new booking in PENDING state.
     *
     * @param userId booking owner
     * @param resourceId target resource
     * @param timeRange booked range
     * @param note optional note
     * @return booking aggregate
     */
    public static Booking create(UserId userId, ResourceId resourceId, TimeRange timeRange, String note) {
        return create(userId, resourceId, timeRange, note, Clock.systemUTC());
    }

    /**
     * Creates a new booking with explicit clock (for deterministic tests).
     *
     * @param userId booking owner
     * @param resourceId target resource
     * @param timeRange booked range
     * @param note optional note
     * @param clock clock to resolve now
     * @return booking aggregate
     */
    public static Booking create(
            UserId userId,
            ResourceId resourceId,
            TimeRange timeRange,
            String note,
            Clock clock
    ) {
        Objects.requireNonNull(clock, "clock must not be null");
        Instant now = Instant.now(clock);
        return builder()
                .id(BookingId.generate())
                .userId(userId)
                .resourceId(resourceId)
                .timeRange(timeRange)
                .status(BookingStatus.PENDING)
                .note(note)
                .version(INITIAL_VERSION)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Updates booking time range with optimistic-lock check.
     *
     * @param newTimeRange new time range
     * @param expectedVersion expected current version
     */
    public void updateTimeRange(TimeRange newTimeRange, int expectedVersion) {
        Objects.requireNonNull(newTimeRange, "newTimeRange must not be null");
        assertVersion(expectedVersion);
        assertModifiable();
        this.timeRange = newTimeRange;
        incrementVersion();
    }

    /**
     * Updates booking note with optimistic-lock check.
     *
     * @param newNote new note value (nullable)
     * @param expectedVersion expected current version
     */
    public void updateNote(String newNote, int expectedVersion) {
        assertVersion(expectedVersion);
        assertModifiable();
        this.note = normalizeOptionalText(newNote, MAX_NOTE_LENGTH, "note");
        incrementVersion();
    }

    /**
     * Transitions booking from PENDING to CONFIRMED.
     */
    public void confirm() {
        if (!status.isConfirmable()) {
            throw new BusinessRuleViolationException(
                    "booking_invalid_state",
                    "Booking cannot be confirmed from status: " + status
            );
        }
        this.status = BookingStatus.CONFIRMED;
        incrementVersion();
    }

    /**
     * Cancels booking from PENDING or CONFIRMED state.
     *
     * @param reason optional cancel reason
     */
    public void cancel(String reason) {
        if (!status.isCancellable()) {
            String code = status == BookingStatus.CANCELLED ? "booking_already_cancelled" : "booking_not_cancellable";
            throw new BusinessRuleViolationException(
                    code,
                    "Booking cannot be cancelled from status: " + status
            );
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelReason = normalizeOptionalText(reason, MAX_CANCEL_REASON_LENGTH, "cancelReason");
        incrementVersion();
    }

    /**
     * Checks ownership by user ID.
     *
     * @param requesterId requester user ID
     * @return true when requester owns this booking
     */
    public boolean isOwnedBy(UserId requesterId) {
        Objects.requireNonNull(requesterId, "requesterId must not be null");
        return userId.equals(requesterId);
    }

    public BookingId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public ResourceId resourceId() {
        return resourceId;
    }

    public TimeRange timeRange() {
        return timeRange;
    }

    public BookingStatus status() {
        return status;
    }

    public String note() {
        return note;
    }

    public int version() {
        return version;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BookingId id;
        private UserId userId;
        private ResourceId resourceId;
        private TimeRange timeRange;
        private BookingStatus status;
        private String note;
        private int version;
        private Instant cancelledAt;
        private String cancelReason;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder id(BookingId id) {
            this.id = id;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder resourceId(ResourceId resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder timeRange(TimeRange timeRange) {
            this.timeRange = timeRange;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder cancelledAt(Instant cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public Builder cancelReason(String cancelReason) {
            this.cancelReason = cancelReason;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Booking build() {
            return new Booking(this);
        }
    }

    private void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }

    private void assertVersion(int expectedVersion) {
        if (expectedVersion < INITIAL_VERSION) {
            throw new IllegalArgumentException("expectedVersion must be positive");
        }
        if (this.version != expectedVersion) {
            throw new ConflictException(
                    "booking_version_mismatch",
                    "Expected version " + expectedVersion + " but was " + version
            );
        }
    }

    private void assertModifiable() {
        if (!status.isModifiable()) {
            throw new BusinessRuleViolationException(
                    "booking_not_modifiable",
                    "Booking cannot be modified from status: " + status
            );
        }
    }

    private void validateInvariants() {
        if (version < INITIAL_VERSION) {
            throw new IllegalArgumentException("version must be positive");
        }

        if (status == BookingStatus.CANCELLED && cancelledAt == null) {
            throw new IllegalArgumentException("cancelledAt must be set when status is CANCELLED");
        }

        if (status != BookingStatus.CANCELLED && (cancelledAt != null || cancelReason != null)) {
            throw new IllegalArgumentException("cancel metadata is only allowed for CANCELLED status");
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    private static String normalizeOptionalText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() > maxLength) {
            throw new BusinessRuleViolationException(
                    "booking_" + fieldName + "_too_long",
                    fieldName + " must not exceed " + maxLength + " characters"
            );
        }

        return trimmed;
    }
}
