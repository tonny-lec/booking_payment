package com.booking.booking.adapter.persistence.entity;

import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping for bookings table.
 */
@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookingEntity() {
    }

    public static BookingEntity fromDomain(Booking booking) {
        BookingEntity entity = new BookingEntity();
        entity.id = booking.id().value();
        entity.userId = booking.userId().value();
        entity.resourceId = booking.resourceId().value();
        entity.startAt = booking.timeRange().startAt();
        entity.endAt = booking.timeRange().endAt();
        entity.status = booking.status();
        entity.note = booking.note();
        entity.version = booking.version();
        entity.cancelledAt = booking.cancelledAt();
        entity.cancelReason = booking.cancelReason();
        entity.createdAt = booking.createdAt();
        entity.updatedAt = booking.updatedAt();
        return entity;
    }

    public Booking toDomain() {
        return Booking.builder()
                .id(BookingId.of(id))
                .userId(UserId.of(userId))
                .resourceId(ResourceId.of(resourceId))
                .timeRange(TimeRange.fromPersisted(startAt, endAt))
                .status(status)
                .note(note)
                .version(version)
                .cancelledAt(cancelledAt)
                .cancelReason(cancelReason)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
