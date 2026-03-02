package com.booking.payment.application.usecase;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.IdempotencyStore;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePaymentUseCase idempotency")
class CreatePaymentUseCaseIdempotencyTest {

    private static final Instant NOW = Instant.parse("2026-02-11T11:30:00Z");

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private IdempotencyStore idempotencyStore;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    private CreatePaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePaymentUseCase(
                paymentRepository,
                idempotencyStore,
                paymentGatewayPort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("should return stored payment when same idempotency key and same request is sent")
    void shouldReturnStoredPaymentWhenSameIdempotencyKeyAndSameRequest() {
        CreatePaymentUseCase.Command command = createCommand(3000);
        String requestHash = requestHash(command);
        Payment existing = createAuthorizedPayment(command);

        when(idempotencyStore.findByKey(command.idempotencyKey()))
                .thenReturn(Optional.of(new IdempotencyStore.IdempotencyRecord(
                        command.idempotencyKey(),
                        requestHash,
                        201,
                        "{\"paymentId\":\"" + existing.id().asString() + "\"}",
                        NOW.minusSeconds(10),
                        NOW.plusSeconds(3600)
                )));
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey()))
                .thenReturn(Optional.of(existing));

        CreatePaymentUseCase.Result result = useCase.execute(command);

        assertThat(result.idempotencyHit()).isTrue();
        assertThat(result.payment().id()).isEqualTo(existing.id());
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.AUTHORIZED);
        verify(paymentRepository).findByIdempotencyKey(command.idempotencyKey());
        verifyNoInteractions(paymentGatewayPort);
    }

    @Test
    @DisplayName("should throw conflict when same idempotency key is used with different request")
    void shouldThrowConflictWhenSameIdempotencyKeyUsedWithDifferentRequest() {
        CreatePaymentUseCase.Command command = createCommand(3000);
        String differentHash = requestHash(createCommand(3500));

        when(idempotencyStore.findByKey(command.idempotencyKey()))
                .thenReturn(Optional.of(new IdempotencyStore.IdempotencyRecord(
                        command.idempotencyKey(),
                        differentHash,
                        201,
                        "{\"paymentId\":\"dummy\"}",
                        NOW.minusSeconds(10),
                        NOW.plusSeconds(3600)
                )));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getErrorCode())
                        .isEqualTo("payment_idempotency_conflict"));

        verifyNoInteractions(paymentGatewayPort, paymentRepository);
    }

    private CreatePaymentUseCase.Command createCommand(int amount) {
        return new CreatePaymentUseCase.Command(
                BookingId.fromString("550e8400-e29b-41d4-a716-446655440010"),
                UserId.fromString("550e8400-e29b-41d4-a716-446655440011"),
                Money.of(amount, "JPY"),
                IdempotencyKey.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440012")),
                "idempotency test"
        );
    }

    private Payment createAuthorizedPayment(CreatePaymentUseCase.Command command) {
        Payment payment = Payment.create(
                command.bookingId(),
                command.userId(),
                command.money(),
                command.idempotencyKey(),
                command.description(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        payment.authorize("stub-tx-existing");
        return payment;
    }

    private String requestHash(CreatePaymentUseCase.Command command) {
        String canonical = String.join(
                "|",
                command.bookingId().asString(),
                Integer.toString(command.money().amount()),
                command.money().currency()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
