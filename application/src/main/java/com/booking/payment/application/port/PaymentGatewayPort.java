package com.booking.payment.application.port;

import com.booking.booking.domain.model.BookingId;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.PaymentId;

import java.util.Objects;
import java.util.UUID;

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
     * Captures a previously authorized payment.
     *
     * @param request capture request
     * @return capture result
     */
    CaptureResult capture(CaptureRequest request);

    /**
     * Refunds a captured payment.
     *
     * @param request refund request
     * @return refund result
     */
    RefundResult refund(RefundRequest request);

    /**
     * Voids a previously authorized payment without capture.
     *
     * @param request void request
     * @return void result
     */
    VoidResult voidAuthorization(VoidRequest request);

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
     * Capture request passed to the gateway.
     *
     * @param paymentId payment identifier
     * @param gatewayTransactionId gateway authorization transaction id
     * @param amount capture amount
     * @param idempotencyKey client idempotency key for this operation
     */
    record CaptureRequest(PaymentId paymentId, String gatewayTransactionId, int amount, UUID idempotencyKey) {
        public CaptureRequest {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(gatewayTransactionId, "gatewayTransactionId must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }

    /**
     * Refund request passed to the gateway.
     *
     * @param paymentId payment identifier
     * @param gatewayTransactionId gateway capture/authorization transaction id
     * @param amount refund amount
     * @param reason refund reason
     * @param note optional refund note
     * @param idempotencyKey client idempotency key for this operation
     */
    record RefundRequest(
            PaymentId paymentId,
            String gatewayTransactionId,
            int amount,
            String reason,
            String note,
            UUID idempotencyKey
    ) {
        public RefundRequest {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(gatewayTransactionId, "gatewayTransactionId must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }

    /**
     * Void request passed to the gateway.
     *
     * @param paymentId payment identifier
     * @param gatewayTransactionId gateway authorization transaction id
     * @param reason void/refund reason
     * @param note optional note
     * @param idempotencyKey client idempotency key for this operation
     */
    record VoidRequest(PaymentId paymentId, String gatewayTransactionId, String reason, String note, UUID idempotencyKey) {
        public VoidRequest {
            Objects.requireNonNull(paymentId, "paymentId must not be null");
            Objects.requireNonNull(gatewayTransactionId, "gatewayTransactionId must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
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

    record CaptureResult(boolean successful, String failureReason) {

        public static CaptureResult captured() {
            return new CaptureResult(true, null);
        }

        public static CaptureResult failed(String failureReason) {
            Objects.requireNonNull(failureReason, "failureReason must not be null");
            return new CaptureResult(false, failureReason);
        }
    }

    record RefundResult(boolean successful, String failureReason) {

        public static RefundResult refunded() {
            return new RefundResult(true, null);
        }

        public static RefundResult failed(String failureReason) {
            Objects.requireNonNull(failureReason, "failureReason must not be null");
            return new RefundResult(false, failureReason);
        }
    }

    record VoidResult(boolean successful, String failureReason) {

        public static VoidResult voided() {
            return new VoidResult(true, null);
        }

        public static VoidResult failed(String failureReason) {
            Objects.requireNonNull(failureReason, "failureReason must not be null");
            return new VoidResult(false, failureReason);
        }
    }
}
