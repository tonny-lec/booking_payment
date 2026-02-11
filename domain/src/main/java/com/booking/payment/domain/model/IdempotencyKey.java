package com.booking.payment.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object for payment idempotency key with 24-hour validity window.
 *
 * @param value key value
 * @param createdAt key creation timestamp
 */
public record IdempotencyKey(UUID value, Instant createdAt) {

    public static final Duration TTL = Duration.ofHours(24);

    public IdempotencyKey {
        Objects.requireNonNull(value, "idempotency key value must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Creates a new key with current timestamp.
     *
     * @param value key UUID
     * @return idempotency key
     */
    public static IdempotencyKey of(UUID value) {
        return of(value, Clock.systemUTC());
    }

    /**
     * Creates a new key with explicit clock.
     *
     * @param value key UUID
     * @param clock clock for timestamp
     * @return idempotency key
     */
    public static IdempotencyKey of(UUID value, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return new IdempotencyKey(value, Instant.now(clock));
    }

    /**
     * Creates key from UUID string with current timestamp.
     *
     * @param value UUID string
     * @return idempotency key
     */
    public static IdempotencyKey fromString(String value) {
        Objects.requireNonNull(value, "idempotency key string must not be null");
        return of(UUID.fromString(value));
    }

    /**
     * Returns true when key has expired (older than 24 hours).
     *
     * @return true when expired
     */
    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    /**
     * Returns true when key has expired using explicit clock.
     *
     * @param clock clock for current time
     * @return true when expired
     */
    public boolean isExpired(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        Instant expiresAt = createdAt.plus(TTL);
        return Instant.now(clock).isAfter(expiresAt);
    }

    /**
     * Returns key as UUID string.
     *
     * @return UUID string
     */
    public String asString() {
        return value.toString();
    }
}
