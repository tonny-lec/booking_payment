package com.booking.payment.adapter.persistence.entity;

import com.booking.payment.application.port.IdempotencyStore;
import com.booking.payment.domain.model.IdempotencyKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping for idempotency records.
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {

    @Id
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecordEntity() {
    }

    public static IdempotencyRecordEntity fromDomain(IdempotencyStore.IdempotencyRecord record) {
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.idempotencyKey = record.key().value();
        entity.requestHash = record.requestHash();
        entity.responseStatus = record.responseStatus();
        entity.responseBody = record.responseBody();
        entity.createdAt = record.createdAt();
        entity.expiresAt = record.expiresAt();
        return entity;
    }

    public IdempotencyStore.IdempotencyRecord toDomain() {
        return new IdempotencyStore.IdempotencyRecord(
                new IdempotencyKey(idempotencyKey, createdAt),
                requestHash,
                responseStatus,
                responseBody,
                createdAt,
                expiresAt
        );
    }
}
