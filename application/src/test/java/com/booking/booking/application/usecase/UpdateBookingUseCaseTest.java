package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateBookingUseCase")
class UpdateBookingUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("execute should reject no-op update when expected version is stale")
    void executeShouldRejectNoOpUpdateWhenExpectedVersionIsStale() {
        Booking booking = pendingBooking();
        UpdateBookingUseCase useCase = new UpdateBookingUseCase(bookingRepository);
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        UpdateBookingUseCase.UpdateBookingCommand command = new UpdateBookingUseCase.UpdateBookingCommand(
                booking.id(),
                booking.userId(),
                null,
                booking.note(),
                booking.version() + 1
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOfSatisfying(ConflictException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("booking_version_mismatch"));

        verify(bookingRepository, never()).save(booking);
    }

    @Test
    @DisplayName("execute should reject no-op update when booking is cancelled")
    void executeShouldRejectNoOpUpdateWhenBookingIsCancelled() {
        Booking booking = cancelledBooking();
        UpdateBookingUseCase useCase = new UpdateBookingUseCase(bookingRepository);
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        UpdateBookingUseCase.UpdateBookingCommand command = new UpdateBookingUseCase.UpdateBookingCommand(
                booking.id(),
                booking.userId(),
                null,
                booking.note(),
                booking.version()
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("booking_not_modifiable"));

        verify(bookingRepository, never()).save(booking);
    }

    @Test
    @DisplayName("execute should treat whitespace-only note diff as no-op after normalization")
    void executeShouldTreatWhitespaceOnlyNoteDiffAsNoOpAfterNormalization() {
        Booking booking = pendingBooking();
        UpdateBookingUseCase useCase = new UpdateBookingUseCase(bookingRepository);
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        UpdateBookingUseCase.UpdateBookingCommand command = new UpdateBookingUseCase.UpdateBookingCommand(
                booking.id(),
                booking.userId(),
                null,
                " note  ",
                booking.version()
        );

        Booking result = useCase.execute(command);

        assertThat(result.version()).isEqualTo(booking.version());
        verify(bookingRepository, never()).save(booking);
    }

    private static Booking pendingBooking() {
        Instant now = Instant.parse("2026-03-01T00:00:00Z");
        return Booking.builder()
                .id(BookingId.generate())
                .userId(UserId.generate())
                .resourceId(ResourceId.generate())
                .timeRange(TimeRange.fromPersisted(
                        Instant.parse("2026-03-10T10:00:00Z"),
                        Instant.parse("2026-03-10T11:00:00Z")
                ))
                .status(BookingStatus.PENDING)
                .note("note")
                .version(Booking.INITIAL_VERSION)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static Booking cancelledBooking() {
        Instant now = Instant.parse("2026-03-01T00:00:00Z");
        Instant cancelledAt = Instant.parse("2026-03-02T00:00:00Z");
        return Booking.builder()
                .id(BookingId.generate())
                .userId(UserId.generate())
                .resourceId(ResourceId.generate())
                .timeRange(TimeRange.fromPersisted(
                        Instant.parse("2026-03-10T10:00:00Z"),
                        Instant.parse("2026-03-10T11:00:00Z")
                ))
                .status(BookingStatus.CANCELLED)
                .note("note")
                .version(Booking.INITIAL_VERSION + 1)
                .cancelledAt(cancelledAt)
                .cancelReason("cancelled")
                .createdAt(now)
                .updatedAt(cancelledAt)
                .build();
    }
}
