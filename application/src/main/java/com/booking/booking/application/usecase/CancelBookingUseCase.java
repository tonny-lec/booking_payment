package com.booking.booking.application.usecase;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.application.usecase.RefundPaymentUseCase;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for canceling bookings.
 */
public class CancelBookingUseCase {

    private static final String BOOKING_CANCELLED_REFUND_REASON = "BOOKING_CANCELLED";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundPaymentUseCase refundPaymentUseCase;

    public CancelBookingUseCase(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RefundPaymentUseCase refundPaymentUseCase
    ) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.refundPaymentUseCase = Objects.requireNonNull(refundPaymentUseCase, "refundPaymentUseCase must not be null");
    }

    /**
     * Cancels an existing booking.
     *
     * @param command cancel command
     * @return cancelled booking
     */
    public Booking execute(CancelBookingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking",
                        command.bookingId().asString(),
                        "Booking not found"
                ));

        if (!booking.isOwnedBy(command.requestUserId())) {
            throw new ForbiddenException(
                    "booking_access_denied",
                    "Only booking owner can cancel booking"
            );
        }
        assertBookingCancellable(booking);

        if (command.refundPolicy() == RefundPolicy.PARTIAL_REFUND) {
            validatePartialRefundAmount(command.partialRefundAmount());
        } else if (command.partialRefundAmount() != null) {
            throw new BusinessRuleViolationException(
                    "payment_invalid_refund_amount",
                    "partialRefundAmount is only allowed for PARTIAL_REFUND"
            );
        }

        if (command.refundPolicy() != RefundPolicy.NO_REFUND) {
            refundPaymentForCancellation(booking, command);
        }

        booking.cancel(command.reason());
        return bookingRepository.save(booking);
    }

    private void assertBookingCancellable(Booking booking) {
        if (!booking.status().isCancellable()) {
            String code = booking.status() == BookingStatus.CANCELLED
                    ? "booking_already_cancelled"
                    : "booking_not_cancellable";
            throw new BusinessRuleViolationException(
                    code,
                    "Booking cannot be cancelled from status: " + booking.status()
            );
        }
    }

    private void refundPaymentForCancellation(Booking booking, CancelBookingCommand command) {
        if (command.idempotencyKey() == null) {
            throw new BusinessRuleViolationException(
                    "payment_idempotency_key_required",
                    "Idempotency-Key is required for cancellation refund"
            );
        }

        Payment payment = paymentRepository.findLatestByBookingId(booking.id())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "payment_required_for_refund_policy",
                        "A payment is required for the selected refund policy"
                ));

        if (command.refundPolicy() == RefundPolicy.PARTIAL_REFUND) {
            validatePartialRefundPayment(payment);
        }

        refundPaymentUseCase.execute(new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                command.requestUserId(),
                command.idempotencyKey(),
                command.refundPolicy() == RefundPolicy.PARTIAL_REFUND ? command.partialRefundAmount() : null,
                BOOKING_CANCELLED_REFUND_REASON,
                command.reason()
        ));
    }

    private void validatePartialRefundPayment(Payment payment) {
        if (payment.status() == PaymentStatus.AUTHORIZED) {
            throw new BusinessRuleViolationException(
                    "payment_not_captured_for_partial_refund",
                    "Partial refund requires a captured payment"
            );
        }
        if (payment.status() == PaymentStatus.REFUNDED) {
            throw new BusinessRuleViolationException(
                    "payment_already_refunded",
                    "Payment is already fully refunded"
            );
        }
        if (payment.status() != PaymentStatus.CAPTURED) {
            throw new BusinessRuleViolationException(
                    "payment_not_refundable",
                    "Payment cannot be refunded from status: " + payment.status()
            );
        }
    }

    private void validatePartialRefundAmount(Integer partialRefundAmount) {
        if (partialRefundAmount == null || partialRefundAmount <= 0) {
            throw new BusinessRuleViolationException(
                    "payment_invalid_refund_amount",
                    "partialRefundAmount must be positive for PARTIAL_REFUND"
            );
        }
    }

    public record CancelBookingCommand(
            BookingId bookingId,
            UserId requestUserId,
            String reason,
            RefundPolicy refundPolicy,
            Integer partialRefundAmount,
            IdempotencyKey idempotencyKey
    ) {
        public CancelBookingCommand {
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
            Objects.requireNonNull(refundPolicy, "refundPolicy must not be null");
        }
    }
}
