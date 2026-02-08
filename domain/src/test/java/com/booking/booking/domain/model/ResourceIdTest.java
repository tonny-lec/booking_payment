package com.booking.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResourceId")
class ResourceIdTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("generate() should create unique IDs")
        void generateShouldCreateUniqueIds() {
            ResourceId id1 = ResourceId.generate();
            ResourceId id2 = ResourceId.generate();

            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        @DisplayName("of() should create ResourceId from UUID")
        void ofShouldCreateFromUuid() {
            UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            ResourceId resourceId = ResourceId.of(uuid);

            assertThat(resourceId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of() should reject null")
        void ofShouldRejectNull() {
            assertThatThrownBy(() -> ResourceId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("fromString() should create ResourceId from valid UUID string")
        void fromStringShouldCreateFromValidUuidString() {
            String raw = "123e4567-e89b-12d3-a456-426614174000";

            ResourceId resourceId = ResourceId.fromString(raw);

            assertThat(resourceId.asString()).isEqualTo(raw);
        }

        @Test
        @DisplayName("fromString() should reject invalid UUID string")
        void fromStringShouldRejectInvalidUuidString() {
            assertThatThrownBy(() -> ResourceId.fromString("invalid-resource-id"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("equals/hashCode should match for same UUID")
        void equalsAndHashCodeShouldMatchForSameUuid() {
            UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            ResourceId left = ResourceId.of(uuid);
            ResourceId right = ResourceId.of(uuid);

            assertThat(left).isEqualTo(right);
            assertThat(left.hashCode()).isEqualTo(right.hashCode());
        }

        @Test
        @DisplayName("equals should be false for different UUID")
        void equalsShouldBeFalseForDifferentUuid() {
            ResourceId left = ResourceId.fromString("123e4567-e89b-12d3-a456-426614174000");
            ResourceId right = ResourceId.fromString("223e4567-e89b-12d3-a456-426614174001");

            assertThat(left).isNotEqualTo(right);
        }
    }
}
