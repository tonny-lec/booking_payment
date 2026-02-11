package com.booking.payment.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique payment identifier.
 *
 * @param value the underlying UUID value
 */
public record PaymentId(UUID value) {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId value must not be null");
    }

    /**
     * Creates PaymentId from UUID.
     *
     * @param value UUID value
     * @return PaymentId
     */
    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    /**
     * Generates a new random payment identifier.
     *
     * @return PaymentId
     */
    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    /**
     * Creates PaymentId from UUID string.
     *
     * @param value UUID string
     * @return PaymentId
     */
    public static PaymentId fromString(String value) {
        Objects.requireNonNull(value, "PaymentId string value must not be null");
        return new PaymentId(UUID.fromString(value));
    }

    /**
     * Returns UUID string representation.
     *
     * @return UUID string
     */
    public String asString() {
        return value.toString();
    }
}
