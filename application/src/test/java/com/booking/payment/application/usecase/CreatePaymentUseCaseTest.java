package com.booking.payment.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;
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
@DisplayName("CreatePaymentUseCase")
class CreatePaymentUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Test
    @DisplayName("execute should authorize via gateway and save payment as AUTHORIZED")
    void executeShouldAuthorizeViaGatewayAndSavePaymentAsAuthorized() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.existsActiveByBookingId(booking.id())).thenReturn(false);
        when(paymentGateway.authorize(any()))
                .thenReturn(PaymentGatewayPort.AuthorizationResult.authorized("txn_123"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentUseCase.CreatePaymentResult result = useCase().execute(command);

        assertThat(result.idempotentReplay()).isFalse();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.payment().gatewayTransactionId()).isEqualTo("txn_123");
        assertThat(result.payment().bookingId()).isEqualTo(booking.id());
        assertThat(result.payment().userId()).isEqualTo(booking.userId());
        assertThat(result.payment().money()).isEqualTo(command.money());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("execute should save payment as FAILED when gateway declines")
    void executeShouldSavePaymentAsFailedWhenGatewayDeclines() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.existsActiveByBookingId(booking.id())).thenReturn(false);
        when(paymentGateway.authorize(any()))
                .thenReturn(PaymentGatewayPort.AuthorizationResult.declined("card_declined"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentUseCase.CreatePaymentResult result = useCase().execute(command);

        assertThat(result.idempotentReplay()).isFalse();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.payment().failureReason()).isEqualTo("card_declined");
    }

    @Test
    @DisplayName("execute should replay existing payment for same key and same content")
    void executeShouldReplayExistingPaymentForSameKeyAndSameContent() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        Payment existing = authorizedPayment(command);
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.of(existing));

        CreatePaymentUseCase.CreatePaymentResult result = useCase().execute(command);

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(result.payment()).isSameAs(existing);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("execute should reject same key with different content")
    void executeShouldRejectSameKeyWithDifferentContent() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        Payment existing = authorizedPayment(new CreatePaymentUseCase.CreatePaymentCommand(
                command.idempotencyKey(),
                command.userId(),
                command.bookingId(),
                Money.of(99999, "JPY"),
                "different amount"
        ));
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(ConflictException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_idempotency_key_conflict"));

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("execute should reject same key reused by another user")
    void executeShouldRejectSameKeyReusedByAnotherUser() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        Payment existing = authorizedPayment(new CreatePaymentUseCase.CreatePaymentCommand(
                command.idempotencyKey(),
                UserId.generate(),
                command.bookingId(),
                command.money(),
                command.description()
        ));
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(ConflictException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_idempotency_key_conflict"));
    }

    @Test
    @DisplayName("execute should reject when booking does not exist")
    void executeShouldRejectWhenBookingDoesNotExist() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("execute should reject when requester is not booking owner")
    void executeShouldRejectWhenRequesterIsNotBookingOwner() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = new CreatePaymentUseCase.CreatePaymentCommand(
                IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK),
                UserId.generate(),
                booking.id(),
                Money.of(10000, "JPY"),
                "meeting room"
        );
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(ForbiddenException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_booking_access_denied"));

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("execute should reject when booking is cancelled")
    void executeShouldRejectWhenBookingIsCancelled() {
        Booking booking = cancelledBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_booking_not_payable"));

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("execute should reject when booking already has active payment")
    void executeShouldRejectWhenBookingAlreadyHasActivePayment() {
        Booking booking = pendingBooking();
        CreatePaymentUseCase.CreatePaymentCommand command = commandFor(booking);
        when(paymentRepository.findByIdempotencyKey(command.idempotencyKey().value()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));
        when(paymentRepository.existsActiveByBookingId(booking.id())).thenReturn(true);

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_booking_already_paid"));

        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private CreatePaymentUseCase useCase() {
        return new CreatePaymentUseCase(paymentRepository, bookingRepository, paymentGateway, FIXED_CLOCK);
    }

    private static CreatePaymentUseCase.CreatePaymentCommand commandFor(Booking booking) {
        return new CreatePaymentUseCase.CreatePaymentCommand(
                IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK),
                booking.userId(),
                booking.id(),
                Money.of(10000, "JPY"),
                "meeting room"
        );
    }

    private static Payment authorizedPayment(CreatePaymentUseCase.CreatePaymentCommand command) {
        Payment payment = Payment.create(
                command.bookingId(),
                command.userId(),
                command.money(),
                command.idempotencyKey(),
                command.description(),
                FIXED_CLOCK
        );
        payment.authorize("txn_existing");
        return payment;
    }

    private static Booking pendingBooking() {
        Instant now = Instant.parse("2026-03-01T00:00:00Z");
        return Booking.builder()
                .id(BookingId.generate())
                .userId(UserId.generate())
                .resourceId(ResourceId.generate())
                .timeRange(TimeRange.fromPersisted(
                        Instant.parse("2026-03-10T10:00:00Z"),
                        Instant.parse("2026-03-10T11:00:00Z")
                ))
                .status(BookingStatus.PENDING)
                .note("note")
                .version(Booking.INITIAL_VERSION)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static Booking cancelledBooking() {
        Instant now = Instant.parse("2026-03-01T00:00:00Z");
        Instant cancelledAt = Instant.parse("2026-03-02T00:00:00Z");
        return Booking.builder()
                .id(BookingId.generate())
                .userId(UserId.generate())
                .resourceId(ResourceId.generate())
                .timeRange(TimeRange.fromPersisted(
                        Instant.parse("2026-03-10T10:00:00Z"),
                        Instant.parse("2026-03-10T11:00:00Z")
                ))
                .status(BookingStatus.CANCELLED)
                .note("note")
                .version(Booking.INITIAL_VERSION + 1)
                .cancelledAt(cancelledAt)
                .cancelReason("cancelled")
                .createdAt(now)
                .updatedAt(cancelledAt)
                .build();
    }
}
