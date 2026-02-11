package com.booking.booking.application.port;

import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;

import java.util.Optional;

/**
 * Port interface for booking persistence operations.
 */
public interface BookingRepository {

    /**
     * Finds a booking by its identifier.
     *
     * @param bookingId booking identifier
     * @return booking if found
     */
    Optional<Booking> findById(BookingId bookingId);

    /**
     * Persists a booking aggregate.
     *
     * @param booking booking aggregate
     * @return persisted aggregate
     */
    Booking save(Booking booking);

    /**
     * Checks whether the given time range conflicts with existing active bookings
     * for the same resource.
     *
     * @param resourceId resource identifier
     * @param candidate candidate time range
     * @return true when conflict exists
     */
    boolean hasConflict(ResourceId resourceId, TimeRange candidate);

    /**
     * Checks whether the given time range conflicts with existing active bookings
     * for the same resource, excluding one booking (update use case).
     *
     * @param resourceId resource identifier
     * @param candidate candidate time range
     * @param excludeBookingId booking identifier to exclude from conflict check
     * @return true when conflict exists
     */
    boolean hasConflict(ResourceId resourceId, TimeRange candidate, BookingId excludeBookingId);
}
