package com.booking.payment.adapter.persistence.entity;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * JPA entity mapping for payments table.
 *
 * <p>The idempotency key is stored as its UUID value only; on reconstruction
 * the key timestamp is restored from {@code created_at} (the moment the
 * server first processed the key), which is the reference point for the
 * 24h TTL defined in {@link IdempotencyKey#TTL}.
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "payment_refund_requests",
            joinColumns = @JoinColumn(name = "payment_id", nullable = false)
    )
    private Set<PaymentRefundRequestEntity> refundRequests = new LinkedHashSet<>();

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
        entity.refundRequests = payment.refundRequestAmounts().entrySet().stream()
                .map(entry -> new PaymentRefundRequestEntity(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
                .refundRequestAmounts(toRefundRequestAmounts())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private Map<UUID, Integer> toRefundRequestAmounts() {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        for (PaymentRefundRequestEntity refundRequest : refundRequests) {
            result.put(refundRequest.idempotencyKey(), refundRequest.requestAmount());
        }
        return result;
    }
}
