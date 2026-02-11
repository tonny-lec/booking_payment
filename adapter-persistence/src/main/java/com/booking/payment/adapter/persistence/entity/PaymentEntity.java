package com.booking.payment.adapter.persistence.entity;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping for payment aggregate.
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "captured_amount")
    private Integer capturedAmount;

    @Column(name = "refunded_amount")
    private Integer refundedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "gateway_transaction_id", length = 255)
    private String gatewayTransactionId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentEntity() {
    }

    public static PaymentEntity fromDomain(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.id().value();
        entity.bookingId = payment.bookingId().value();
        entity.userId = payment.userId().value();
        entity.amount = payment.money().amount();
        entity.capturedAmount = payment.capturedAmount();
        entity.refundedAmount = payment.refundedAmount();
        entity.currency = payment.money().currency();
        entity.status = payment.status();
        entity.description = payment.description();
        entity.gatewayTransactionId = payment.gatewayTransactionId();
        entity.failureReason = payment.failureReason();
        entity.idempotencyKey = payment.idempotencyKey().value();
        entity.createdAt = payment.createdAt();
        entity.updatedAt = payment.updatedAt();
        return entity;
    }

    public Payment toDomain() {
        return Payment.builder()
                .id(PaymentId.of(id))
                .bookingId(BookingId.of(bookingId))
                .userId(UserId.of(userId))
                .money(Money.of(amount, currency))
                .capturedAmount(capturedAmount)
                .refundedAmount(refundedAmount)
                .status(status)
                .description(description)
                .gatewayTransactionId(gatewayTransactionId)
                .failureReason(failureReason)
                .idempotencyKey(new IdempotencyKey(idempotencyKey, createdAt))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
