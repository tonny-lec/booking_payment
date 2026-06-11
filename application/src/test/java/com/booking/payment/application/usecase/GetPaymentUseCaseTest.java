package com.booking.payment.application.usecase;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPaymentUseCase")
class GetPaymentUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("execute should return payment for owner")
    void executeShouldReturnPaymentForOwner() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        GetPaymentUseCase useCase = new GetPaymentUseCase(paymentRepository);

        Payment result = useCase.execute(new GetPaymentUseCase.GetPaymentQuery(payment.id(), payment.userId()));

        assertThat(result).isSameAs(payment);
    }

    @Test
    @DisplayName("execute should reject when payment does not exist")
    void executeShouldRejectWhenPaymentDoesNotExist() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.empty());
        GetPaymentUseCase useCase = new GetPaymentUseCase(paymentRepository);

        assertThatThrownBy(() -> useCase.execute(
                new GetPaymentUseCase.GetPaymentQuery(payment.id(), payment.userId())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("execute should reject when requester is not owner")
    void executeShouldRejectWhenRequesterIsNotOwner() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.id())).thenReturn(Optional.of(payment));
        GetPaymentUseCase useCase = new GetPaymentUseCase(paymentRepository);

        assertThatThrownBy(() -> useCase.execute(
                new GetPaymentUseCase.GetPaymentQuery(payment.id(), UserId.generate())))
                .isInstanceOfSatisfying(ForbiddenException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_access_denied"));
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
}
