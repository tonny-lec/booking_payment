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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePaymentUseCase")
class CreatePaymentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-02-11T11:00:00Z");

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
    @DisplayName("should create authorized payment when gateway authorization succeeds")
    void shouldCreateAuthorizedPaymentWhenGatewayAuthorizationSucceeds() {
        CreatePaymentUseCase.Command command = createCommand();
        when(idempotencyStore.findByKey(command.idempotencyKey())).thenReturn(Optional.empty());
        when(paymentGatewayPort.authorize(any(Payment.class)))
                .thenReturn(PaymentGatewayPort.AuthorizationResult.success("stub-tx-001"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idempotencyStore.save(any(IdempotencyStore.IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentUseCase.Result result = useCase.execute(command);

        assertThat(result.idempotencyHit()).isFalse();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.payment().gatewayTransactionId()).isEqualTo("stub-tx-001");
        verify(paymentRepository).save(any(Payment.class));
        verify(idempotencyStore).save(any(IdempotencyStore.IdempotencyRecord.class));
    }

    @Test
    @DisplayName("should create failed payment when gateway authorization fails")
    void shouldCreateFailedPaymentWhenGatewayAuthorizationFails() {
        CreatePaymentUseCase.Command command = createCommand();
        when(idempotencyStore.findByKey(command.idempotencyKey())).thenReturn(Optional.empty());
        when(paymentGatewayPort.authorize(any(Payment.class)))
                .thenReturn(PaymentGatewayPort.AuthorizationResult.failure("stub_gateway_rejected"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idempotencyStore.save(any(IdempotencyStore.IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentUseCase.Result result = useCase.execute(command);

        assertThat(result.idempotencyHit()).isFalse();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.payment().failureReason()).isEqualTo("stub_gateway_rejected");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().status()).isEqualTo(PaymentStatus.FAILED);
    }

    private CreatePaymentUseCase.Command createCommand() {
        return new CreatePaymentUseCase.Command(
                BookingId.generate(),
                UserId.generate(),
                Money.of(4500, "JPY"),
                IdempotencyKey.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440001")),
                "test payment"
        );
    }
}
