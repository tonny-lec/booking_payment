package com.booking.payment.adapter.persistence;

import com.booking.payment.adapter.persistence.entity.IdempotencyRecordEntity;
import com.booking.payment.adapter.persistence.repository.IdempotencyRecordJpaRepository;
import com.booking.payment.application.port.IdempotencyStore;
import com.booking.payment.domain.model.IdempotencyKey;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA-based implementation of {@link IdempotencyStore}.
 */
@Repository
public class JpaIdempotencyStore implements IdempotencyStore {

    private final IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    public JpaIdempotencyStore(IdempotencyRecordJpaRepository idempotencyRecordJpaRepository) {
        this.idempotencyRecordJpaRepository = Objects.requireNonNull(
                idempotencyRecordJpaRepository,
                "idempotencyRecordJpaRepository must not be null"
        );
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(IdempotencyKey key) {
        return idempotencyRecordJpaRepository.findById(key.value())
                .map(IdempotencyRecordEntity::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        IdempotencyRecordEntity entity = IdempotencyRecordEntity.fromDomain(record);
        return idempotencyRecordJpaRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public long deleteExpired(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return idempotencyRecordJpaRepository.deleteByExpiresAtBefore(now);
    }
}
