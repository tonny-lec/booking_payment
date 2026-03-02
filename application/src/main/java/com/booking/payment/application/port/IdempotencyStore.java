package com.booking.payment.application.port;

import com.booking.payment.domain.model.IdempotencyKey;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Port for idempotency key based request deduplication.
 */
public interface IdempotencyStore {

    /**
     * Finds idempotency record by key.
     *
     * @param key idempotency key
     * @return record if exists
     */
    Optional<IdempotencyRecord> findByKey(IdempotencyKey key);

    /**
     * Persists idempotency record.
     *
     * @param record idempotency record
     * @return stored record
     */
    IdempotencyRecord save(IdempotencyRecord record);

    /**
     * Deletes expired records.
     *
     * @param now current instant
     * @return number of deleted records
     */
    long deleteExpired(Instant now);

    /**
     * Idempotency storage model.
     */
    record IdempotencyRecord(
            IdempotencyKey key,
            String requestHash,
            int responseStatus,
            String responseBody,
            Instant createdAt,
            Instant expiresAt
    ) {
        public IdempotencyRecord {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
            Objects.requireNonNull(responseBody, "responseBody must not be null");
            Objects.requireNonNull(createdAt, "createdAt must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
            if (requestHash.isBlank()) {
                throw new IllegalArgumentException("requestHash must not be blank");
            }
        }
    }
}
