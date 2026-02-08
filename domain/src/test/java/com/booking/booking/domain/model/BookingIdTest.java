package com.booking.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BookingId")
class BookingIdTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("generate() should create unique IDs")
        void generateShouldCreateUniqueIds() {
            BookingId id1 = BookingId.generate();
            BookingId id2 = BookingId.generate();

            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        @DisplayName("of() should create BookingId from UUID")
        void ofShouldCreateFromUuid() {
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            BookingId bookingId = BookingId.of(uuid);

            assertThat(bookingId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("of() should reject null")
        void ofShouldRejectNull() {
            assertThatThrownBy(() -> BookingId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("fromString() should create BookingId from valid UUID string")
        void fromStringShouldCreateFromValidUuidString() {
            String raw = "550e8400-e29b-41d4-a716-446655440000";

            BookingId bookingId = BookingId.fromString(raw);

            assertThat(bookingId.asString()).isEqualTo(raw);
        }

        @Test
        @DisplayName("fromString() should reject invalid UUID string")
        void fromStringShouldRejectInvalidUuidString() {
            assertThatThrownBy(() -> BookingId.fromString("not-a-valid-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("equals/hashCode should match for same UUID")
        void equalsAndHashCodeShouldMatchForSameUuid() {
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            BookingId left = BookingId.of(uuid);
            BookingId right = BookingId.of(uuid);

            assertThat(left).isEqualTo(right);
            assertThat(left.hashCode()).isEqualTo(right.hashCode());
        }

        @Test
        @DisplayName("equals should be false for different UUID")
        void equalsShouldBeFalseForDifferentUuid() {
            BookingId left = BookingId.fromString("550e8400-e29b-41d4-a716-446655440000");
            BookingId right = BookingId.fromString("660e8400-e29b-41d4-a716-446655440001");

            assertThat(left).isNotEqualTo(right);
        }
    }
}
