package com.booking.booking.adapter.web;

import com.booking.booking.application.usecase.CancelBookingUseCase;
import com.booking.booking.application.usecase.CreateBookingUseCase;
import com.booking.booking.application.usecase.GetBookingUseCase;
import com.booking.booking.application.usecase.RefundPolicy;
import com.booking.booking.application.usecase.UpdateBookingUseCase;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreateBookingUseCase createBookingUseCase;
    private final GetBookingUseCase getBookingUseCase;
    private final UpdateBookingUseCase updateBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    public BookingController(
            CreateBookingUseCase createBookingUseCase,
            GetBookingUseCase getBookingUseCase,
            UpdateBookingUseCase updateBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase
    ) {
        this.createBookingUseCase = Objects.requireNonNull(createBookingUseCase, "createBookingUseCase must not be null");
        this.getBookingUseCase = Objects.requireNonNull(getBookingUseCase, "getBookingUseCase must not be null");
        this.updateBookingUseCase = Objects.requireNonNull(updateBookingUseCase, "updateBookingUseCase must not be null");
        this.cancelBookingUseCase = Objects.requireNonNull(cancelBookingUseCase, "cancelBookingUseCase must not be null");
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

    /**
     * Gets a booking detail.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable("bookingId") String bookingId,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        Booking booking = getBookingUseCase.execute(new GetBookingUseCase.GetBookingQuery(
                toBookingId(bookingId),
                requestUserId
        ));
        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    /**
     * Updates an existing booking.
     */
    @PutMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable("bookingId") String bookingId,
            @Valid @RequestBody UpdateBookingRequest request,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        TimeRange range = toOptionalTimeRange(request.startAt(), request.endAt());
        Booking booking = updateBookingUseCase.execute(new UpdateBookingUseCase.UpdateBookingCommand(
                toBookingId(bookingId),
                requestUserId,
                range,
                request.note(),
                request.version()
        ));
        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    /**
     * Cancels an existing booking.
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable("bookingId") String bookingId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CancelBookingRequest request,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        Booking booking = cancelBookingUseCase.execute(new CancelBookingUseCase.CancelBookingCommand(
                toBookingId(bookingId),
                requestUserId,
                request.reason(),
                request.refundPolicy(),
                request.partialRefundAmount(),
                request.refundPolicy() == RefundPolicy.NO_REFUND ? null : toIdempotencyKey(idempotencyKey)
        ));
        return ResponseEntity.ok(BookingResponse.from(booking));
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

    private TimeRange toOptionalTimeRange(Instant startAt, Instant endAt) {
        if (startAt == null && endAt == null) {
            return null;
        }
        if (startAt == null || endAt == null) {
            throw new ResponseStatusException(BAD_REQUEST, "startAt and endAt must be provided together");
        }
        return toTimeRange(startAt, endAt);
    }

    private BookingId toBookingId(String bookingId) {
        try {
            return BookingId.fromString(bookingId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid bookingId", ex);
        }
    }

    private IdempotencyKey toIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Idempotency-Key is required for refund policies");
        }
        try {
            return IdempotencyKey.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Idempotency-Key must be a UUID", ex);
        }
    }

    public record CreateBookingRequest(
            @NotNull java.util.UUID resourceId,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @Size(max = Booking.MAX_NOTE_LENGTH) String note
    ) {
    }

    public record UpdateBookingRequest(
            Instant startAt,
            Instant endAt,
            @Size(max = Booking.MAX_NOTE_LENGTH) String note,
            @NotNull @Min(1) Integer version
    ) {
    }

    public record CancelBookingRequest(
            @NotNull RefundPolicy refundPolicy,
            @Min(1) Integer partialRefundAmount,
            @Size(max = Booking.MAX_CANCEL_REASON_LENGTH) String reason
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
