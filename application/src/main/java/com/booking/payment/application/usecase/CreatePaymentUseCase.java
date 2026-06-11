package com.booking.payment.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case for creating payments idempotently.
 *
 * <p>Flow (see {@code docs/design/usecases/payment-create.md}):
 * <ol>
 *   <li>Idempotency check: a payment already created with the same key and the
 *       same request content is returned as-is ({@code idempotentReplay=true},
 *       HTTP 200); the same key with different content is rejected (409).</li>
 *   <li>Booking validation: existence (404), ownership (403) and payable
 *       state (422, e.g. cancelled or already paid).</li>
 *   <li>Gateway authorization via {@link PaymentGatewayPort} (ACL): the
 *       payment transitions to AUTHORIZED, or FAILED when declined.</li>
 *   <li>Persistence: the payments table enforces idempotency-key uniqueness
 *       as the final guard against concurrent duplicates.</li>
 * </ol>
 *
 * <p>Request identity is compared on {@code userId/bookingId/amount/currency}
 * (the user check prevents replaying another user's payment). Key TTL (24h)
 * enforcement is deferred to the idempotency-records store
 * (see {@code docs/tasks/implementation-slice-a.md} PAY-A-02).
 */
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGatewayPort paymentGateway;
    private final Clock clock;

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            PaymentGatewayPort paymentGateway
    ) {
        this(paymentRepository, bookingRepository, paymentGateway, Clock.systemUTC());
    }

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            PaymentGatewayPort paymentGateway,
            Clock clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Creates a payment or replays the stored result for a repeated request.
     *
     * @param command create command
     * @return created (or replayed) payment with the replay flag
     */
    public CreatePaymentResult execute(CreatePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(command.idempotencyKey().value());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!matchesRequest(payment, command)) {
                throw new ConflictException(
                        "payment_idempotency_key_conflict",
                        "Idempotency-Key was already used with a different request content"
                );
            }
            return new CreatePaymentResult(payment, true);
        }

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking",
                        command.bookingId().asString(),
                        "Booking not found"
                ));
        if (!booking.isOwnedBy(command.userId())) {
            throw new ForbiddenException(
                    "payment_booking_access_denied",
                    "Only booking owner can pay for the booking"
            );
        }
        if (booking.status().isTerminal()) {
            throw new BusinessRuleViolationException(
                    "payment_booking_not_payable",
                    "Booking is not in a payable state: " + booking.status()
            );
        }
        if (paymentRepository.existsActiveByBookingId(command.bookingId())) {
            throw new BusinessRuleViolationException(
                    "payment_booking_already_paid",
                    "An active payment already exists for the booking"
            );
        }

        Payment payment = Payment.create(
                command.bookingId(),
                command.userId(),
                command.money(),
                command.idempotencyKey(),
                command.description(),
                clock
        );

        PaymentGatewayPort.AuthorizationResult authorization = paymentGateway.authorize(
                new PaymentGatewayPort.AuthorizationRequest(payment.id(), payment.bookingId(), payment.money())
        );
        if (authorization.authorized()) {
            payment.authorize(authorization.gatewayTransactionId());
        } else {
            payment.fail(authorization.failureReason());
        }

        return new CreatePaymentResult(paymentRepository.save(payment), false);
    }

    private boolean matchesRequest(Payment payment, CreatePaymentCommand command) {
        return payment.userId().equals(command.userId())
                && payment.bookingId().equals(command.bookingId())
                && payment.money().equals(command.money());
    }

    public record CreatePaymentCommand(
            IdempotencyKey idempotencyKey,
            UserId userId,
            BookingId bookingId,
            Money money,
            String description
    ) {
        public CreatePaymentCommand {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(money, "money must not be null");
        }
    }

    /**
     * Use case result.
     *
     * @param payment created or replayed payment
     * @param idempotentReplay true when an existing result was returned
     *        (the web adapter maps this to HTTP 200 instead of 201)
     */
    public record CreatePaymentResult(Payment payment, boolean idempotentReplay) {
        public CreatePaymentResult {
            Objects.requireNonNull(payment, "payment must not be null");
        }
    }
}
