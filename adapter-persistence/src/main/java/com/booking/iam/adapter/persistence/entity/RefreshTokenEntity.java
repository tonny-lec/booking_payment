package com.booking.iam.adapter.persistence.entity;

import com.booking.iam.domain.model.HashedToken;
import com.booking.iam.domain.model.RefreshToken;
import com.booking.iam.domain.model.RefreshTokenId;
import com.booking.iam.domain.model.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping for IAM refresh tokens.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {
    }

    public static RefreshTokenEntity fromDomain(RefreshToken refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = refreshToken.id().value();
        entity.userId = refreshToken.userId().value();
        entity.tokenHash = refreshToken.tokenHash().value();
        entity.expiresAt = refreshToken.expiresAt();
        entity.revokedAt = refreshToken.revokedAt();
        entity.createdAt = refreshToken.createdAt();
        return entity;
    }

    public RefreshToken toDomain() {
        return RefreshToken.builder()
                .id(RefreshTokenId.of(id))
                .userId(UserId.of(userId))
                .tokenHash(HashedToken.fromTrustedSource(tokenHash))
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .createdAt(createdAt)
                .build();
    }
}
