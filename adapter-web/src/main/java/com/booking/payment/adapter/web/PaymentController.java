package com.booking.payment.adapter.web;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.usecase.CreatePaymentUseCase;
import com.booking.payment.application.usecase.GetPaymentUseCase;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment endpoints.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreatePaymentUseCase createPaymentUseCase;
    @SuppressWarnings("unused")
    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase
    ) {
        this.createPaymentUseCase = Objects.requireNonNull(createPaymentUseCase, "createPaymentUseCase must not be null");
        this.getPaymentUseCase = Objects.requireNonNull(getPaymentUseCase, "getPaymentUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKeyHeader,
            @Valid @RequestBody CreatePaymentRequest request,
            Principal principal
    ) {
        UserId userId = resolveAuthenticatedUserId(principal);
        IdempotencyKey idempotencyKey = parseIdempotencyKey(idempotencyKeyHeader);
        CreatePaymentUseCase.Command command = new CreatePaymentUseCase.Command(
                parseBookingId(request.bookingId()),
                userId,
                Money.of(request.amount(), request.currency()),
                idempotencyKey,
                request.description()
        );

        CreatePaymentUseCase.Result result = createPaymentUseCase.execute(command);
        PaymentResponse response = toResponse(result.payment());
        HttpHeaders headers = new HttpHeaders();
        headers.add(IDEMPOTENCY_KEY_HEADER, idempotencyKey.asString());

        if (result.idempotencyHit()) {
            return new ResponseEntity<>(response, headers, HttpStatus.OK);
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(result.payment().id().asString())
                .toUri();
        headers.setLocation(location);
        return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.id().asString(),
                payment.bookingId().asString(),
                payment.userId().asString(),
                payment.money().amount(),
                payment.capturedAmount(),
                payment.refundedAmount(),
                payment.money().currency(),
                payment.status().code(),
                payment.description(),
                payment.gatewayTransactionId(),
                payment.failureReason(),
                payment.idempotencyKey().asString(),
                payment.createdAt().toString(),
                payment.updatedAt().toString()
        );
    }

    private UserId resolveAuthenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("unauthorized", "Authentication is required");
        }
        return UserId.fromString(principal.getName());
    }

    private BookingId parseBookingId(String value) {
        try {
            return BookingId.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bookingId", ex);
        }
    }

    private IdempotencyKey parseIdempotencyKey(String value) {
        try {
            return IdempotencyKey.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key", ex);
        }
    }

    public record CreatePaymentRequest(
            @NotBlank String bookingId,
            @NotNull @Min(1) Integer amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @Size(max = Payment.MAX_DESCRIPTION_LENGTH) String description
    ) {
    }

    public record PaymentResponse(
            String id,
            String bookingId,
            String userId,
            int amount,
            Integer capturedAmount,
            Integer refundedAmount,
            String currency,
            String status,
            String description,
            String gatewayTransactionId,
            String failureReason,
            String idempotencyKey,
            String createdAt,
            String updatedAt
    ) {
    }
}
