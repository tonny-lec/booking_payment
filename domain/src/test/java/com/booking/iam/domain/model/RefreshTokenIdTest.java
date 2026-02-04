package com.booking.iam.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RefreshTokenId} value object.
 */
@DisplayName("RefreshTokenId")
class RefreshTokenIdTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("generate() should create a new unique RefreshTokenId")
        void generateShouldCreateUniqueRefreshTokenId() {
            // When
            RefreshTokenId id1 = RefreshTokenId.generate();
            RefreshTokenId id2 = RefreshTokenId.generate();

            // Then
            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        @DisplayName("of() should create RefreshTokenId from UUID")
        void ofShouldCreateRefreshTokenIdFromUuid() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // When
            RefreshTokenId tokenId = RefreshTokenId.of(uuid);

            // Then
            assertThat(tokenId).isNotNull();
            assertThat(tokenId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of() should throw NullPointerException for null UUID")
        void ofShouldThrowExceptionForNullUuid() {
            assertThatThrownBy(() -> RefreshTokenId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("fromString() should create RefreshTokenId from valid UUID string")
        void fromStringShouldCreateRefreshTokenIdFromValidString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";

            // When
            RefreshTokenId tokenId = RefreshTokenId.fromString(uuidString);

            // Then
            assertThat(tokenId).isNotNull();
            assertThat(tokenId.asString()).isEqualTo(uuidString);
        }

        @Test
        @DisplayName("fromString() should throw IllegalArgumentException for invalid UUID string")
        void fromStringShouldThrowExceptionForInvalidString() {
            assertThatThrownBy(() -> RefreshTokenId.fromString("not-a-valid-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromString() should throw NullPointerException for null string")
        void fromStringShouldThrowExceptionForNullString() {
            assertThatThrownBy(() -> RefreshTokenId.fromString(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("equals() should return true for same UUID")
        void equalsShouldReturnTrueForSameUuid() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // When
            RefreshTokenId id1 = RefreshTokenId.of(uuid);
            RefreshTokenId id2 = RefreshTokenId.of(uuid);

            // Then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.equals(id2)).isTrue();
        }

        @Test
        @DisplayName("equals() should return false for different UUID")
        void equalsShouldReturnFalseForDifferentUuid() {
            // Given
            RefreshTokenId id1 = RefreshTokenId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            RefreshTokenId id2 = RefreshTokenId.of(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("equals() should return false for null")
        void equalsShouldReturnFalseForNull() {
            // Given
            RefreshTokenId tokenId = RefreshTokenId.generate();

            // Then
            assertThat(tokenId.equals(null)).isFalse();
        }

        @Test
        @DisplayName("equals() should return false for different type")
        void equalsShouldReturnFalseForDifferentType() {
            // Given
            RefreshTokenId tokenId = RefreshTokenId.generate();

            // Then
            assertThat(tokenId.equals("not-a-refresh-token-id")).isFalse();
        }

        @Test
        @DisplayName("equals() should return false when compared to UserId with same UUID")
        void equalsShouldReturnFalseForUserIdWithSameUuid() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            RefreshTokenId tokenId = RefreshTokenId.of(uuid);
            UserId userId = UserId.of(uuid);

            // Then - Different types should not be equal even with same UUID
            assertThat(tokenId.equals(userId)).isFalse();
        }

        @Test
        @DisplayName("hashCode() should be consistent with equals()")
        void hashCodeShouldBeConsistentWithEquals() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            RefreshTokenId id1 = RefreshTokenId.of(uuid);
            RefreshTokenId id2 = RefreshTokenId.of(uuid);

            // Then
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("hashCode() should be consistent across multiple calls")
        void hashCodeShouldBeConsistent() {
            // Given
            RefreshTokenId tokenId = RefreshTokenId.generate();

            // Then
            int hashCode1 = tokenId.hashCode();
            int hashCode2 = tokenId.hashCode();
            assertThat(hashCode1).isEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("String representation")
    class StringRepresentation {

        @Test
        @DisplayName("asString() should return UUID string")
        void asStringShouldReturnUuidString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            RefreshTokenId tokenId = RefreshTokenId.fromString(uuidString);

            // Then
            assertThat(tokenId.asString()).isEqualTo(uuidString);
        }

        @Test
        @DisplayName("toString() should return descriptive string")
        void toStringShouldReturnDescriptiveString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            RefreshTokenId tokenId = RefreshTokenId.fromString(uuidString);

            // Then
            assertThat(tokenId.toString())
                    .contains("RefreshTokenId")
                    .contains(uuidString);
        }
    }
}
