package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ConflictException;

import java.time.Clock;
import java.util.Objects;

/**
 * Use case for creating bookings.
 */
public class CreateBookingUseCase {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    public CreateBookingUseCase(BookingRepository bookingRepository) {
        this(bookingRepository, Clock.systemUTC());
    }

    public CreateBookingUseCase(BookingRepository bookingRepository, Clock clock) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Creates a new booking after conflict validation.
     *
     * @param command create command
     * @return created booking
     */
    public Booking execute(CreateBookingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        if (bookingRepository.hasConflict(command.resourceId(), command.timeRange())) {
            throw new ConflictException(
                    "booking_time_conflict",
                    "Booking time range conflicts with an existing booking"
            );
        }

        Booking booking = Booking.create(
                command.userId(),
                command.resourceId(),
                command.timeRange(),
                command.note(),
                clock
        );
        return bookingRepository.save(booking);
    }

    public record CreateBookingCommand(
            UserId userId,
            ResourceId resourceId,
            TimeRange timeRange,
            String note
    ) {
        public CreateBookingCommand {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(resourceId, "resourceId must not be null");
            Objects.requireNonNull(timeRange, "timeRange must not be null");
        }
    }
}
