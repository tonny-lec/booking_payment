package com.booking.payment.domain.event;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when payment authorization fails.
 */
public record PaymentFailed(
        UUID eventId,
        PaymentId paymentId,
        BookingId bookingId,
        UserId userId,
        String failureReason,
        Instant occurredAt
) implements DomainEvent {

    public PaymentFailed {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    /**
     * Creates failure event from payment aggregate.
     */
    public static PaymentFailed from(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        if (payment.status() != PaymentStatus.FAILED) {
            throw new IllegalArgumentException("payment status must be FAILED");
        }
        return new PaymentFailed(
                UUID.randomUUID(),
                payment.id(),
                payment.bookingId(),
                payment.userId(),
                payment.failureReason(),
                Instant.now()
        );
    }

    @Override
    public UUID aggregateId() {
        return paymentId.value();
    }
}
