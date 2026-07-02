package com.booking.payment.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;

/**
 * Processed refund request marker for payment refund idempotency.
 */
@Embeddable
public class PaymentRefundRequestEntity {

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "request_amount")
    private Integer requestAmount;

    protected PaymentRefundRequestEntity() {
    }

    PaymentRefundRequestEntity(UUID idempotencyKey, Integer requestAmount) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.requestAmount = requestAmount;
    }

    UUID idempotencyKey() {
        return idempotencyKey;
    }

    Integer requestAmount() {
        return requestAmount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaymentRefundRequestEntity that)) {
            return false;
        }
        return idempotencyKey.equals(that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return idempotencyKey.hashCode();
    }
}
