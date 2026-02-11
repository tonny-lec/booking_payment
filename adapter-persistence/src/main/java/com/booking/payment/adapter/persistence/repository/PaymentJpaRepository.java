package com.booking.payment.adapter.persistence.repository;

import com.booking.payment.adapter.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for payments.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdempotencyKey(UUID idempotencyKey);
}
