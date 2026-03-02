package com.booking.payment.domain.model;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment")
class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-02-11T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("create should initialize payment in PENDING state")
        void createShouldInitializePendingPayment() {
            Payment payment = createPendingPayment();

            assertThat(payment.id()).isNotNull();
            assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.money().amount()).isEqualTo(5000);
            assertThat(payment.money().currency()).isEqualTo("JPY");
            assertThat(payment.capturedAmount()).isNull();
            assertThat(payment.refundedAmount()).isNull();
            assertThat(payment.createdAt()).isEqualTo(NOW);
            assertThat(payment.updatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("authorize should move payment to AUTHORIZED")
        void authorizeShouldMoveToAuthorized() {
            Payment payment = createPendingPayment();

            payment.authorize(" tx-001 ");

            assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.gatewayTransactionId()).isEqualTo("tx-001");
            assertThat(payment.failureReason()).isNull();
        }

        @Test
        @DisplayName("authorize should reject when payment is not pending")
        void authorizeShouldRejectWhenNotPending() {
            Payment payment = createPendingPayment();
            payment.fail("gateway_error");

            assertThatThrownBy(() -> payment.authorize("tx-001"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("payment_not_authorizable"));
        }

        @Test
        @DisplayName("fail should move payment to FAILED")
        void failShouldMoveToFailed() {
            Payment payment = createPendingPayment();

            payment.fail("gateway timeout");

            assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.failureReason()).isEqualTo("gateway timeout");
        }
    }

    @Nested
    @DisplayName("capture and refund")
    class CaptureAndRefund {

        @Test
        @DisplayName("capture with null amount should capture full authorized amount")
        void captureWithNullAmountShouldCaptureFullAmount() {
            Payment payment = createPendingPayment();
            payment.authorize("tx-001");

            payment.capture(null);

            assertThat(payment.status()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(payment.capturedAmount()).isEqualTo(5000);
        }

        @Test
        @DisplayName("capture should reject amount above authorized")
        void captureShouldRejectAmountAboveAuthorized() {
            Payment payment = createPendingPayment();
            payment.authorize("tx-001");

            assertThatThrownBy(() -> payment.capture(5001))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleViolationException) ex).getErrorCode())
                            .isEqualTo("payment_capture_exceeds_authorized"));
        }

        @Test
        @DisplayName("refund should move CAPTURED payment to REFUNDED")
        void refundShouldMoveCapturedPaymentToRefunded() {
            Payment payment = createPendingPayment();
            payment.authorize("tx-001");
            payment.capture(3000);

            payment.refund(1000);

            assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.refundedAmount()).isEqualTo(1000);
        }

        @Test
        @DisplayName("voidAuthorization should move AUTHORIZED payment to REFUNDED")
        void voidAuthorizationShouldMoveAuthorizedPaymentToRefunded() {
            Payment payment = createPendingPayment();
            payment.authorize("tx-001");

            payment.voidAuthorization();

            assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.refundedAmount()).isEqualTo(5000);
        }
    }

    private Payment createPendingPayment() {
        return Payment.create(
                BookingId.generate(),
                UserId.generate(),
                Money.of(5000, "JPY"),
                IdempotencyKey.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), FIXED_CLOCK),
                "payment for booking",
                FIXED_CLOCK
        );
    }
}
