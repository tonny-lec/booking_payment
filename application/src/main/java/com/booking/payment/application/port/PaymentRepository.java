package com.booking.payment.application.port;

import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;

import java.util.Optional;

/**
 * Port interface for payment persistence operations.
 */
public interface PaymentRepository {

    /**
     * Finds payment by identifier.
     *
     * @param paymentId payment identifier
     * @return payment when found
     */
    Optional<Payment> findById(PaymentId paymentId);

    /**
     * Finds payment by idempotency key.
     *
     * @param idempotencyKey idempotency key
     * @return payment when found
     */
    Optional<Payment> findByIdempotencyKey(IdempotencyKey idempotencyKey);

    /**
     * Saves payment aggregate.
     *
     * @param payment payment aggregate
     * @return persisted payment
     */
    Payment save(Payment payment);
}
