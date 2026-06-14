package com.booking.payment.adapter.gateway;

import com.booking.payment.application.port.PaymentGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of {@link PaymentGatewayPort} (PAY-I-05).
 *
 * <p>Always authorizes and returns a generated transaction id, allowing the
 * full payment flow (HTTP -> use case -> domain -> persistence) to run
 * without an external provider. Until a real adapter (Stripe/PayPay, with
 * timeout, retry and 502/504 error translation per
 * {@code docs/design/usecases/payment-create.md} section 8) replaces it,
 * this bean lives in the bootstrap composition root because the choice of
 * gateway is an environment concern, not a web/persistence concern.
 */
@Component
public class StubPaymentGateway implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StubPaymentGateway.class);

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        String transactionId = "txn_stub_" + UUID.randomUUID();
        log.info("Stub gateway authorized payment: paymentId={}, amount={} {}, transactionId={}",
                request.paymentId().asString(),
                request.money().amount(),
                request.money().currency(),
                transactionId);
        return AuthorizationResult.authorized(transactionId);
    }

    @Override
    public CaptureResult capture(CaptureRequest request) {
        log.info("Stub gateway captured payment: paymentId={}, amount={}, transactionId={}",
                request.paymentId().asString(),
                request.amount(),
                request.gatewayTransactionId());
        return CaptureResult.captured();
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        log.info("Stub gateway refunded payment: paymentId={}, amount={}, reason={}, transactionId={}",
                request.paymentId().asString(),
                request.amount(),
                request.reason(),
                request.gatewayTransactionId());
        return RefundResult.refunded();
    }

    @Override
    public VoidResult voidAuthorization(VoidRequest request) {
        log.info("Stub gateway voided payment: paymentId={}, reason={}, transactionId={}",
                request.paymentId().asString(),
                request.reason(),
                request.gatewayTransactionId());
        return VoidResult.voided();
    }
}
