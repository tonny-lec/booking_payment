package com.booking.iam.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HashedToken} value object.
 */
@DisplayName("HashedToken")
class HashedTokenTest {

    // Valid SHA-256 hash (64 hex characters)
    private static final String VALID_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String VALID_HASH_UPPERCASE = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
    private static final String DIFFERENT_HASH = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456";

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of() should create HashedToken from valid SHA-256 hash")
        void ofShouldCreateHashedTokenFromValidHash() {
            // When
            HashedToken token = HashedToken.of(VALID_HASH);

            // Then
            assertThat(token).isNotNull();
            assertThat(token.value()).isEqualTo(VALID_HASH);
        }

        @Test
        @DisplayName("of() should accept uppercase hex characters")
        void ofShouldAcceptUppercaseHex() {
            // When
            HashedToken token = HashedToken.of(VALID_HASH_UPPERCASE);

            // Then
            assertThat(token).isNotNull();
            assertThat(token.value()).isEqualTo(VALID_HASH_UPPERCASE);
        }

        @Test
        @DisplayName("of() should throw IllegalArgumentException for invalid hash format")
        void ofShouldThrowExceptionForInvalidHash() {
            assertThatThrownBy(() -> HashedToken.of("not-a-valid-hash"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid token hash format");
        }

        @Test
        @DisplayName("of() should throw IllegalArgumentException for too short hash")
        void ofShouldThrowExceptionForTooShortHash() {
            assertThatThrownBy(() -> HashedToken.of("e3b0c44298fc1c14"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid token hash format");
        }

        @Test
        @DisplayName("of() should throw IllegalArgumentException for too long hash")
        void ofShouldThrowExceptionForTooLongHash() {
            assertThatThrownBy(() -> HashedToken.of(VALID_HASH + "0000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid token hash format");
        }

        @Test
        @DisplayName("of() should throw NullPointerException for null hash")
        void ofShouldThrowExceptionForNullHash() {
            assertThatThrownBy(() -> HashedToken.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("fromTrustedSource() should create HashedToken without strict validation")
        void fromTrustedSourceShouldCreateHashedTokenWithoutStrictValidation() {
            // Given - a hash that doesn't follow SHA-256 format
            String customHash = "custom-hash-from-trusted-source";

            // When
            HashedToken token = HashedToken.fromTrustedSource(customHash);

            // Then
            assertThat(token).isNotNull();
            assertThat(token.value()).isEqualTo(customHash);
        }

        @Test
        @DisplayName("fromTrustedSource() should throw NullPointerException for null hash")
        void fromTrustedSourceShouldThrowExceptionForNullHash() {
            assertThatThrownBy(() -> HashedToken.fromTrustedSource(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("fromTrustedSource() should throw IllegalArgumentException for empty hash")
        void fromTrustedSourceShouldThrowExceptionForEmptyHash() {
            assertThatThrownBy(() -> HashedToken.fromTrustedSource(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }
    }

    @Nested
    @DisplayName("Token matching")
    class TokenMatching {

        @Test
        @DisplayName("matches() should return true when hash matches")
        void matchesShouldReturnTrueWhenHashMatches() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);
            String rawToken = "test-token";

            // When - mock hasher returns the same hash
            boolean result = token.matches(rawToken, t -> VALID_HASH);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("matches() should return true for case-insensitive hash comparison")
        void matchesShouldReturnTrueForCaseInsensitiveComparison() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);
            String rawToken = "test-token";

            // When - hasher returns uppercase, but should still match
            boolean result = token.matches(rawToken, t -> VALID_HASH_UPPERCASE);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("matches() should return false when hash does not match")
        void matchesShouldReturnFalseWhenHashDoesNotMatch() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);
            String rawToken = "test-token";

            // When - hasher returns different hash
            boolean result = token.matches(rawToken, t -> DIFFERENT_HASH);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("matches() should pass raw token to hasher")
        void matchesShouldPassRawTokenToHasher() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);
            String rawToken = "my-secret-token";

            // When/Then - verify hasher receives the correct token
            token.matches(rawToken, t -> {
                assertThat(t).isEqualTo(rawToken);
                return VALID_HASH;
            });
        }

        @Test
        @DisplayName("matches() should throw NullPointerException for null raw token")
        void matchesShouldThrowExceptionForNullRawToken() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);

            // Then
            assertThatThrownBy(() -> token.matches(null, t -> VALID_HASH))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Raw token must not be null");
        }

        @Test
        @DisplayName("matches() should throw NullPointerException for null hasher")
        void matchesShouldThrowExceptionForNullHasher() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);

            // Then
            assertThatThrownBy(() -> token.matches("test-token", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Token hasher must not be null");
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("equals() should return true for same hash")
        void equalsShouldReturnTrueForSameHash() {
            // Given
            HashedToken token1 = HashedToken.of(VALID_HASH);
            HashedToken token2 = HashedToken.of(VALID_HASH);

            // Then
            assertThat(token1).isEqualTo(token2);
        }

        @Test
        @DisplayName("equals() should return false for different hash")
        void equalsShouldReturnFalseForDifferentHash() {
            // Given
            HashedToken token1 = HashedToken.of(VALID_HASH);
            HashedToken token2 = HashedToken.of(DIFFERENT_HASH);

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("hashCode() should be consistent with equals()")
        void hashCodeShouldBeConsistentWithEquals() {
            // Given
            HashedToken token1 = HashedToken.of(VALID_HASH);
            HashedToken token2 = HashedToken.of(VALID_HASH);

            // Then
            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }
    }

    @Nested
    @DisplayName("String representation")
    class StringRepresentation {

        @Test
        @DisplayName("masked() should return first 8 characters followed by ellipsis")
        void maskedShouldReturnFirst8CharactersWithEllipsis() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);

            // When
            String masked = token.masked();

            // Then
            assertThat(masked).isEqualTo(VALID_HASH.substring(0, 8) + "...");
        }

        @Test
        @DisplayName("toString() should return masked representation")
        void toStringShouldReturnMaskedRepresentation() {
            // Given
            HashedToken token = HashedToken.of(VALID_HASH);

            // Then
            assertThat(token.toString())
                    .contains("HashedToken")
                    .contains(VALID_HASH.substring(0, 8))
                    .doesNotContain(VALID_HASH);
        }
    }
}
