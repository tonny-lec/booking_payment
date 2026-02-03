package com.booking.domain.iam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RefreshToken} entity.
 */
@DisplayName("RefreshToken")
class RefreshTokenTest {

    private static final String VALID_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private UserId createUserId() {
        return UserId.generate();
    }

    private HashedToken createHashedToken() {
        return HashedToken.of(VALID_HASH);
    }

    private Instant futureExpiration() {
        return Instant.now().plus(Duration.ofDays(7));
    }

    private Instant pastExpiration() {
        return Instant.now().minus(Duration.ofDays(1));
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("create() should create a new RefreshToken with generated ID")
        void createShouldCreateNewRefreshToken() {
            // Given
            UserId userId = createUserId();
            HashedToken tokenHash = createHashedToken();
            Instant expiresAt = futureExpiration();

            // When
            RefreshToken token = RefreshToken.create(userId, tokenHash, expiresAt);

            // Then
            assertThat(token).isNotNull();
            assertThat(token.id()).isNotNull();
            assertThat(token.userId()).isEqualTo(userId);
            assertThat(token.tokenHash()).isEqualTo(tokenHash);
            assertThat(token.expiresAt()).isEqualTo(expiresAt);
            assertThat(token.revokedAt()).isNull();
            assertThat(token.createdAt()).isNotNull();
            assertThat(token.createdAt()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("create() should generate unique IDs for each token")
        void createShouldGenerateUniqueIds() {
            // Given
            UserId userId = createUserId();
            HashedToken tokenHash = createHashedToken();
            Instant expiresAt = futureExpiration();

            // When
            RefreshToken token1 = RefreshToken.create(userId, tokenHash, expiresAt);
            RefreshToken token2 = RefreshToken.create(userId, tokenHash, expiresAt);

            // Then
            assertThat(token1.id()).isNotEqualTo(token2.id());
        }

        @Test
        @DisplayName("create() should throw NullPointerException for null userId")
        void createShouldThrowExceptionForNullUserId() {
            assertThatThrownBy(() -> RefreshToken.create(null, createHashedToken(), futureExpiration()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId must not be null");
        }

        @Test
        @DisplayName("create() should throw NullPointerException for null tokenHash")
        void createShouldThrowExceptionForNullTokenHash() {
            assertThatThrownBy(() -> RefreshToken.create(createUserId(), null, futureExpiration()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("tokenHash must not be null");
        }

        @Test
        @DisplayName("create() should throw NullPointerException for null expiresAt")
        void createShouldThrowExceptionForNullExpiresAt() {
            assertThatThrownBy(() -> RefreshToken.create(createUserId(), createHashedToken(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("expiresAt must not be null");
        }

        @Test
        @DisplayName("create() should throw IllegalArgumentException for past expiration")
        void createShouldThrowExceptionForPastExpiration() {
            assertThatThrownBy(() -> RefreshToken.create(createUserId(), createHashedToken(), pastExpiration()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expiration time must be after creation time");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder() should create RefreshToken with all fields")
        void builderShouldCreateRefreshTokenWithAllFields() {
            // Given
            RefreshTokenId id = RefreshTokenId.generate();
            UserId userId = createUserId();
            HashedToken tokenHash = createHashedToken();
            Instant expiresAt = futureExpiration();
            Instant createdAt = Instant.now().minus(Duration.ofHours(1));

            // When
            RefreshToken token = RefreshToken.builder()
                    .id(id)
                    .userId(userId)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .build();

            // Then
            assertThat(token.id()).isEqualTo(id);
            assertThat(token.userId()).isEqualTo(userId);
            assertThat(token.tokenHash()).isEqualTo(tokenHash);
            assertThat(token.expiresAt()).isEqualTo(expiresAt);
            assertThat(token.createdAt()).isEqualTo(createdAt);
            assertThat(token.revokedAt()).isNull();
        }

        @Test
        @DisplayName("builder() should allow creating revoked token from persistence")
        void builderShouldAllowCreatingRevokedToken() {
            // Given
            Instant createdAt = Instant.now().minus(Duration.ofDays(2));
            Instant expiresAt = Instant.now().plus(Duration.ofDays(5));
            Instant revokedAt = Instant.now().minus(Duration.ofDays(1));

            // When
            RefreshToken token = RefreshToken.builder()
                    .id(RefreshTokenId.generate())
                    .userId(createUserId())
                    .tokenHash(createHashedToken())
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .revokedAt(revokedAt)
                    .build();

            // Then
            assertThat(token.revokedAt()).isEqualTo(revokedAt);
            assertThat(token.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("builder() should allow loading expired token from persistence")
        void builderShouldAllowLoadingExpiredToken() {
            // Given - token created in the past and already expired
            Instant createdAt = Instant.now().minus(Duration.ofDays(30));
            Instant expiresAt = Instant.now().minus(Duration.ofDays(1));
            Instant revokedAt = Instant.now().minus(Duration.ofDays(2));

            // When - loading from persistence with revokedAt set (skips future validation)
            RefreshToken token = RefreshToken.builder()
                    .id(RefreshTokenId.generate())
                    .userId(createUserId())
                    .tokenHash(createHashedToken())
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .revokedAt(revokedAt)
                    .build();

            // Then
            assertThat(token.isExpired()).isTrue();
            assertThat(token.isRevoked()).isTrue();
            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validity checks")
    class ValidityChecks {

        @Test
        @DisplayName("isValid() should return true for non-expired, non-revoked token")
        void isValidShouldReturnTrueForValidToken() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // Then
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("isValid() should return false for revoked token")
        void isValidShouldReturnFalseForRevokedToken() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // When
            token.revoke();

            // Then
            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("isValid() should return false for expired token")
        void isValidShouldReturnFalseForExpiredToken() {
            // Given - create token with very short expiration
            Instant shortExpiration = Instant.now().plusMillis(1);
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), shortExpiration);

            // Wait for expiration
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then
            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("isRevoked() should return false for non-revoked token")
        void isRevokedShouldReturnFalseForNonRevokedToken() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // Then
            assertThat(token.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("isRevoked() should return true after revoke()")
        void isRevokedShouldReturnTrueAfterRevoke() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // When
            token.revoke();

            // Then
            assertThat(token.isRevoked()).isTrue();
            assertThat(token.revokedAt()).isNotNull();
        }

        @Test
        @DisplayName("isExpired() should return false for non-expired token")
        void isExpiredShouldReturnFalseForNonExpiredToken() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // Then
            assertThat(token.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Revocation")
    class Revocation {

        @Test
        @DisplayName("revoke() should set revokedAt timestamp")
        void revokeShouldSetRevokedAtTimestamp() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());
            Instant beforeRevoke = Instant.now();

            // When
            token.revoke();

            // Then
            assertThat(token.revokedAt()).isNotNull();
            assertThat(token.revokedAt()).isAfterOrEqualTo(beforeRevoke);
        }

        @Test
        @DisplayName("revoke() should be idempotent")
        void revokeShouldBeIdempotent() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());
            token.revoke();
            Instant firstRevokedAt = token.revokedAt();

            // Wait a bit
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When - revoke again
            token.revoke();

            // Then - revokedAt should not change
            assertThat(token.revokedAt()).isEqualTo(firstRevokedAt);
        }
    }

    @Nested
    @DisplayName("Entity semantics")
    class EntitySemantics {

        @Test
        @DisplayName("equals() should return true for same ID")
        void equalsShouldReturnTrueForSameId() {
            // Given
            RefreshTokenId id = RefreshTokenId.generate();
            RefreshToken token1 = RefreshToken.builder()
                    .id(id)
                    .userId(createUserId())
                    .tokenHash(createHashedToken())
                    .expiresAt(futureExpiration())
                    .build();
            RefreshToken token2 = RefreshToken.builder()
                    .id(id)
                    .userId(createUserId())
                    .tokenHash(createHashedToken())
                    .expiresAt(futureExpiration())
                    .build();

            // Then
            assertThat(token1).isEqualTo(token2);
        }

        @Test
        @DisplayName("equals() should return false for different ID")
        void equalsShouldReturnFalseForDifferentId() {
            // Given
            RefreshToken token1 = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());
            RefreshToken token2 = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("equals() should return false for null")
        void equalsShouldReturnFalseForNull() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // Then
            assertThat(token.equals(null)).isFalse();
        }

        @Test
        @DisplayName("hashCode() should be consistent with equals()")
        void hashCodeShouldBeConsistentWithEquals() {
            // Given
            RefreshTokenId id = RefreshTokenId.generate();
            RefreshToken token1 = RefreshToken.builder()
                    .id(id)
                    .userId(createUserId())
                    .tokenHash(createHashedToken())
                    .expiresAt(futureExpiration())
                    .build();
            RefreshToken token2 = RefreshToken.builder()
                    .id(id)
                    .userId(createUserId())
                    .tokenHash(createHashedToken())
                    .expiresAt(futureExpiration())
                    .build();

            // Then
            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }
    }

    @Nested
    @DisplayName("String representation")
    class StringRepresentation {

        @Test
        @DisplayName("toString() should contain relevant information")
        void toStringShouldContainRelevantInformation() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());

            // Then
            String str = token.toString();
            assertThat(str)
                    .contains("RefreshToken")
                    .contains(token.id().toString())
                    .contains("revoked=false");
        }

        @Test
        @DisplayName("toString() should show revoked status when revoked")
        void toStringShouldShowRevokedStatus() {
            // Given
            RefreshToken token = RefreshToken.create(createUserId(), createHashedToken(), futureExpiration());
            token.revoke();

            // Then
            assertThat(token.toString()).contains("revoked=true");
        }
    }
}
