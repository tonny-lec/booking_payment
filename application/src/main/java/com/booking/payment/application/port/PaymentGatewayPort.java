package com.booking.payment.application.port;

import com.booking.booking.domain.model.BookingId;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.PaymentId;

import java.util.Objects;

/**
 * Anti-Corruption Layer port for the external payment gateway
 * (Stripe, PayPay, etc.).
 *
 * <p>The application layer only depends on this interface; concrete adapters
 * translate gateway-specific protocols, timeouts and error formats into the
 * neutral {@link AuthorizationResult} model defined here
 * (see {@code docs/design/usecases/payment-create.md} section 3).
 *
 * <p>Gateway communication failures (I/O errors, timeouts) are adapter
 * concerns: a production adapter is expected to retry/translate them.
 * Mapping such failures to HTTP 502/504 is intentionally out of scope of
 * Slice A and tracked in {@code docs/tasks/implementation-slice-a.md}.
 */
public interface PaymentGatewayPort {

    /**
     * Requests authorization (credit hold) for a new payment.
     *
     * @param request authorization request
     * @return authorization result (authorized or declined)
     */
    AuthorizationResult authorize(AuthorizationRequest request);

    /**
     * Authorization request passed to the gateway.
     *
     * @param paymentId payment identifier (used as gateway reference)
     * @param bookingId booking the payment belongs to
     * @param money amount and currency to authorize
     */
    record AuthorizationRequest(PaymentId paymentId, BookingId bookingId, Money money) {
        public AuthorizationRequest {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(bookingId, "bookingId must not be null");
            Objects.requireNonNull(money, "money must not be null");
        }
    }

    /**
     * Neutral authorization result.
     *
     * @param authorized true when the gateway authorized the amount
     * @param gatewayTransactionId gateway transaction id (present when authorized)
     * @param failureReason decline reason (present when not authorized)
     */
    record AuthorizationResult(boolean authorized, String gatewayTransactionId, String failureReason) {

        /**
         * Creates a successful authorization result.
         *
         * @param gatewayTransactionId gateway transaction id
         * @return authorized result
         */
        public static AuthorizationResult authorized(String gatewayTransactionId) {
            Objects.requireNonNull(gatewayTransactionId, "gatewayTransactionId must not be null");
            return new AuthorizationResult(true, gatewayTransactionId, null);
        }

        /**
         * Creates a declined authorization result.
         *
         * @param failureReason decline reason (e.g. "card_declined")
         * @return declined result
         */
        public static AuthorizationResult declined(String failureReason) {
            Objects.requireNonNull(failureReason, "failureReason must not be null");
            return new AuthorizationResult(false, null, failureReason);
        }
    }
}
