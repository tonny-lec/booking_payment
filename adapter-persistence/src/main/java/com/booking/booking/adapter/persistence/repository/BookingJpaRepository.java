package com.booking.booking.adapter.persistence.repository;

import com.booking.booking.adapter.persistence.entity.BookingEntity;
import com.booking.booking.domain.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Spring Data JPA repository for bookings.
 */
public interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    @Query("""
            select count(b) > 0
            from BookingEntity b
            where b.resourceId = :resourceId
              and b.status in :activeStatuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsConflict(
            @Param("resourceId") UUID resourceId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );

    @Query("""
            select count(b) > 0
            from BookingEntity b
            where b.resourceId = :resourceId
              and b.status in :activeStatuses
              and b.startAt < :endAt
              and b.endAt > :startAt
              and b.id <> :excludeBookingId
            """)
    boolean existsConflictExcluding(
            @Param("resourceId") UUID resourceId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeBookingId") UUID excludeBookingId,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );
}
