package com.booking.payment.application.usecase;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CapturePaymentUseCase")
class CapturePaymentUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Test
    @DisplayName("execute should capture authorized payment through gateway and save it")
    void executeShouldCaptureAuthorizedPaymentThroughGatewayAndSaveIt() {
        Payment payment = authorizedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        when(paymentGateway.capture(any()))
                .thenReturn(PaymentGatewayPort.CaptureResult.captured());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyKey idempotencyKey = idempotencyKey();
        Payment result = useCase().execute(new CapturePaymentUseCase.CapturePaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey,
                7000
        ));

        assertThat(result.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(result.capturedAmount()).isEqualTo(7000);
        verify(paymentGateway).capture(new PaymentGatewayPort.CaptureRequest(
                payment.id(),
                payment.gatewayTransactionId(),
                7000,
                idempotencyKey.value()
        ));
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("execute should reject when payment does not exist")
    void executeShouldRejectWhenPaymentDoesNotExist() {
        Payment payment = authorizedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(new CapturePaymentUseCase.CapturePaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey(),
                null
        ))).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("execute should reject when requester is not owner")
    void executeShouldRejectWhenRequesterIsNotOwner() {
        Payment payment = authorizedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(new CapturePaymentUseCase.CapturePaymentCommand(
                payment.id(),
                UserId.generate(),
                idempotencyKey(),
                null
        ))).isInstanceOfSatisfying(ForbiddenException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_access_denied"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("execute should reject non-capturable payment with domain exception")
    void executeShouldRejectNonCapturablePaymentWithDomainException() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(new CapturePaymentUseCase.CapturePaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey(),
                null
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_not_capturable"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("execute should reject invalid capture amount with domain exception")
    void executeShouldRejectInvalidCaptureAmountWithDomainException() {
        Payment payment = authorizedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(new CapturePaymentUseCase.CapturePaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey(),
                11000
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_capture_exceeds_authorized"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private CapturePaymentUseCase useCase() {
        return new CapturePaymentUseCase(paymentRepository, paymentGateway);
    }

    private static Payment authorizedPayment() {
        Payment payment = pendingPayment();
        payment.authorize("txn_existing");
        return payment;
    }

    private static Payment pendingPayment() {
        return Payment.create(
                BookingId.generate(),
                UserId.generate(),
                Money.of(10000, "JPY"),
                IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK),
                "meeting room",
                FIXED_CLOCK
        );
    }

    private static IdempotencyKey idempotencyKey() {
        return IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK);
    }
}
