package com.booking.payment.adapter.persistence;

import com.booking.payment.adapter.persistence.entity.PaymentEntity;
import com.booking.payment.adapter.persistence.repository.PaymentJpaRepository;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * JPA-based implementation of {@link PaymentRepository}.
 */
@Repository
public class JpaPaymentRepository implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    public JpaPaymentRepository(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = Objects.requireNonNull(paymentJpaRepository, "paymentJpaRepository must not be null");
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return paymentJpaRepository.findById(paymentId.value())
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey idempotencyKey) {
        return paymentJpaRepository.findByIdempotencyKey(idempotencyKey.value())
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.fromDomain(payment);
        return paymentJpaRepository.save(entity).toDomain();
    }
}
