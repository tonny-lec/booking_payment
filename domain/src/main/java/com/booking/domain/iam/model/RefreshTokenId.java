package com.booking.domain.iam.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique refresh token identifier.
 *
 * <p>RefreshTokenId is an immutable identifier based on UUID that uniquely identifies
 * a refresh token in the IAM bounded context. It follows DDD value object semantics
 * where equality is based on the underlying value, not object identity.
 *
 * <p>Refresh tokens are long-lived tokens used to obtain new access tokens without
 * requiring the user to re-authenticate. Each refresh token has a unique identifier
 * for tracking, revocation, and audit purposes.
 *
 * <p>Usage:
 * <pre>{@code
 * // Generate a new RefreshTokenId
 * RefreshTokenId newId = RefreshTokenId.generate();
 *
 * // Create from existing UUID
 * RefreshTokenId existingId = RefreshTokenId.of(someUuid);
 *
 * // Parse from string
 * RefreshTokenId parsedId = RefreshTokenId.fromString("550e8400-e29b-41d4-a716-446655440000");
 * }</pre>
 *
 * @see UUID
 * @see UserId
 */
public final class RefreshTokenId {

    private final UUID value;

    private RefreshTokenId(UUID value) {
        this.value = Objects.requireNonNull(value, "RefreshTokenId value must not be null");
    }

    /**
     * Creates a RefreshTokenId from an existing UUID.
     *
     * @param value the UUID value
     * @return a new RefreshTokenId instance
     * @throws NullPointerException if value is null
     */
    public static RefreshTokenId of(UUID value) {
        return new RefreshTokenId(value);
    }

    /**
     * Generates a new random RefreshTokenId.
     *
     * <p>Uses {@link UUID#randomUUID()} to generate a unique identifier.
     *
     * @return a new RefreshTokenId with a randomly generated UUID
     */
    public static RefreshTokenId generate() {
        return new RefreshTokenId(UUID.randomUUID());
    }

    /**
     * Creates a RefreshTokenId from a string representation.
     *
     * @param value the string representation of a UUID
     * @return a new RefreshTokenId instance
     * @throws IllegalArgumentException if the string is not a valid UUID format
     * @throws NullPointerException if value is null
     */
    public static RefreshTokenId fromString(String value) {
        Objects.requireNonNull(value, "RefreshTokenId string value must not be null");
        return new RefreshTokenId(UUID.fromString(value));
    }

    /**
     * Returns the underlying UUID value.
     *
     * @return the UUID value
     */
    public UUID value() {
        return value;
    }

    /**
     * Returns the string representation of the underlying UUID.
     *
     * @return the UUID as a string
     */
    public String asString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshTokenId that = (RefreshTokenId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshTokenId[" + value + "]";
    }
}
