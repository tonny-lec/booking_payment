package com.booking.payment.application.port;

import com.booking.payment.domain.model.Payment;

import java.util.Objects;

/**
 * Anti-corruption port for external payment gateway.
 */
public interface PaymentGatewayPort {

    /**
     * Performs payment authorization against external gateway.
     *
     * @param payment payment aggregate
     * @return authorization result
     */
    AuthorizationResult authorize(Payment payment);

    /**
     * Result model for payment authorization.
     */
    record AuthorizationResult(
            boolean success,
            String gatewayTransactionId,
            String failureReason
    ) {
        public AuthorizationResult {
            if (success) {
                Objects.requireNonNull(gatewayTransactionId, "gatewayTransactionId must not be null when success");
            } else {
                Objects.requireNonNull(failureReason, "failureReason must not be null when failure");
            }
        }

        public static AuthorizationResult success(String gatewayTransactionId) {
            return new AuthorizationResult(true, gatewayTransactionId, null);
        }

        public static AuthorizationResult failure(String failureReason) {
            return new AuthorizationResult(false, null, failureReason);
        }
    }
}
