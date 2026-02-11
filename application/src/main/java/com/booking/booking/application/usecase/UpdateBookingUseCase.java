package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ConflictException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for updating existing bookings.
 */
public class UpdateBookingUseCase {

    private final BookingRepository bookingRepository;

    public UpdateBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
    }

    /**
     * Updates booking time range and optional note.
     *
     * @param command update command
     * @return updated booking
     */
    public Booking execute(UpdateBookingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking",
                        command.bookingId().asString(),
                        "Booking not found"
                ));

        if (!booking.isOwnedBy(command.requestUserId())) {
            throw new ForbiddenException(
                    "booking_access_denied",
                    "Only booking owner can update booking"
            );
        }

        if (bookingRepository.hasConflict(booking.resourceId(), command.timeRange(), booking.id())) {
            throw new ConflictException(
                    "booking_time_conflict",
                    "Booking time range conflicts with an existing booking"
            );
        }

        booking.updateTimeRange(command.timeRange(), command.expectedVersion());
        if (!Objects.equals(booking.note(), command.note())) {
            booking.updateNote(command.note(), booking.version());
        }

        return bookingRepository.save(booking);
    }

    public record UpdateBookingCommand(
            BookingId bookingId,
            UserId requestUserId,
            TimeRange timeRange,
            String note,
            int expectedVersion
    ) {
        public UpdateBookingCommand {
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
            Objects.requireNonNull(timeRange, "timeRange must not be null");
            if (expectedVersion < Booking.INITIAL_VERSION) {
                throw new IllegalArgumentException("expectedVersion must be positive");
            }
        }
    }
}
