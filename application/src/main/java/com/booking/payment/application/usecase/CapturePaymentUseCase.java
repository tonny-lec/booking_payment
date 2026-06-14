package com.booking.payment.application.usecase;

import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for capturing an authorized payment.
 */
public class CapturePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGateway;

    public CapturePaymentUseCase(PaymentRepository paymentRepository, PaymentGatewayPort paymentGateway) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
    }

    /**
     * Captures a payment owned by the requester.
     *
     * @param command capture command
     * @return captured payment
     */
    public Payment execute(CapturePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment",
                        command.paymentId().asString(),
                        "Payment not found"
                ));
        if (!payment.isOwnedBy(command.requestUserId())) {
            throw new ForbiddenException(
                    "payment_access_denied",
                    "Only payment owner can capture payment"
            );
        }
        if (payment.status() == PaymentStatus.CAPTURED) {
            return payment;
        }
        int captureAmount = command.amount() != null ? command.amount() : payment.money().amount();
        validateCaptureRequest(payment, captureAmount);
        PaymentGatewayPort.CaptureResult result = paymentGateway.capture(new PaymentGatewayPort.CaptureRequest(
                payment.id(),
                requireGatewayTransactionId(payment),
                captureAmount,
                command.idempotencyKey().value()
        ));
        if (!result.successful()) {
            throw new BusinessRuleViolationException(
                    "payment_capture_failed",
                    result.failureReason() != null ? result.failureReason() : "Payment capture failed"
            );
        }

        payment.capture(command.amount());
        return paymentRepository.save(payment);
    }

    private void validateCaptureRequest(Payment payment, int captureAmount) {
        if (!payment.status().isCapturable()) {
            throw new BusinessRuleViolationException(
                    "payment_not_capturable",
                    "Payment cannot be captured from status: " + payment.status()
            );
        }
        if (captureAmount <= 0) {
            throw new BusinessRuleViolationException("payment_invalid_capture_amount", "Capture amount must be positive");
        }
        if (captureAmount > payment.money().amount()) {
            throw new BusinessRuleViolationException(
                    "payment_capture_exceeds_authorized",
                    "Capture amount cannot exceed authorized amount"
            );
        }
    }

    private String requireGatewayTransactionId(Payment payment) {
        if (payment.gatewayTransactionId() == null || payment.gatewayTransactionId().isBlank()) {
            throw new BusinessRuleViolationException(
                    "payment_gateway_transaction_missing",
                    "Payment has no gateway transaction id"
            );
        }
        return payment.gatewayTransactionId();
    }

    public record CapturePaymentCommand(
            PaymentId paymentId,
            UserId requestUserId,
            IdempotencyKey idempotencyKey,
            Integer amount
    ) {
        public CapturePaymentCommand {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        }
    }
}
