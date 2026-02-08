package com.booking.booking.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a reservable resource identifier.
 *
 * @param value the underlying UUID value
 */
public record ResourceId(UUID value) {

    public ResourceId {
        Objects.requireNonNull(value, "ResourceId value must not be null");
    }

    /**
     * Creates ResourceId from UUID.
     *
     * @param value UUID value
     * @return ResourceId
     */
    public static ResourceId of(UUID value) {
        return new ResourceId(value);
    }

    /**
     * Generates a new random resource identifier.
     *
     * @return ResourceId
     */
    public static ResourceId generate() {
        return new ResourceId(UUID.randomUUID());
    }

    /**
     * Creates ResourceId from UUID string.
     *
     * @param value UUID string value
     * @return ResourceId
     */
    public static ResourceId fromString(String value) {
        Objects.requireNonNull(value, "ResourceId string value must not be null");
        return new ResourceId(UUID.fromString(value));
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
