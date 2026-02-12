package com.booking.booking.adapter.persistence;

import com.booking.booking.adapter.persistence.entity.BookingEntity;
import com.booking.booking.adapter.persistence.repository.BookingJpaRepository;
import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.Optional;

/**
 * JPA-based implementation of {@link BookingRepository}.
 */
@Repository
public class JpaBookingRepository implements BookingRepository {

    private static final EnumSet<BookingStatus> ACTIVE_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingJpaRepository bookingJpaRepository;

    public JpaBookingRepository(BookingJpaRepository bookingJpaRepository) {
        this.bookingJpaRepository = bookingJpaRepository;
    }

    @Override
    public Optional<Booking> findById(BookingId bookingId) {
        return bookingJpaRepository.findById(bookingId.value())
                .map(BookingEntity::toDomain);
    }

    @Override
    public Booking save(Booking booking) {
        return bookingJpaRepository.save(BookingEntity.fromDomain(booking)).toDomain();
    }

    @Override
    public boolean hasConflict(ResourceId resourceId, TimeRange candidate) {
        return bookingJpaRepository.existsConflict(
                resourceId.value(),
                candidate.startAt(),
                candidate.endAt(),
                ACTIVE_STATUSES
        );
    }

    @Override
    public boolean hasConflict(ResourceId resourceId, TimeRange candidate, BookingId excludeBookingId) {
        return bookingJpaRepository.existsConflictExcluding(
                resourceId.value(),
                candidate.startAt(),
                candidate.endAt(),
                excludeBookingId.value(),
                ACTIVE_STATUSES
        );
    }
}
