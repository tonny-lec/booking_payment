package com.booking.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique user identifier.
 *
 * <p>UserId is an immutable identifier based on UUID that uniquely identifies
 * a user in the IAM bounded context. It follows DDD value object semantics
 * where equality is based on the underlying value, not object identity.
 *
 * <p>Usage:
 * <pre>{@code
 * // Generate a new UserId
 * UserId newId = UserId.generate();
 *
 * // Create from existing UUID
 * UserId existingId = UserId.of(someUuid);
 *
 * // Parse from string
 * UserId parsedId = UserId.fromString("550e8400-e29b-41d4-a716-446655440000");
 * }</pre>
 *
 * @param value the underlying UUID value
 * @see UUID
 */
public record UserId(UUID value) {

    /**
     * Canonical constructor with null validation.
     *
     * @param value the UUID value
     * @throws NullPointerException if value is null
     */
    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    /**
     * Creates a UserId from an existing UUID.
     *
     * @param value the UUID value
     * @return a new UserId instance
     * @throws NullPointerException if value is null
     */
    public static UserId of(UUID value) {
        return new UserId(value);
    }

    /**
     * Generates a new random UserId.
     *
     * <p>Uses {@link UUID#randomUUID()} to generate a unique identifier.
     *
     * @return a new UserId with a randomly generated UUID
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Creates a UserId from a string representation.
     *
     * @param value the string representation of a UUID
     * @return a new UserId instance
     * @throws IllegalArgumentException if the string is not a valid UUID format
     * @throws NullPointerException if value is null
     */
    public static UserId fromString(String value) {
        Objects.requireNonNull(value, "UserId string value must not be null");
        return new UserId(UUID.fromString(value));
    }

    /**
     * Returns the string representation of the underlying UUID.
     *
     * @return the UUID as a string
     */
    public String asString() {
        return value.toString();
    }
}
