package com.booking.booking.domain.model;

import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Booking")
class BookingTest {

    private static final Instant NOW = Instant.parse("2000-01-01T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("create() should create pending booking with initial values")
        void createShouldCreatePendingBookingWithInitialValues() {
            UserId userId = UserId.generate();
            ResourceId resourceId = ResourceId.generate();
            TimeRange timeRange = TimeRange.of(NOW.plusSeconds(3600), NOW.plusSeconds(7200), FIXED_CLOCK);

            Booking booking = Booking.create(userId, resourceId, timeRange, "  team sync  ", FIXED_CLOCK);

            assertThat(booking.id()).isNotNull();
            assertThat(booking.userId()).isEqualTo(userId);
            assertThat(booking.resourceId()).isEqualTo(resourceId);
            assertThat(booking.timeRange()).isEqualTo(timeRange);
            assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
            assertThat(booking.note()).isEqualTo("team sync");
            assertThat(booking.version()).isEqualTo(Booking.INITIAL_VERSION);
            assertThat(booking.cancelledAt()).isNull();
            assertThat(booking.cancelReason()).isNull();
            assertThat(booking.createdAt()).isEqualTo(NOW);
            assertThat(booking.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("create() should normalize blank note to null")
        void createShouldNormalizeBlankNoteToNull() {
            Booking booking = newBookingWithClock("   ");

            assertThat(booking.note()).isNull();
        }

        @Test
        @DisplayName("create() should reject too long note")
        void createShouldRejectTooLongNote() {
            String tooLong = "a".repeat(Booking.MAX_NOTE_LENGTH + 1);

            assertThatThrownBy(() -> newBookingWithClock(tooLong))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("booking_note_too_long"));
        }
    }

    @Nested
    @DisplayName("Update operations")
    class UpdateOperations {

        @Test
        @DisplayName("updateTimeRange() should update range and increment version")
        void updateTimeRangeShouldUpdateRangeAndIncrementVersion() {
            Booking booking = newBookingWithClock(null);
            TimeRange newRange = TimeRange.of(NOW.plusSeconds(10800), NOW.plusSeconds(14400), FIXED_CLOCK);

            booking.updateTimeRange(newRange, Booking.INITIAL_VERSION);

            assertThat(booking.timeRange()).isEqualTo(newRange);
            assertThat(booking.version()).isEqualTo(Booking.INITIAL_VERSION + 1);
            assertThat(booking.updatedAt()).isAfterOrEqualTo(booking.createdAt());
        }

        @Test
        @DisplayName("updateNote() should normalize note and increment version")
        void updateNoteShouldNormalizeNoteAndIncrementVersion() {
            Booking booking = newBookingWithClock("initial");

            booking.updateNote("  updated note  ", Booking.INITIAL_VERSION);

            assertThat(booking.note()).isEqualTo("updated note");
            assertThat(booking.version()).isEqualTo(Booking.INITIAL_VERSION + 1);
        }

        @Test
        @DisplayName("update methods should reject version mismatch")
        void updateMethodsShouldRejectVersionMismatch() {
            Booking booking = newBookingWithClock(null);

            assertThatThrownBy(() -> booking.updateNote("note", Booking.INITIAL_VERSION + 1))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(ex -> assertThat(((ConflictException) ex).getErrorCode())
                            .isEqualTo("booking_version_mismatch"));

            assertThat(booking.version()).isEqualTo(Booking.INITIAL_VERSION);
        }

        @Test
        @DisplayName("update methods should reject when booking is cancelled")
        void updateMethodsShouldRejectWhenBookingIsCancelled() {
            Booking booking = newBookingWithClock(null);
            booking.cancel("no longer needed");
            int cancelledVersion = booking.version();

            assertThatThrownBy(() -> booking.updateNote("new", cancelledVersion))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("booking_not_modifiable"));
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("confirm() should move PENDING to CONFIRMED")
        void confirmShouldMovePendingToConfirmed() {
            Booking booking = newBookingWithClock(null);

            booking.confirm();

            assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking.version()).isEqualTo(Booking.INITIAL_VERSION + 1);
        }

        @Test
        @DisplayName("confirm() should reject invalid state")
        void confirmShouldRejectInvalidState() {
            Booking booking = newBookingWithClock(null);
            booking.cancel("cancel first");

            assertThatThrownBy(booking::confirm)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("booking_invalid_state"));
        }

        @Test
        @DisplayName("cancel() should move to CANCELLED and set metadata")
        void cancelShouldMoveToCancelledAndSetMetadata() {
            Booking booking = newBookingWithClock(null);

            booking.cancel("  user request  ");

            assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.cancelledAt()).isNotNull();
            assertThat(booking.cancelReason()).isEqualTo("user request");
            assertThat(booking.version()).isEqualTo(Booking.INITIAL_VERSION + 1);
            assertThat(booking.updatedAt()).isAfterOrEqualTo(booking.cancelledAt());
        }

        @Test
        @DisplayName("cancel() should reject already cancelled booking")
        void cancelShouldRejectAlreadyCancelledBooking() {
            Booking booking = newBookingWithClock(null);
            booking.cancel("first");

            assertThatThrownBy(() -> booking.cancel("second"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("booking_already_cancelled"));
        }

        @Test
        @DisplayName("cancel() should reject too long reason")
        void cancelShouldRejectTooLongReason() {
            Booking booking = newBookingWithClock(null);
            String tooLongReason = "r".repeat(Booking.MAX_CANCEL_REASON_LENGTH + 1);

            assertThatThrownBy(() -> booking.cancel(tooLongReason))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("booking_cancelReason_too_long"));
        }
    }

    @Nested
    @DisplayName("Builder and ownership")
    class BuilderAndOwnership {

        @Test
        @DisplayName("builder should reject cancel metadata for non-cancelled status")
        void builderShouldRejectCancelMetadataForNonCancelledStatus() {
            assertThatThrownBy(() -> Booking.builder()
                    .id(BookingId.generate())
                    .userId(UserId.generate())
                    .resourceId(ResourceId.generate())
                    .timeRange(TimeRange.fromPersisted(NOW.plusSeconds(3600), NOW.plusSeconds(7200)))
                    .status(BookingStatus.PENDING)
                    .cancelledAt(NOW)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cancel metadata");
        }

        @Test
        @DisplayName("builder should require cancelledAt when status is CANCELLED")
        void builderShouldRequireCancelledAtWhenCancelled() {
            assertThatThrownBy(() -> Booking.builder()
                    .id(BookingId.generate())
                    .userId(UserId.generate())
                    .resourceId(ResourceId.generate())
                    .timeRange(TimeRange.fromPersisted(NOW.plusSeconds(3600), NOW.plusSeconds(7200)))
                    .status(BookingStatus.CANCELLED)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cancelledAt");
        }

        @Test
        @DisplayName("isOwnedBy() should return true only for owner")
        void isOwnedByShouldReturnTrueOnlyForOwner() {
            UserId owner = UserId.generate();
            Booking booking = Booking.builder()
                    .id(BookingId.generate())
                    .userId(owner)
                    .resourceId(ResourceId.generate())
                    .timeRange(TimeRange.fromPersisted(NOW.plusSeconds(3600), NOW.plusSeconds(7200)))
                    .status(BookingStatus.PENDING)
                    .build();

            assertThat(booking.isOwnedBy(owner)).isTrue();
            assertThat(booking.isOwnedBy(UserId.generate())).isFalse();
        }
    }

    private Booking newBookingWithClock(String note) {
        return Booking.create(
                UserId.generate(),
                ResourceId.generate(),
                TimeRange.of(NOW.plusSeconds(3600), NOW.plusSeconds(7200), FIXED_CLOCK),
                note,
                FIXED_CLOCK
        );
    }
}
