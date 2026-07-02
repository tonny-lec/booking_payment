package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.application.usecase.RefundPaymentUseCase;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.BusinessRuleViolationException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelBookingUseCase")
class CancelBookingUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundPaymentUseCase refundPaymentUseCase;

    @Test
    @DisplayName("execute should full-refund captured payment and cancel booking")
    void executeShouldFullRefundCapturedPaymentAndCancelBooking() {
        Booking booking = confirmedBooking();
        Payment payment = capturedPayment(booking);
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.findLatestByBookingId(booking.id())).thenReturn(Optional.of(payment));
        when(refundPaymentUseCase.execute(any())).thenReturn(payment);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.FULL_REFUND,
                null,
                idempotencyKey()
        ));

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(refundPaymentUseCase).execute(argThat(command ->
                command.paymentId().equals(payment.id())
                        && command.requestUserId().equals(booking.userId())
                        && command.amount() == null
                        && command.reason().equals("BOOKING_CANCELLED")
        ));
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("execute should partial-refund captured payment amount and cancel booking")
    void executeShouldPartialRefundCapturedPaymentAmountAndCancelBooking() {
        Booking booking = confirmedBooking();
        Payment payment = capturedPayment(booking);
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.findLatestByBookingId(booking.id())).thenReturn(Optional.of(payment));
        when(refundPaymentUseCase.execute(any())).thenReturn(payment);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.PARTIAL_REFUND,
                3000,
                idempotencyKey()
        ));

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(refundPaymentUseCase).execute(argThat(command ->
                command.paymentId().equals(payment.id())
                        && command.amount().equals(3000)
                        && command.reason().equals("BOOKING_CANCELLED")
        ));
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("execute should cancel booking without touching payment for no-refund policy")
    void executeShouldCancelBookingWithoutTouchingPaymentForNoRefundPolicy() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.NO_REFUND,
                null,
                null
        ));

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        verifyNoInteractions(paymentRepository, refundPaymentUseCase);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("execute should reject refund policy when booking has no payment")
    void executeShouldRejectRefundPolicyWhenBookingHasNoPayment() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.findLatestByBookingId(booking.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.FULL_REFUND,
                null,
                idempotencyKey()
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_required_for_refund_policy"));

        verifyNoInteractions(refundPaymentUseCase);
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    @DisplayName("execute should reject partial refund for authorized payment")
    void executeShouldRejectPartialRefundForAuthorizedPayment() {
        Booking booking = confirmedBooking();
        Payment payment = authorizedPayment(booking);
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.findLatestByBookingId(booking.id())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.PARTIAL_REFUND,
                3000,
                idempotencyKey()
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_not_captured_for_partial_refund"));

        verifyNoInteractions(refundPaymentUseCase);
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    @DisplayName("execute should reject partial refund without amount")
    void executeShouldRejectPartialRefundWithoutAmount() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.PARTIAL_REFUND,
                null,
                idempotencyKey()
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("payment_invalid_refund_amount"));

        verifyNoInteractions(paymentRepository, refundPaymentUseCase);
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    @DisplayName("execute should reject already cancelled booking before payment side effects")
    void executeShouldRejectAlreadyCancelledBookingBeforePaymentSideEffects() {
        Booking booking = confirmedBooking();
        booking.cancel("already cancelled");
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> useCase().execute(new CancelBookingUseCase.CancelBookingCommand(
                booking.id(),
                booking.userId(),
                "changed plans",
                RefundPolicy.PARTIAL_REFUND,
                3000,
                idempotencyKey()
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo("booking_already_cancelled"));

        verifyNoInteractions(paymentRepository, refundPaymentUseCase);
        verify(bookingRepository, never()).save(booking);
    }

    private CancelBookingUseCase useCase() {
        return new CancelBookingUseCase(bookingRepository, paymentRepository, refundPaymentUseCase);
    }

    private static Booking confirmedBooking() {
        Instant now = Instant.parse("2026-03-01T00:00:00Z");
        return Booking.builder()
                .id(BookingId.generate())
                .userId(UserId.generate())
                .resourceId(ResourceId.generate())
                .timeRange(TimeRange.fromPersisted(
                        Instant.parse("2026-03-10T10:00:00Z"),
                        Instant.parse("2026-03-10T11:00:00Z")
                ))
                .status(BookingStatus.CONFIRMED)
                .note("note")
                .version(Booking.INITIAL_VERSION)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static Payment capturedPayment(Booking booking) {
        Payment payment = authorizedPayment(booking);
        payment.capture(10000);
        return payment;
    }

    private static Payment authorizedPayment(Booking booking) {
        Payment payment = Payment.create(
                booking.id(),
                booking.userId(),
                Money.of(10000, "JPY"),
                idempotencyKey(),
                "meeting room",
                FIXED_CLOCK
        );
        payment.authorize("txn_existing");
        return payment;
    }

    private static IdempotencyKey idempotencyKey() {
        return IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK);
    }
}
