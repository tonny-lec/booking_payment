package com.booking.payment.domain.event;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when a payment is created.
 */
public record PaymentCreated(
        UUID eventId,
        PaymentId paymentId,
        BookingId bookingId,
        UserId userId,
        Money money,
        PaymentStatus status,
        IdempotencyKey idempotencyKey,
        Instant occurredAt
) implements DomainEvent {

    public PaymentCreated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    /**
     * Creates event from payment aggregate.
     */
    public static PaymentCreated from(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        return new PaymentCreated(
                UUID.randomUUID(),
                payment.id(),
                payment.bookingId(),
                payment.userId(),
                payment.money(),
                payment.status(),
                payment.idempotencyKey(),
                Instant.now()
        );
    }

    @Override
    public UUID aggregateId() {
        return paymentId.value();
    }
}
