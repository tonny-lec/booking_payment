package com.booking.payment.adapter.web.config;

import com.booking.booking.application.port.BookingRepository;
import com.booking.payment.application.port.PaymentGatewayPort;
import com.booking.payment.application.port.PaymentRepository;
import com.booking.payment.application.usecase.CapturePaymentUseCase;
import com.booking.payment.application.usecase.CreatePaymentUseCase;
import com.booking.payment.application.usecase.GetPaymentUseCase;
import com.booking.payment.application.usecase.RefundPaymentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for Payment web-facing use cases.
 *
 * <p>{@link PaymentGatewayPort} is provided by the composition root
 * (bootstrap module); Slice A wires the stub gateway there.
 */
@Configuration
public class PaymentUseCaseConfig {

    @Bean
    public CreatePaymentUseCase createPaymentUseCase(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            PaymentGatewayPort paymentGateway
    ) {
        return new CreatePaymentUseCase(paymentRepository, bookingRepository, paymentGateway);
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(PaymentRepository paymentRepository) {
        return new GetPaymentUseCase(paymentRepository);
    }

    @Bean
    public CapturePaymentUseCase capturePaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentGatewayPort paymentGateway
    ) {
        return new CapturePaymentUseCase(paymentRepository, paymentGateway);
    }

    @Bean
    public RefundPaymentUseCase refundPaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentGatewayPort paymentGateway
    ) {
        return new RefundPaymentUseCase(paymentRepository, paymentGateway);
    }
}
