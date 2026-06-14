package com.booking.payment.adapter.web;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.usecase.CapturePaymentUseCase;
import com.booking.payment.application.usecase.CreatePaymentUseCase;
import com.booking.payment.application.usecase.GetPaymentUseCase;
import com.booking.payment.application.usecase.RefundPaymentUseCase;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Payment API controller (contract: docs/api/openapi/payment.yaml).
 *
 * <p>POST /payments requires the {@code Idempotency-Key} header; a repeated
 * request with the same key and content returns the stored result with
 * HTTP 200 instead of 201.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CapturePaymentUseCase capturePaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase,
            CapturePaymentUseCase capturePaymentUseCase,
            RefundPaymentUseCase refundPaymentUseCase
    ) {
        this.createPaymentUseCase = Objects.requireNonNull(createPaymentUseCase, "createPaymentUseCase must not be null");
        this.getPaymentUseCase = Objects.requireNonNull(getPaymentUseCase, "getPaymentUseCase must not be null");
        this.capturePaymentUseCase = Objects.requireNonNull(capturePaymentUseCase, "capturePaymentUseCase must not be null");
        this.refundPaymentUseCase = Objects.requireNonNull(refundPaymentUseCase, "refundPaymentUseCase must not be null");
    }

    /**
     * Creates a payment idempotently.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        CreatePaymentUseCase.CreatePaymentResult result = createPaymentUseCase.execute(
                new CreatePaymentUseCase.CreatePaymentCommand(
                        toIdempotencyKey(idempotencyKey),
                        requestUserId,
                        BookingId.of(request.bookingId()),
                        toMoney(request.amount(), request.currency()),
                        request.description()
                ));

        Payment payment = result.payment();
        HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(IDEMPOTENCY_KEY_HEADER, payment.idempotencyKey().asString());
        if (!result.idempotentReplay()) {
            builder.location(URI.create("/api/v1/payments/" + payment.id().asString()));
        }
        return builder.body(PaymentResponse.from(payment));
    }

    /**
     * Gets a payment detail.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable("paymentId") String paymentId,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        Payment payment = getPaymentUseCase.execute(new GetPaymentUseCase.GetPaymentQuery(
                toPaymentId(paymentId),
                requestUserId
        ));
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    /**
     * Captures an authorized payment.
     */
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @PathVariable("paymentId") String paymentId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody(required = false) CapturePaymentRequest request,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        Payment payment = capturePaymentUseCase.execute(new CapturePaymentUseCase.CapturePaymentCommand(
                toPaymentId(paymentId),
                requestUserId,
                toIdempotencyKey(idempotencyKey),
                request != null ? request.amount() : null
        ));
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    /**
     * Refunds a captured payment, or voids an authorized payment.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody RefundPaymentRequest request,
            Principal principal
    ) {
        UserId requestUserId = resolveAuthenticatedUserId(principal);
        Payment payment = refundPaymentUseCase.execute(new RefundPaymentUseCase.RefundPaymentCommand(
                toPaymentId(paymentId),
                requestUserId,
                toIdempotencyKey(idempotencyKey),
                request.amount(),
                request.reason().name(),
                request.note()
        ));
        return ResponseEntity.ok(PaymentResponse.from(payment));
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

    private IdempotencyKey toIdempotencyKey(String value) {
        try {
            return IdempotencyKey.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Idempotency-Key must be a UUID", ex);
        }
    }

    private Money toMoney(Integer amount, String currency) {
        try {
            return Money.of(amount, currency);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid amount or currency", ex);
        }
    }

    private PaymentId toPaymentId(String paymentId) {
        try {
            return PaymentId.fromString(paymentId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid paymentId", ex);
        }
    }

    public record CreatePaymentRequest(
            @NotNull UUID bookingId,
            @NotNull @Min(1) Integer amount,
            @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = Payment.MAX_DESCRIPTION_LENGTH) String description
    ) {
    }

    public record CapturePaymentRequest(
            @Min(1) Integer amount
    ) {
    }

    public record RefundPaymentRequest(
            @Min(1) Integer amount,
            @NotNull RefundReason reason,
            @Size(max = 500) String note
    ) {
    }

    public enum RefundReason {
        CUSTOMER_REQUEST,
        DUPLICATE,
        FRAUDULENT,
        OTHER
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
            Instant createdAt,
            Instant updatedAt
    ) {
        private static PaymentResponse from(Payment payment) {
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
                    payment.createdAt(),
                    payment.updatedAt()
            );
        }
    }
}
