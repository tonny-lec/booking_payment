package com.booking.iam.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HashedPassword} value object.
 */
@DisplayName("HashedPassword")
class HashedPasswordTest {

    // Valid bcrypt hashes for testing (generated with various costs)
    // Format: $2[aby]$cost$22-char-salt + 31-char-hash = 60 chars total
    private static final String VALID_HASH_2A = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.MVW7jOvXgS5rCa";
    private static final String VALID_HASH_2B = "$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdyL8tG8tI/X3bJM8q5qT.0sT6u";
    private static final String VALID_HASH_2Y = "$2y$14$i6p9x.4gZN8wqFDPL3IKj.W3u.Qx4t9zN.iV/qKnF6bXhN.sG.pPa";

    @Nested
    @DisplayName("Factory method: of()")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create HashedPassword from valid $2a$ hash")
        void shouldCreateFromValid2aHash() {
            // When
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.value()).isEqualTo(VALID_HASH_2A);
        }

        @Test
        @DisplayName("should create HashedPassword from valid $2b$ hash")
        void shouldCreateFromValid2bHash() {
            // When
            HashedPassword hash = HashedPassword.of(VALID_HASH_2B);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.value()).isEqualTo(VALID_HASH_2B);
        }

        @Test
        @DisplayName("should create HashedPassword from valid $2y$ hash")
        void shouldCreateFromValid2yHash() {
            // When
            HashedPassword hash = HashedPassword.of(VALID_HASH_2Y);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.value()).isEqualTo(VALID_HASH_2Y);
        }

        @Test
        @DisplayName("should reject null hash")
        void shouldRejectNullHash() {
            assertThatThrownBy(() -> HashedPassword.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",                                          // Empty
                "plaintext",                                 // Not a hash
                "$2a$12$short",                              // Too short
                "$2x$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.MVW7jOvXgS5rCa", // Invalid version
                "$2a$99$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.MVW7jOvXgS5rCa", // Invalid cost (99)
                "2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.MVW7jOvXgS5rCa",  // Missing leading $
        })
        @DisplayName("should reject invalid hash formats")
        void shouldRejectInvalidHashFormats(String invalidHash) {
            assertThatThrownBy(() -> HashedPassword.of(invalidHash))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid bcrypt hash format");
        }
    }

    @Nested
    @DisplayName("Factory method: fromTrustedSource()")
    class FromTrustedSourceFactoryMethod {

        @Test
        @DisplayName("should create HashedPassword from trusted source")
        void shouldCreateFromTrustedSource() {
            // When
            HashedPassword hash = HashedPassword.fromTrustedSource(VALID_HASH_2A);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.value()).isEqualTo(VALID_HASH_2A);
        }

        @Test
        @DisplayName("should accept non-standard format from trusted source")
        void shouldAcceptNonStandardFormatFromTrustedSource() {
            // Given - A hash that might not match strict bcrypt pattern
            String customHash = "$argon2id$v=19$m=65536,t=3,p=4$somesalt$somehash";

            // When
            HashedPassword hash = HashedPassword.fromTrustedSource(customHash);

            // Then
            assertThat(hash.value()).isEqualTo(customHash);
        }

        @Test
        @DisplayName("should reject null hash")
        void shouldRejectNullHash() {
            assertThatThrownBy(() -> HashedPassword.fromTrustedSource(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("should reject empty hash")
        void shouldRejectEmptyHash() {
            assertThatThrownBy(() -> HashedPassword.fromTrustedSource(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }
    }

    @Nested
    @DisplayName("Password matching")
    class PasswordMatching {

        @Test
        @DisplayName("should return true when password matches")
        void shouldReturnTrueWhenPasswordMatches() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // When - Simulate matching password
            boolean result = hash.matches("correctPassword", (raw, hashed) -> true);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when password does not match")
        void shouldReturnFalseWhenPasswordDoesNotMatch() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // When - Simulate non-matching password
            boolean result = hash.matches("wrongPassword", (raw, hashed) -> false);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should pass raw password and hash to matcher")
        void shouldPassRawPasswordAndHashToMatcher() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);
            String rawPassword = "testPassword";

            // When/Then - Verify the matcher receives correct arguments
            hash.matches(rawPassword, (raw, hashed) -> {
                assertThat(raw).isEqualTo(rawPassword);
                assertThat(hashed).isEqualTo(VALID_HASH_2A);
                return true;
            });
        }

        @Test
        @DisplayName("should reject null raw password")
        void shouldRejectNullRawPassword() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThatThrownBy(() -> hash.matches(null, (raw, hashed) -> true))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Raw password must not be null");
        }

        @Test
        @DisplayName("should reject null matcher")
        void shouldRejectNullMatcher() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThatThrownBy(() -> hash.matches("password", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Password matcher must not be null");
        }
    }

    @Nested
    @DisplayName("Cost factor extraction")
    class CostFactorExtraction {

        @Test
        @DisplayName("should extract cost factor 12 from hash")
        void shouldExtractCostFactor12() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThat(hash.costFactor()).isEqualTo(12);
        }

        @Test
        @DisplayName("should extract cost factor 10 from hash")
        void shouldExtractCostFactor10() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2B);

            // Then
            assertThat(hash.costFactor()).isEqualTo(10);
        }

        @Test
        @DisplayName("should extract cost factor 14 from hash")
        void shouldExtractCostFactor14() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2Y);

            // Then
            assertThat(hash.costFactor()).isEqualTo(14);
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("should be equal for same hash")
        void shouldBeEqualForSameHash() {
            // Given
            HashedPassword hash1 = HashedPassword.of(VALID_HASH_2A);
            HashedPassword hash2 = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1.hashCode()).isEqualTo(hash2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different hashes")
        void shouldNotBeEqualForDifferentHashes() {
            // Given
            HashedPassword hash1 = HashedPassword.of(VALID_HASH_2A);
            HashedPassword hash2 = HashedPassword.of(VALID_HASH_2B);

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThat(hash.equals(null)).isFalse();
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThat(hash.equals(VALID_HASH_2A)).isFalse();
        }
    }

    @Nested
    @DisplayName("Security - toString() protection")
    class SecurityProtection {

        @Test
        @DisplayName("toString should not expose hash value")
        void toStringShouldNotExposeHashValue() {
            // Given
            HashedPassword hash = HashedPassword.of(VALID_HASH_2A);

            // Then
            assertThat(hash.toString())
                    .doesNotContain(VALID_HASH_2A)
                    .doesNotContain("$2a$")
                    .contains("PROTECTED");
        }
    }
}
