package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBookingUseCase")
class CreateBookingUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("execute should save booking when there is no conflict")
    void executeShouldSaveBookingWhenThereIsNoConflict() {
        CreateBookingUseCase useCase = new CreateBookingUseCase(bookingRepository, FIXED_CLOCK);
        CreateBookingUseCase.CreateBookingCommand command = command();
        when(bookingRepository.hasConflict(command.resourceId(), command.timeRange())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = useCase.execute(command);

        assertThat(result.userId()).isEqualTo(command.userId());
        assertThat(result.resourceId()).isEqualTo(command.resourceId());
        assertThat(result.timeRange()).isEqualTo(command.timeRange());
        assertThat(result.note()).isEqualTo("planning");
        assertThat(result.createdAt()).isEqualTo(Instant.now(FIXED_CLOCK));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("execute should reject booking when repository reports conflict")
    void executeShouldRejectBookingWhenRepositoryReportsConflict() {
        CreateBookingUseCase useCase = new CreateBookingUseCase(bookingRepository, FIXED_CLOCK);
        CreateBookingUseCase.CreateBookingCommand command = command();
        when(bookingRepository.hasConflict(command.resourceId(), command.timeRange())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOfSatisfying(ConflictException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("booking_time_conflict"));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    private static CreateBookingUseCase.CreateBookingCommand command() {
        return new CreateBookingUseCase.CreateBookingCommand(
                UserId.generate(),
                ResourceId.generate(),
                TimeRange.fromPersisted(
                        Instant.parse("2026-03-10T10:00:00Z"),
                        Instant.parse("2026-03-10T11:00:00Z")
                ),
                " planning "
        );
    }
}
