package com.booking.booking.adapter.persistence;

import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaBookingRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("JpaBookingRepository integration")
class JpaBookingRepositoryIntegrationTest {

    @Autowired
    private JpaBookingRepository bookingRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("save/find")
    class SaveFind {

        @Test
        @DisplayName("should persist booking and find by id")
        void shouldPersistBookingAndFindById() {
            Booking booking = booking(
                    ResourceId.generate(),
                    Instant.parse("2026-03-03T10:00:00Z"),
                    Instant.parse("2026-03-03T11:00:00Z"),
                    BookingStatus.PENDING
            );

            Booking saved = bookingRepository.save(booking);
            entityManager.flush();
            entityManager.clear();

            assertThat(saved.id()).isNotNull();
            Booking reloaded = bookingRepository.findById(saved.id()).orElseThrow();
            assertThat(reloaded.id()).isEqualTo(saved.id());
            assertThat(reloaded.userId()).isEqualTo(saved.userId());
            assertThat(reloaded.resourceId()).isEqualTo(saved.resourceId());
        }
    }

    @Nested
    @DisplayName("conflict query")
    class ConflictQuery {

        @Test
        @DisplayName("hasConflict should return true for overlapping active booking")
        void hasConflictShouldReturnTrueForOverlappingActiveBooking() {
            ResourceId resourceId = ResourceId.generate();
            bookingRepository.save(booking(
                    resourceId,
                    Instant.parse("2026-03-03T10:00:00Z"),
                    Instant.parse("2026-03-03T12:00:00Z"),
                    BookingStatus.CONFIRMED
            ));

            boolean hasConflict = bookingRepository.hasConflict(
                    resourceId,
                    TimeRange.fromPersisted(
                            Instant.parse("2026-03-03T11:30:00Z"),
                            Instant.parse("2026-03-03T13:00:00Z")
                    )
            );

            assertThat(hasConflict).isTrue();
        }

        @Test
        @DisplayName("hasConflict should return false for adjacent booking")
        void hasConflictShouldReturnFalseForAdjacentBooking() {
            ResourceId resourceId = ResourceId.generate();
            bookingRepository.save(booking(
                    resourceId,
                    Instant.parse("2026-03-03T10:00:00Z"),
                    Instant.parse("2026-03-03T12:00:00Z"),
                    BookingStatus.PENDING
            ));

            boolean hasConflict = bookingRepository.hasConflict(
                    resourceId,
                    TimeRange.fromPersisted(
                            Instant.parse("2026-03-03T12:00:00Z"),
                            Instant.parse("2026-03-03T13:00:00Z")
                    )
            );

            assertThat(hasConflict).isFalse();
        }

        @Test
        @DisplayName("hasConflict(excluding) should ignore target booking itself")
        void hasConflictExcludingShouldIgnoreTargetBookingItself() {
            ResourceId resourceId = ResourceId.generate();
            Booking booking = bookingRepository.save(booking(
                    resourceId,
                    Instant.parse("2026-03-03T10:00:00Z"),
                    Instant.parse("2026-03-03T12:00:00Z"),
                    BookingStatus.PENDING
            ));

            boolean hasConflict = bookingRepository.hasConflict(
                    resourceId,
                    TimeRange.fromPersisted(
                            Instant.parse("2026-03-03T10:00:00Z"),
                            Instant.parse("2026-03-03T12:00:00Z")
                    ),
                    booking.id()
            );

            assertThat(hasConflict).isFalse();
        }
    }

    private Booking booking(ResourceId resourceId, Instant startAt, Instant endAt, BookingStatus status) {
        Booking.Builder builder = Booking.builder()
                .id(BookingId.generate())
                .userId(UserId.generate())
                .resourceId(resourceId)
                .timeRange(TimeRange.fromPersisted(startAt, endAt))
                .status(status)
                .version(Booking.INITIAL_VERSION)
                .createdAt(Instant.parse("2026-03-03T00:00:00Z"))
                .updatedAt(Instant.parse("2026-03-03T00:00:00Z"));

        if (status == BookingStatus.CANCELLED) {
            builder.cancelledAt(Instant.parse("2026-03-03T01:00:00Z"))
                    .cancelReason("cancelled");
        }
        return builder.build();
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.booking.booking.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.booking.booking.adapter.persistence.repository")
    static class TestApplication {
    }
}
