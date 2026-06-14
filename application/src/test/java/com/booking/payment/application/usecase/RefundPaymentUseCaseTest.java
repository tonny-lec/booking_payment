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
@DisplayName("RefundPaymentUseCase")
class RefundPaymentUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Test
    @DisplayName("execute should void authorized payment through gateway and save it")
    void executeShouldVoidAuthorizedPaymentThroughGatewayAndSaveIt() {
        Payment payment = authorizedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        when(paymentGateway.voidAuthorization(any()))
                .thenReturn(PaymentGatewayPort.VoidResult.voided());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyKey idempotencyKey = idempotencyKey();
        Payment result = useCase().execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey,
                null,
                "customer_request",
                "changed plans"
        ));

        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.refundedAmount()).isEqualTo(10000);
        verify(paymentGateway).voidAuthorization(new PaymentGatewayPort.VoidRequest(
                payment.id(),
                payment.gatewayTransactionId(),
                "customer_request",
                "changed plans",
                idempotencyKey.value()
        ));
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("execute should refund captured payment through gateway and save it")
    void executeShouldRefundCapturedPaymentThroughGatewayAndSaveIt() {
        Payment payment = capturedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        when(paymentGateway.refund(any()))
                .thenReturn(PaymentGatewayPort.RefundResult.refunded());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyKey idempotencyKey = idempotencyKey();
        Payment result = useCase().execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey,
                3000,
                "customer_request",
                "partial refund"
        ));

        assertThat(result.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(result.refundedAmount()).isEqualTo(3000);
        verify(paymentGateway).refund(new PaymentGatewayPort.RefundRequest(
                payment.id(),
                payment.gatewayTransactionId(),
                3000,
                "customer_request",
                "partial refund",
                idempotencyKey.value()
        ));
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("execute should reject when payment does not exist")
    void executeShouldRejectWhenPaymentDoesNotExist() {
        Payment payment = authorizedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey(),
                null,
                "customer_request",
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

        assertThatThrownBy(() -> useCase().execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                UserId.generate(),
                idempotencyKey(),
                null,
                "customer_request",
                null
        ))).isInstanceOfSatisfying(ForbiddenException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_access_denied"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("execute should reject non-refundable payment with domain exception")
    void executeShouldRejectNonRefundablePaymentWithDomainException() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey(),
                null,
                "customer_request",
                null
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_not_refundable"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("execute should reject invalid refund amount with domain exception")
    void executeShouldRejectInvalidRefundAmountWithDomainException() {
        Payment payment = capturedPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                payment.userId(),
                idempotencyKey(),
                11000,
                "customer_request",
                "too much"
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_refund_exceeds_captured"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private RefundPaymentUseCase useCase() {
        return new RefundPaymentUseCase(paymentRepository, paymentGateway);
    }

    private static Payment capturedPayment() {
        Payment payment = authorizedPayment();
        payment.capture(10000);
        return payment;
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
