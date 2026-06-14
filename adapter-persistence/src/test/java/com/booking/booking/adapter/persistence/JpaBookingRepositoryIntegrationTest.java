package com.booking.booking.adapter.persistence;

import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaBookingRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("JpaBookingRepository integration")
class JpaBookingRepositoryIntegrationTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private JpaBookingRepository bookingRepository;

    @Nested
    @DisplayName("save/find")
    class SaveFind {

        @Test
        @DisplayName("should persist booking and find by id")
        void shouldPersistBookingAndFindById() {
            Booking booking = newBooking(
                    ResourceId.generate(),
                    range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z")
            );

            Booking saved = bookingRepository.save(booking);

            assertThat(bookingRepository.findById(saved.id()))
                    .hasValueSatisfying(reloaded -> {
                        assertThat(reloaded.id()).isEqualTo(saved.id());
                        assertThat(reloaded.userId()).isEqualTo(saved.userId());
                        assertThat(reloaded.resourceId()).isEqualTo(saved.resourceId());
                        assertThat(reloaded.timeRange()).isEqualTo(saved.timeRange());
                        assertThat(reloaded.status()).isEqualTo(BookingStatus.PENDING);
                        assertThat(reloaded.note()).isEqualTo("repository test");
                        assertThat(reloaded.version()).isEqualTo(Booking.INITIAL_VERSION);
                    });
        }

        @Test
        @DisplayName("should return empty when id does not exist")
        void shouldReturnEmptyWhenIdDoesNotExist() {
            assertThat(bookingRepository.findById(BookingId.generate())).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasConflict")
    class HasConflict {

        @Test
        @DisplayName("should detect overlapping booking for same resource")
        void shouldDetectOverlappingBookingForSameResource() {
            ResourceId resourceId = ResourceId.generate();
            bookingRepository.save(newBooking(
                    resourceId,
                    range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z")
            ));

            assertThat(bookingRepository.hasConflict(
                    resourceId,
                    range("2026-03-10T10:30:00Z", "2026-03-10T11:30:00Z")
            )).isTrue();
        }

        @Test
        @DisplayName("should not detect conflict for adjacent booking")
        void shouldNotDetectConflictForAdjacentBooking() {
            ResourceId resourceId = ResourceId.generate();
            bookingRepository.save(newBooking(
                    resourceId,
                    range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z")
            ));

            assertThat(bookingRepository.hasConflict(
                    resourceId,
                    range("2026-03-10T11:00:00Z", "2026-03-10T12:00:00Z")
            )).isFalse();
        }

        @Test
        @DisplayName("should ignore bookings for different resource")
        void shouldIgnoreBookingsForDifferentResource() {
            bookingRepository.save(newBooking(
                    ResourceId.generate(),
                    range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z")
            ));

            assertThat(bookingRepository.hasConflict(
                    ResourceId.generate(),
                    range("2026-03-10T10:30:00Z", "2026-03-10T11:30:00Z")
            )).isFalse();
        }

        @Test
        @DisplayName("should ignore cancelled bookings")
        void shouldIgnoreCancelledBookings() {
            ResourceId resourceId = ResourceId.generate();
            Booking booking = newBooking(
                    resourceId,
                    range("2026-03-10T10:00:00Z", "2026-03-10T11:00:00Z")
            );
            booking.cancel("no longer needed");
            bookingRepository.save(booking);

            assertThat(bookingRepository.hasConflict(
                    resourceId,
                    range("2026-03-10T10:30:00Z", "2026-03-10T11:30:00Z")
            )).isFalse();
        }
    }

    private static Booking newBooking(ResourceId resourceId, TimeRange timeRange) {
        return Booking.create(
                UserId.generate(),
                resourceId,
                timeRange,
                "repository test",
                FIXED_CLOCK
        );
    }

    private static TimeRange range(String startAt, String endAt) {
        return TimeRange.fromPersisted(Instant.parse(startAt), Instant.parse(endAt));
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.booking.booking.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.booking.booking.adapter.persistence.repository")
    static class TestApplication {
    }
}
