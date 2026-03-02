package com.booking.payment.adapter.web.config;

import com.booking.payment.application.port.IdempotencyStore;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.application.usecase.CreatePaymentUseCase;
import com.booking.payment.application.usecase.GetPaymentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for payment use cases.
 */
@Configuration
public class PaymentUseCaseConfig {

    @Bean
    public CreatePaymentUseCase createPaymentUseCase(
            PaymentRepository paymentRepository,
            IdempotencyStore idempotencyStore,
            PaymentGatewayPort paymentGatewayPort
    ) {
        return new CreatePaymentUseCase(paymentRepository, idempotencyStore, paymentGatewayPort);
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(PaymentRepository paymentRepository) {
        return new GetPaymentUseCase(paymentRepository);
    }
}
