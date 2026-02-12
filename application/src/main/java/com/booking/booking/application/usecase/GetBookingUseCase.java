package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for retrieving a booking.
 */
public class GetBookingUseCase {

    private final BookingRepository bookingRepository;

    public GetBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
    }

    /**
     * Retrieves a booking when the requester is owner.
     *
     * @param query get query
     * @return booking aggregate
     */
    public Booking execute(GetBookingQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Booking booking = bookingRepository.findById(query.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking",
                        query.bookingId().asString(),
                        "Booking not found"
                ));

        if (!booking.isOwnedBy(query.requestUserId())) {
            throw new ForbiddenException(
                    "booking_access_denied",
                    "Only booking owner can access booking"
            );
        }
        return booking;
    }

    public record GetBookingQuery(BookingId bookingId, UserId requestUserId) {
        public GetBookingQuery {
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
        }
    }
}
