package com.booking.payment.adapter.persistence;

import com.booking.booking.domain.model.BookingId;
import com.booking.payment.adapter.persistence.entity.PaymentEntity;
import com.booking.payment.adapter.persistence.repository.PaymentJpaRepository;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-based implementation of {@link PaymentRepository}.
 *
 * <p>The unique constraint on {@code payments.idempotency_key} is the final
 * guard against concurrent requests with the same key: when two requests
 * race past the application-level replay check, the loser's insert violates
 * the constraint and is translated into a 409 {@link ConflictException}
 * (the client retries and receives the stored result).
 */
@Repository
public class JpaPaymentRepository implements PaymentRepository {

    private static final EnumSet<PaymentStatus> ACTIVE_STATUSES =
            EnumSet.of(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED);

    private final PaymentJpaRepository paymentJpaRepository;

    public JpaPaymentRepository(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return paymentJpaRepository.findById(paymentId.value())
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(UUID idempotencyKeyValue) {
        return paymentJpaRepository.findByIdempotencyKey(idempotencyKeyValue)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findLatestByBookingId(BookingId bookingId) {
        return paymentJpaRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId.value())
                .map(PaymentEntity::toDomain);
    }

    @Override
    public boolean existsActiveByBookingId(BookingId bookingId) {
        return paymentJpaRepository.existsByBookingIdAndStatusIn(bookingId.value(), ACTIVE_STATUSES);
    }

    @Override
    public Payment save(Payment payment) {
        try {
            // saveAndFlush: force the INSERT inside this call so that a unique
            // constraint violation is raised (and translated) here instead of
            // at transaction commit, where it could no longer be mapped to 409.
            return paymentJpaRepository.saveAndFlush(PaymentEntity.fromDomain(payment)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "payment_idempotency_key_conflict",
                    "Concurrent request with the same Idempotency-Key was already persisted",
                    ex
            );
        }
    }
}
