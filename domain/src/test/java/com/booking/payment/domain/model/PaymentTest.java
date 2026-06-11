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

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create pending payment")
        void shouldCreatePendingPayment() {
            Payment payment = newPayment();

            assertThat(payment.id()).isNotNull();
            assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.money()).isEqualTo(Money.of(10000, "JPY"));
            assertThat(payment.capturedAmount()).isNull();
            assertThat(payment.refundedAmount()).isNull();
            assertThat(payment.gatewayTransactionId()).isNull();
            assertThat(payment.failureReason()).isNull();
            assertThat(payment.createdAt()).isEqualTo(Instant.parse("2026-03-01T00:00:00Z"));
            assertThat(payment.updatedAt()).isEqualTo(payment.createdAt());
        }

        @Test
        @DisplayName("should normalize blank description to null")
        void shouldNormalizeBlankDescriptionToNull() {
            Payment payment = Payment.create(
                    BookingId.generate(), UserId.generate(), Money.of(10000, "JPY"),
                    IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK), "   ", FIXED_CLOCK);

            assertThat(payment.description()).isNull();
        }

        @Test
        @DisplayName("should reject description over max length")
        void shouldRejectDescriptionOverMaxLength() {
            String tooLong = "a".repeat(Payment.MAX_DESCRIPTION_LENGTH + 1);

            assertThatThrownBy(() -> Payment.create(
                    BookingId.generate(), UserId.generate(), Money.of(10000, "JPY"),
                    IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK), tooLong, FIXED_CLOCK))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_description_too_long"));
        }
    }

    @Nested
    @DisplayName("authorize / fail")
    class AuthorizeFail {

        @Test
        @DisplayName("authorize should transition PENDING to AUTHORIZED")
        void authorizeShouldTransitionPendingToAuthorized() {
            Payment payment = newPayment();

            payment.authorize("txn_123");

            assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.gatewayTransactionId()).isEqualTo("txn_123");
        }

        @Test
        @DisplayName("authorize should reject non-pending payment")
        void authorizeShouldRejectNonPendingPayment() {
            Payment payment = authorizedPayment();

            assertThatThrownBy(() -> payment.authorize("txn_456"))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_not_authorizable"));
        }

        @Test
        @DisplayName("fail should transition PENDING to FAILED with reason")
        void failShouldTransitionPendingToFailedWithReason() {
            Payment payment = newPayment();

            payment.fail("card_declined");

            assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.failureReason()).isEqualTo("card_declined");
        }

        @Test
        @DisplayName("fail should require non-blank reason")
        void failShouldRequireNonBlankReason() {
            Payment payment = newPayment();

            assertThatThrownBy(() -> payment.fail("  "))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_failureReason_required"));
        }
    }

    @Nested
    @DisplayName("capture")
    class Capture {

        @Test
        @DisplayName("should capture full amount when amount is null")
        void shouldCaptureFullAmountWhenAmountIsNull() {
            Payment payment = authorizedPayment();

            payment.capture(null);

            assertThat(payment.status()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(payment.capturedAmount()).isEqualTo(10000);
        }

        @Test
        @DisplayName("should capture partial amount")
        void shouldCapturePartialAmount() {
            Payment payment = authorizedPayment();

            payment.capture(4000);

            assertThat(payment.capturedAmount()).isEqualTo(4000);
        }

        @Test
        @DisplayName("should reject capture exceeding authorized amount")
        void shouldRejectCaptureExceedingAuthorizedAmount() {
            Payment payment = authorizedPayment();

            assertThatThrownBy(() -> payment.capture(10001))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_capture_exceeds_authorized"));
        }

        @Test
        @DisplayName("should reject capture of pending payment")
        void shouldRejectCaptureOfPendingPayment() {
            Payment payment = newPayment();

            assertThatThrownBy(() -> payment.capture(null))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_not_capturable"));
        }
    }

    @Nested
    @DisplayName("void / refund")
    class VoidRefund {

        @Test
        @DisplayName("voidAuthorization should refund full amount of authorized payment")
        void voidAuthorizationShouldRefundFullAmountOfAuthorizedPayment() {
            Payment payment = authorizedPayment();

            payment.voidAuthorization();

            assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.refundedAmount()).isEqualTo(10000);
        }

        @Test
        @DisplayName("voidAuthorization should reject captured payment")
        void voidAuthorizationShouldRejectCapturedPayment() {
            Payment payment = capturedPayment();

            assertThatThrownBy(payment::voidAuthorization)
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_not_voidable"));
        }

        @Test
        @DisplayName("refund should refund full captured amount when amount is null")
        void refundShouldRefundFullCapturedAmountWhenAmountIsNull() {
            Payment payment = capturedPayment();

            payment.refund(null);

            assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.refundedAmount()).isEqualTo(10000);
        }

        @Test
        @DisplayName("refund should reject amount exceeding captured amount")
        void refundShouldRejectAmountExceedingCapturedAmount() {
            Payment payment = authorizedPayment();
            payment.capture(4000);

            assertThatThrownBy(() -> payment.refund(4001))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_refund_exceeds_captured"));
        }

        @Test
        @DisplayName("refund should reject pending payment")
        void refundShouldRejectPendingPayment() {
            Payment payment = newPayment();

            assertThatThrownBy(() -> payment.refund(null))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo("payment_not_refundable"));
        }
    }

    @Test
    @DisplayName("isOwnedBy should compare payer user id")
    void isOwnedByShouldComparePayerUserId() {
        UserId owner = UserId.generate();
        Payment payment = Payment.create(
                BookingId.generate(), owner, Money.of(10000, "JPY"),
                IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK), null, FIXED_CLOCK);

        assertThat(payment.isOwnedBy(owner)).isTrue();
        assertThat(payment.isOwnedBy(UserId.generate())).isFalse();
    }

    private static Payment newPayment() {
        return Payment.create(
                BookingId.generate(),
                UserId.generate(),
                Money.of(10000, "JPY"),
                IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK),
                "meeting room",
                FIXED_CLOCK
        );
    }

    private static Payment authorizedPayment() {
        Payment payment = newPayment();
        payment.authorize("txn_123");
        return payment;
    }

    private static Payment capturedPayment() {
        Payment payment = authorizedPayment();
        payment.capture(null);
        return payment;
    }
}
