package com.booking.booking.adapter.web;

import com.booking.booking.application.usecase.CreateBookingUseCase;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Booking API controller.
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;

    public BookingController(CreateBookingUseCase createBookingUseCase) {
        this.createBookingUseCase = Objects.requireNonNull(createBookingUseCase, "createBookingUseCase must not be null");
    }

    /**
     * Creates a new booking.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        TimeRange range = toTimeRange(request.startAt(), request.endAt());
        Booking booking = createBookingUseCase.execute(new CreateBookingUseCase.CreateBookingCommand(
                requestUserId,
                ResourceId.of(request.resourceId()),
                range,
                request.note()
        ));

        return ResponseEntity.created(URI.create("/api/v1/bookings/" + booking.id().asString()))
                .body(BookingResponse.from(booking));
    }

    private UserId resolveAuthenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("unauthorized", "Authentication is required");
        }
        try {
            return UserId.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("unauthorized", "Authentication is required");
        }
    }

    private TimeRange toTimeRange(Instant startAt, Instant endAt) {
        try {
            return TimeRange.of(startAt, endAt);
        } catch (IllegalArgumentException | BusinessRuleViolationException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid time range", ex);
        }
    }

    public record CreateBookingRequest(
            @NotNull java.util.UUID resourceId,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @Size(max = Booking.MAX_NOTE_LENGTH) String note
    ) {
    }

    public record BookingResponse(
            String id,
            String userId,
            String resourceId,
            Instant startAt,
            Instant endAt,
            String status,
            String note,
            int version,
            Instant createdAt,
            Instant updatedAt
    ) {
        private static BookingResponse from(Booking booking) {
            return new BookingResponse(
                    booking.id().asString(),
                    booking.userId().asString(),
                    booking.resourceId().asString(),
                    booking.timeRange().startAt(),
                    booking.timeRange().endAt(),
                    booking.status().code(),
                    booking.note(),
                    booking.version(),
                    booking.createdAt(),
                    booking.updatedAt()
            );
        }
    }
}
