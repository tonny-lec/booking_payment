package com.booking.payment.application.port;

import com.booking.booking.domain.model.BookingId;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for payment persistence operations.
 */
public interface PaymentRepository {

    /**
     * Finds a payment by its identifier.
     *
     * @param paymentId payment identifier
     * @return payment if found
     */
    Optional<Payment> findById(PaymentId paymentId);

    /**
     * Finds a payment by the idempotency key used at creation time.
     *
     * <p>Used to detect idempotent replays of {@code POST /payments}.
     *
     * @param idempotencyKeyValue idempotency key value
     * @return payment if a request with the same key was already processed
     */
    Optional<Payment> findByIdempotencyKey(UUID idempotencyKeyValue);

    /**
     * Finds the latest payment created for a booking.
     *
     * <p>Cancellation-time refund policy selection needs terminal payments too
     * (for example REFUNDED), so this lookup is intentionally not limited to
     * active statuses.
     *
     * @param bookingId booking identifier
     * @return latest payment for the booking, if any
     */
    Optional<Payment> findLatestByBookingId(BookingId bookingId);

    /**
     * Checks whether an active (PENDING/AUTHORIZED/CAPTURED) payment
     * already exists for the given booking.
     *
     * <p>Used to reject double payment attempts with different idempotency keys.
     *
     * @param bookingId booking identifier
     * @return true when an active payment exists
     */
    boolean existsActiveByBookingId(BookingId bookingId);

    /**
     * Persists a payment aggregate.
     *
     * @param payment payment aggregate
     * @return persisted aggregate
     */
    Payment save(Payment payment);
}
