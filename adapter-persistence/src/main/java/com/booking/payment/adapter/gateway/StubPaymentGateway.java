package com.booking.payment.adapter.gateway;

import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Stub implementation for external payment gateway authorization.
 */
@Component
public class StubPaymentGateway implements PaymentGatewayPort {

    @Override
    public AuthorizationResult authorize(Payment payment) {
        if (shouldFail(payment)) {
            return AuthorizationResult.failure("stub_gateway_rejected");
        }
        return AuthorizationResult.success("stub-tx-" + UUID.randomUUID());
    }

    private boolean shouldFail(Payment payment) {
        String description = payment.description();
        return description != null && description.toLowerCase(Locale.ROOT).contains("fail");
    }
}
