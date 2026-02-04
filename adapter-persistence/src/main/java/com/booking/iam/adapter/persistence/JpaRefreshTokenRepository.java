package com.booking.iam.adapter.persistence;

import com.booking.iam.adapter.persistence.entity.RefreshTokenEntity;
import com.booking.iam.adapter.persistence.repository.RefreshTokenJpaRepository;
import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.domain.model.HashedToken;
import com.booking.iam.domain.model.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA-based implementation of {@link RefreshTokenRepository}.
 */
@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public JpaRefreshTokenRepository(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(HashedToken tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash.value())
                .map(RefreshTokenEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = RefreshTokenEntity.fromDomain(refreshToken);
        return refreshTokenJpaRepository.save(entity).toDomain();
    }
}
