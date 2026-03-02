package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CreateBookingUseCase")
class CreateBookingUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("execute creates booking when no conflict exists")
    void executeCreatesBookingWhenNoConflictExists() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        CreateBookingUseCase useCase = new CreateBookingUseCase(bookingRepository, FIXED_CLOCK);

        UserId userId = UserId.generate();
        ResourceId resourceId = ResourceId.generate();
        TimeRange range = TimeRange.of(NOW.plusSeconds(3600), NOW.plusSeconds(7200), FIXED_CLOCK);
        CreateBookingUseCase.CreateBookingCommand command =
                new CreateBookingUseCase.CreateBookingCommand(userId, resourceId, range, "note");

        when(bookingRepository.hasConflict(resourceId, range)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking created = useCase.execute(command);

        assertThat(created.userId()).isEqualTo(userId);
        assertThat(created.resourceId()).isEqualTo(resourceId);
        assertThat(created.timeRange()).isEqualTo(range);
        assertThat(created.status().code()).isEqualTo("PENDING");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("execute throws ConflictException when conflict exists")
    void executeThrowsConflictExceptionWhenConflictExists() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        CreateBookingUseCase useCase = new CreateBookingUseCase(bookingRepository, FIXED_CLOCK);

        UserId userId = UserId.generate();
        ResourceId resourceId = ResourceId.generate();
        TimeRange range = TimeRange.of(NOW.plusSeconds(3600), NOW.plusSeconds(7200), FIXED_CLOCK);
        CreateBookingUseCase.CreateBookingCommand command =
                new CreateBookingUseCase.CreateBookingCommand(userId, resourceId, range, null);

        when(bookingRepository.hasConflict(resourceId, range)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("conflicts");

        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
