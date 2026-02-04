package com.booking.iam.domain.event;

import com.booking.iam.domain.model.UserId;
import com.booking.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when an account is locked.
 *
 * <p>This event is published when an account becomes locked due to consecutive
 * failures or an administrative action. It is intended for audit logging and
 * security notifications.
 */
public record AccountLocked(
        UUID eventId,
        UserId userId,
        Instant occurredAt,
        Reason reason,
        Instant lockedUntil
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public AccountLocked {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(occurredAt, "Occurred at must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");
        // lockedUntil can be null for indefinite locks
    }

    /**
     * Reason for account lock.
     */
    public enum Reason {
        CONSECUTIVE_FAILURES,
        ADMIN_ACTION
    }

    /**
     * Creates a new AccountLocked event with auto-generated event ID and timestamp.
     *
     * @param userId the ID of the locked user
     * @param reason the reason for the lock
     * @param lockedUntil when the lock expires (nullable for indefinite)
     * @return a new AccountLocked event
     */
    public static AccountLocked create(UserId userId, Reason reason, Instant lockedUntil) {
        return new AccountLocked(
                UUID.randomUUID(),
                userId,
                Instant.now(),
                reason,
                lockedUntil
        );
    }

    /**
     * Creates an AccountLocked event with all fields specified.
     *
     * <p>Use this factory method when reconstructing events from storage
     * or when you need to specify the event ID and timestamp explicitly.
     *
     * @param eventId the unique event identifier
     * @param userId the locked user ID
     * @param occurredAt when the lock occurred
     * @param reason the reason for the lock
     * @param lockedUntil when the lock expires (nullable for indefinite)
     * @return a new AccountLocked event
     */
    public static AccountLocked of(
            UUID eventId,
            UserId userId,
            Instant occurredAt,
            Reason reason,
            Instant lockedUntil
    ) {
        return new AccountLocked(eventId, userId, occurredAt, reason, lockedUntil);
    }

    /**
     * Returns the aggregate ID (user ID) as a UUID.
     *
     * @return the user ID as UUID
     */
    @Override
    public UUID aggregateId() {
        return userId.value();
    }

    @Override
    public String toString() {
        return "AccountLocked[eventId=" + eventId +
                ", userId=" + userId +
                ", occurredAt=" + occurredAt +
                ", reason=" + reason +
                ", lockedUntil=" + lockedUntil +
                "]";
    }
}
