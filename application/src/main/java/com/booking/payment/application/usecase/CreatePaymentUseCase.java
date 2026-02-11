package com.booking.payment.application.usecase;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.IdempotencyStore;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.shared.exception.ConflictException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Use case for creating payments with idempotency support.
 */
public class CreatePaymentUseCase {

    private static final int CREATED_STATUS = 201;

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;
    private final PaymentGatewayPort paymentGatewayPort;
    private final Clock clock;

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository,
            IdempotencyStore idempotencyStore,
            PaymentGatewayPort paymentGatewayPort
    ) {
        this(paymentRepository, idempotencyStore, paymentGatewayPort, Clock.systemUTC());
    }

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository,
            IdempotencyStore idempotencyStore,
            PaymentGatewayPort paymentGatewayPort,
            Clock clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore must not be null");
        this.paymentGatewayPort = Objects.requireNonNull(paymentGatewayPort, "paymentGatewayPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Executes payment creation flow.
     *
     * @param command create command
     * @return result with payment aggregate and idempotency flag
     */
    public Result execute(Command command) {
        Objects.requireNonNull(command, "command must not be null");

        String requestHash = calculateRequestHash(command);
        IdempotencyStore.IdempotencyRecord existingRecord = idempotencyStore.findByKey(command.idempotencyKey())
                .orElse(null);

        if (existingRecord != null) {
            if (!existingRecord.requestHash().equals(requestHash)) {
                throw new ConflictException(
                        "payment_idempotency_conflict",
                        "Idempotency key already used with different request"
                );
            }
            Payment existingPayment = paymentRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> new ConflictException(
                            "payment_idempotency_inconsistent",
                            "Idempotency record exists but payment not found"
                    ));
            return new Result(existingPayment, true);
        }

        Payment payment = Payment.create(
                command.bookingId(),
                command.userId(),
                command.money(),
                command.idempotencyKey(),
                command.description(),
                clock
        );
        PaymentGatewayPort.AuthorizationResult authorization = paymentGatewayPort.authorize(payment);
        if (authorization.success()) {
            payment.authorize(authorization.gatewayTransactionId());
        } else {
            payment.fail(authorization.failureReason());
        }

        Payment saved = paymentRepository.save(payment);
        Instant now = Instant.now(clock);
        idempotencyStore.save(new IdempotencyStore.IdempotencyRecord(
                command.idempotencyKey(),
                requestHash,
                CREATED_STATUS,
                "{\"paymentId\":\"" + saved.id().asString() + "\"}",
                now,
                now.plus(IdempotencyKey.TTL)
        ));

        return new Result(saved, false);
    }

    private String calculateRequestHash(Command command) {
        String canonical = String.join(
                "|",
                command.bookingId().asString(),
                Integer.toString(command.money().amount()),
                command.money().currency()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    public record Command(
            BookingId bookingId,
            UserId userId,
            Money money,
            IdempotencyKey idempotencyKey,
            String description
    ) {
        public Command {
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(money, "money must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        }
    }

    public record Result(Payment payment, boolean idempotencyHit) {
        public Result {
            Objects.requireNonNull(payment, "payment must not be null");
        }
    }
}
