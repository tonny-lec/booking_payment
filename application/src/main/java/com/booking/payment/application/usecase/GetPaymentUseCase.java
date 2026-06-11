package com.booking.payment.application.usecase;

import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for retrieving a payment.
 */
public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
    }

    /**
     * Retrieves a payment when the requester is owner.
     *
     * @param query get query
     * @return payment aggregate
     */
    public Payment execute(GetPaymentQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Payment payment = paymentRepository.findById(query.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment",
                        query.paymentId().asString(),
                        "Payment not found"
                ));

        if (!payment.isOwnedBy(query.requestUserId())) {
            throw new ForbiddenException(
                    "payment_access_denied",
                    "Only payment owner can access payment"
            );
        }
        return payment;
    }

    public record GetPaymentQuery(PaymentId paymentId, UserId requestUserId) {
        public GetPaymentQuery {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(requestUserId, "requestUserId must not be null");
        }
    }
}
