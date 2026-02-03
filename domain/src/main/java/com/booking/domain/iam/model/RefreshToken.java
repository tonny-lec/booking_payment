package com.booking.domain.iam.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a refresh token in the IAM bounded context.
 *
 * <p>RefreshToken is a long-lived token used to obtain new access tokens without
 * requiring the user to re-authenticate. Each token is bound to a specific user
 * and has an expiration time.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>Token hash must be unique (enforced at repository level)</li>
 *   <li>Expiration time must be in the future at creation time</li>
 *   <li>Once revoked, the token cannot be used</li>
 * </ul>
 *
 * <h2>Behaviors</h2>
 * <ul>
 *   <li>{@link #isValid()} - Check if the token is valid (not expired, not revoked)</li>
 *   <li>{@link #revoke()} - Revoke the token</li>
 * </ul>
 *
 * @see RefreshTokenId
 * @see UserId
 * @see HashedToken
 */
public class RefreshToken {

    private final RefreshTokenId id;
    private final UserId userId;
    private final HashedToken tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private final Instant createdAt;

    private RefreshToken(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "RefreshToken id must not be null");
        this.userId = Objects.requireNonNull(builder.userId, "RefreshToken userId must not be null");
        this.tokenHash = Objects.requireNonNull(builder.tokenHash, "RefreshToken tokenHash must not be null");
        this.expiresAt = Objects.requireNonNull(builder.expiresAt, "RefreshToken expiresAt must not be null");
        this.revokedAt = builder.revokedAt;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();

        validateInvariants();
    }

    private void validateInvariants() {
        // For new tokens (not loaded from persistence), expiresAt must be in the future
        if (revokedAt == null && !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Expiration time must be after creation time");
        }
    }

    /**
     * Creates a new RefreshToken with a generated ID.
     *
     * @param userId the ID of the user this token belongs to
     * @param tokenHash the hashed token value
     * @param expiresAt the expiration time
     * @return a new RefreshToken instance
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if expiresAt is not in the future
     */
    public static RefreshToken create(UserId userId, HashedToken tokenHash, Instant expiresAt) {
        return builder()
                .id(RefreshTokenId.generate())
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Checks if the token is currently valid.
     *
     * <p>A token is valid if:
     * <ul>
     *   <li>It has not been revoked (revokedAt is null)</li>
     *   <li>It has not expired (current time is before expiresAt)</li>
     * </ul>
     *
     * @return true if the token is valid, false otherwise
     */
    public boolean isValid() {
        if (isRevoked()) {
            return false;
        }
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * Checks if the token has been revoked.
     *
     * @return true if the token has been revoked, false otherwise
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Checks if the token has expired.
     *
     * @return true if the token has expired, false otherwise
     */
    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    /**
     * Revokes this token.
     *
     * <p>Once revoked, the token cannot be used to obtain new access tokens.
     * This operation is idempotent - calling revoke on an already revoked token
     * has no effect.
     */
    public void revoke() {
        if (revokedAt == null) {
            this.revokedAt = Instant.now();
            // Domain event: RefreshTokenRevoked would be raised here
        }
    }

    // Getters

    public RefreshTokenId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public HashedToken tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    // Builder

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating RefreshToken instances.
     *
     * <p>Use this builder when reconstructing a RefreshToken from persistence
     * or when creating a RefreshToken with specific initial values.
     */
    public static class Builder {
        private RefreshTokenId id;
        private UserId userId;
        private HashedToken tokenHash;
        private Instant expiresAt;
        private Instant revokedAt;
        private Instant createdAt;

        private Builder() {}

        public Builder id(RefreshTokenId id) {
            this.id = id;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder tokenHash(HashedToken tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RefreshToken build() {
            return new RefreshToken(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken[id=" + id + ", userId=" + userId + ", expiresAt=" + expiresAt +
                ", revoked=" + isRevoked() + "]";
    }
}
