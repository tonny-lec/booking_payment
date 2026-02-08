package com.booking.booking.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BookingStatus")
class BookingStatusTest {

    @Nested
    @DisplayName("Status code")
    class StatusCode {

        @Test
        @DisplayName("PENDING should have correct code")
        void pendingShouldHaveCorrectCode() {
            assertThat(BookingStatus.PENDING.code()).isEqualTo("pending");
        }

        @Test
        @DisplayName("CONFIRMED should have correct code")
        void confirmedShouldHaveCorrectCode() {
            assertThat(BookingStatus.CONFIRMED.code()).isEqualTo("confirmed");
        }

        @Test
        @DisplayName("CANCELLED should have correct code")
        void cancelledShouldHaveCorrectCode() {
            assertThat(BookingStatus.CANCELLED.code()).isEqualTo("cancelled");
        }
    }

    @Nested
    @DisplayName("Lifecycle capabilities")
    class LifecycleCapabilities {

        @ParameterizedTest
        @EnumSource(value = BookingStatus.class, names = {"PENDING", "CONFIRMED"})
        @DisplayName("PENDING and CONFIRMED should be modifiable")
        void pendingAndConfirmedShouldBeModifiable(BookingStatus status) {
            assertThat(status.isModifiable()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED should not be modifiable")
        void cancelledShouldNotBeModifiable() {
            assertThat(BookingStatus.CANCELLED.isModifiable()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = BookingStatus.class, names = {"PENDING", "CONFIRMED"})
        @DisplayName("PENDING and CONFIRMED should be cancellable")
        void pendingAndConfirmedShouldBeCancellable(BookingStatus status) {
            assertThat(status.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED should not be cancellable")
        void cancelledShouldNotBeCancellable() {
            assertThat(BookingStatus.CANCELLED.isCancellable()).isFalse();
        }

        @Test
        @DisplayName("only PENDING should be confirmable")
        void onlyPendingShouldBeConfirmable() {
            assertThat(BookingStatus.PENDING.isConfirmable()).isTrue();
            assertThat(BookingStatus.CONFIRMED.isConfirmable()).isFalse();
            assertThat(BookingStatus.CANCELLED.isConfirmable()).isFalse();
        }

        @Test
        @DisplayName("only CANCELLED should be terminal")
        void onlyCancelledShouldBeTerminal() {
            assertThat(BookingStatus.PENDING.isTerminal()).isFalse();
            assertThat(BookingStatus.CONFIRMED.isTerminal()).isFalse();
            assertThat(BookingStatus.CANCELLED.isTerminal()).isTrue();
        }
    }

    @Nested
    @DisplayName("fromCode")
    class FromCode {

        @Test
        @DisplayName("should return PENDING for pending code")
        void shouldReturnPendingForPendingCode() {
            assertThat(BookingStatus.fromCode("pending")).isEqualTo(BookingStatus.PENDING);
        }

        @Test
        @DisplayName("should return CONFIRMED for confirmed code")
        void shouldReturnConfirmedForConfirmedCode() {
            assertThat(BookingStatus.fromCode("confirmed")).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should return CANCELLED for cancelled code")
        void shouldReturnCancelledForCancelledCode() {
            assertThat(BookingStatus.fromCode("cancelled")).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("should reject null code")
        void shouldRejectNullCode() {
            assertThatThrownBy(() -> BookingStatus.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "PENDING", "unknown", "confirmed_status"})
        @DisplayName("should reject unknown code")
        void shouldRejectUnknownCode(String code) {
            assertThatThrownBy(() -> BookingStatus.fromCode(code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown booking status code");
        }
    }

    @Nested
    @DisplayName("Enum completeness")
    class EnumCompleteness {

        @Test
        @DisplayName("should have exactly 3 values")
        void shouldHaveExactlyThreeValues() {
            assertThat(BookingStatus.values()).hasSize(3);
        }

        @ParameterizedTest
        @EnumSource(BookingStatus.class)
        @DisplayName("fromCode should round-trip all values")
        void fromCodeShouldRoundTrip(BookingStatus status) {
            assertThat(BookingStatus.fromCode(status.code())).isEqualTo(status);
        }
    }
}
