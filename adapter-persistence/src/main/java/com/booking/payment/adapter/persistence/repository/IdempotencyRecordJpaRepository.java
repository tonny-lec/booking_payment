package com.booking.payment.adapter.persistence.repository;

import com.booking.payment.adapter.persistence.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for idempotency records.
 */
public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

    long deleteByExpiresAtBefore(Instant threshold);
}
