package com.booking.payment.application.usecase;

import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for refunding or voiding a payment.
 */
public class RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGateway;

    public RefundPaymentUseCase(PaymentRepository paymentRepository, PaymentGatewayPort paymentGateway) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
    }

    /**
     * Refunds a captured payment, or voids an authorized payment.
     *
     * @param command refund command
     * @return refunded payment
     */
    public Payment execute(RefundPaymentCommand command) {
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
                    "Only payment owner can refund payment"
            );
        }
        if (payment.isSameRefundRequest(command.idempotencyKey(), command.amount())) {
            return payment;
        }
        if (payment.hasRefundRequestWith(command.idempotencyKey())) {
            throw new ConflictException(
                    "payment_refund_idempotency_key_conflict",
                    "Idempotency-Key was already used with a different refund request content"
            );
        }
        if (payment.status() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.status() == PaymentStatus.AUTHORIZED) {
            voidAuthorization(payment, command);
        } else {
            refundCapturedPayment(payment, command);
        }
        return paymentRepository.save(payment);
    }

    private void voidAuthorization(Payment payment, RefundPaymentCommand command) {
        PaymentGatewayPort.VoidResult result = paymentGateway.voidAuthorization(new PaymentGatewayPort.VoidRequest(
                payment.id(),
                requireGatewayTransactionId(payment),
                command.reason(),
                command.note(),
                command.idempotencyKey().value()
        ));
        if (!result.successful()) {
            throw new BusinessRuleViolationException(
                    "payment_void_failed",
                    result.failureReason() != null ? result.failureReason() : "Payment void failed"
            );
        }
        payment.voidAuthorization(command.idempotencyKey());
    }

    private void refundCapturedPayment(Payment payment, RefundPaymentCommand command) {
        int refundAmount = command.amount() != null
                ? command.amount()
                : remainingRefundableAmount(payment);
        validateRefundRequest(payment, refundAmount);
        PaymentGatewayPort.RefundResult result = paymentGateway.refund(new PaymentGatewayPort.RefundRequest(
                payment.id(),
                requireGatewayTransactionId(payment),
                refundAmount,
                command.reason(),
                command.note(),
                command.idempotencyKey().value()
        ));
        if (!result.successful()) {
            throw new BusinessRuleViolationException(
                    "payment_refund_failed",
                    result.failureReason() != null ? result.failureReason() : "Payment refund failed"
            );
        }
        payment.refund(command.amount(), command.idempotencyKey());
    }

    private int remainingRefundableAmount(Payment payment) {
        int maxRefundable = payment.capturedAmount() != null ? payment.capturedAmount() : payment.money().amount();
        int alreadyRefunded = payment.refundedAmount() != null ? payment.refundedAmount() : 0;
        return maxRefundable - alreadyRefunded;
    }

    private void validateRefundRequest(Payment payment, int refundAmount) {
        if (payment.status() != PaymentStatus.CAPTURED) {
            throw new BusinessRuleViolationException(
                    "payment_not_refundable",
                    "Payment cannot be refunded from status: " + payment.status()
            );
        }
        int remainingRefundable = remainingRefundableAmount(payment);
        if (refundAmount <= 0) {
            throw new BusinessRuleViolationException("payment_invalid_refund_amount", "Refund amount must be positive");
        }
        if (refundAmount > remainingRefundable) {
            throw new BusinessRuleViolationException(
                    "payment_refund_exceeds_captured",
                    "Refund amount cannot exceed captured amount"
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

    public record RefundPaymentCommand(
            PaymentId paymentId,
            UserId requestUserId,
            IdempotencyKey idempotencyKey,
            Integer amount,
            String reason,
            String note
    ) {
        public RefundPaymentCommand {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
