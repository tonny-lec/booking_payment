package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for canceling bookings.
 */
public class CancelBookingUseCase {

    private final BookingRepository bookingRepository;

    public CancelBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
    }

    /**
     * Cancels an existing booking.
     *
     * @param command cancel command
     * @return cancelled booking
     */
    public Booking execute(CancelBookingCommand command) {
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
                    "Only booking owner can cancel booking"
            );
        }

        booking.cancel(command.reason());
        return bookingRepository.save(booking);
    }

    public record CancelBookingCommand(
            BookingId bookingId,
            UserId requestUserId,
            String reason
    ) {
        public CancelBookingCommand {
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
        }
    }
}
