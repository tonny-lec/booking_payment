package com.booking.payment.application.usecase;

import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Use case for fetching a payment with ownership check.
 */
public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
    }

    /**
     * Loads payment detail for authenticated user.
     *
     * @param query lookup query
     * @return payment aggregate
     */
    public Payment execute(Query query) {
        Objects.requireNonNull(query, "query must not be null");

        Payment payment = paymentRepository.findById(query.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment",
                        query.paymentId().asString(),
                        "Payment not found: " + query.paymentId().asString()
                ));

        if (!payment.isOwnedBy(query.requesterId())) {
            throw new ForbiddenException("payment_forbidden", "Access to payment is forbidden");
        }

        return payment;
    }

    public record Query(PaymentId paymentId, UserId requesterId) {
        public Query {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(requesterId, "requesterId must not be null");
        }
    }
}
