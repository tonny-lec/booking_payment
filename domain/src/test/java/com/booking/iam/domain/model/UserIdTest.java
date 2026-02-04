package com.booking.iam.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UserId} value object.
 */
@DisplayName("UserId")
class UserIdTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("generate() should create a new unique UserId")
        void generateShouldCreateUniqueUserId() {
            // When
            UserId id1 = UserId.generate();
            UserId id2 = UserId.generate();

            // Then
            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        @DisplayName("of() should create UserId from UUID")
        void ofShouldCreateUserIdFromUuid() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // When
            UserId userId = UserId.of(uuid);

            // Then
            assertThat(userId).isNotNull();
            assertThat(userId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of() should throw NullPointerException for null UUID")
        void ofShouldThrowExceptionForNullUuid() {
            assertThatThrownBy(() -> UserId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("fromString() should create UserId from valid UUID string")
        void fromStringShouldCreateUserIdFromValidString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";

            // When
            UserId userId = UserId.fromString(uuidString);

            // Then
            assertThat(userId).isNotNull();
            assertThat(userId.asString()).isEqualTo(uuidString);
        }

        @Test
        @DisplayName("fromString() should throw IllegalArgumentException for invalid UUID string")
        void fromStringShouldThrowExceptionForInvalidString() {
            assertThatThrownBy(() -> UserId.fromString("not-a-valid-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromString() should throw NullPointerException for null string")
        void fromStringShouldThrowExceptionForNullString() {
            assertThatThrownBy(() -> UserId.fromString(null))
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
            UserId id1 = UserId.of(uuid);
            UserId id2 = UserId.of(uuid);

            // Then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.equals(id2)).isTrue();
        }

        @Test
        @DisplayName("equals() should return false for different UUID")
        void equalsShouldReturnFalseForDifferentUuid() {
            // Given
            UserId id1 = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            UserId id2 = UserId.of(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("equals() should return false for null")
        void equalsShouldReturnFalseForNull() {
            // Given
            UserId userId = UserId.generate();

            // Then
            assertThat(userId.equals(null)).isFalse();
        }

        @Test
        @DisplayName("equals() should return false for different type")
        void equalsShouldReturnFalseForDifferentType() {
            // Given
            UserId userId = UserId.generate();

            // Then
            assertThat(userId.equals("not-a-userid")).isFalse();
        }

        @Test
        @DisplayName("hashCode() should be consistent with equals()")
        void hashCodeShouldBeConsistentWithEquals() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UserId id1 = UserId.of(uuid);
            UserId id2 = UserId.of(uuid);

            // Then
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("hashCode() should be consistent across multiple calls")
        void hashCodeShouldBeConsistent() {
            // Given
            UserId userId = UserId.generate();

            // Then
            int hashCode1 = userId.hashCode();
            int hashCode2 = userId.hashCode();
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
            UserId userId = UserId.fromString(uuidString);

            // Then
            assertThat(userId.asString()).isEqualTo(uuidString);
        }

        @Test
        @DisplayName("toString() should return descriptive string")
        void toStringShouldReturnDescriptiveString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            UserId userId = UserId.fromString(uuidString);

            // Then
            assertThat(userId.toString())
                    .contains("UserId")
                    .contains(uuidString);
        }
    }
}
