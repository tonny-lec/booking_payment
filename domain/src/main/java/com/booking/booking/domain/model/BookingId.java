package com.booking.booking.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique booking identifier.
 *
 * @param value the underlying UUID value
 */
public record BookingId(UUID value) {

    public BookingId {
        Objects.requireNonNull(value, "BookingId value must not be null");
    }

    /**
     * Creates a BookingId from an existing UUID.
     *
     * @param value UUID value
     * @return BookingId
     */
    public static BookingId of(UUID value) {
        return new BookingId(value);
    }

    /**
     * Generates a new random booking identifier.
     *
     * @return BookingId with random UUID
     */
    public static BookingId generate() {
        return new BookingId(UUID.randomUUID());
    }

    /**
     * Creates a BookingId from UUID string representation.
     *
     * @param value UUID string
     * @return BookingId
     */
    public static BookingId fromString(String value) {
        Objects.requireNonNull(value, "BookingId string value must not be null");
        return new BookingId(UUID.fromString(value));
    }

    /**
     * Returns UUID as string.
     *
     * @return UUID string
     */
    public String asString() {
        return value.toString();
    }
}
