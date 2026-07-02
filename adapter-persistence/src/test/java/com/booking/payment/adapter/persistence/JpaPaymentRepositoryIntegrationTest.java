package com.booking.payment.adapter.persistence;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import com.booking.payment.domain.model.PaymentStatus;
import com.booking.shared.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaPaymentRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("JpaPaymentRepository integration")
class JpaPaymentRepositoryIntegrationTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private JpaPaymentRepository paymentRepository;

    @Nested
    @DisplayName("save/find")
    class SaveFind {

        @Test
        @DisplayName("should persist authorized payment and find by id")
        void shouldPersistAuthorizedPaymentAndFindById() {
            Payment payment = newPayment();
            payment.authorize("txn_123");

            Payment saved = paymentRepository.save(payment);

            Payment reloaded = paymentRepository.findById(saved.id()).orElseThrow();
            assertThat(reloaded.id()).isEqualTo(payment.id());
            assertThat(reloaded.bookingId()).isEqualTo(payment.bookingId());
            assertThat(reloaded.userId()).isEqualTo(payment.userId());
            assertThat(reloaded.money()).isEqualTo(Money.of(10000, "JPY"));
            assertThat(reloaded.status()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(reloaded.gatewayTransactionId()).isEqualTo("txn_123");
            assertThat(reloaded.description()).isEqualTo("meeting room");
            assertThat(reloaded.idempotencyKey().value()).isEqualTo(payment.idempotencyKey().value());
        }

        @Test
        @DisplayName("should persist failed payment with failure reason")
        void shouldPersistFailedPaymentWithFailureReason() {
            Payment payment = newPayment();
            payment.fail("card_declined");

            Payment reloaded = paymentRepository.save(payment);

            assertThat(reloaded.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(reloaded.failureReason()).isEqualTo("card_declined");
        }

        @Test
        @DisplayName("should return empty when id does not exist")
        void shouldReturnEmptyWhenIdDoesNotExist() {
            assertThat(paymentRepository.findById(PaymentId.generate())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdempotencyKey")
    class FindByIdempotencyKey {

        @Test
        @DisplayName("should find payment by idempotency key")
        void shouldFindPaymentByIdempotencyKey() {
            Payment payment = newPayment();
            paymentRepository.save(payment);

            assertThat(paymentRepository.findByIdempotencyKey(payment.idempotencyKey().value()))
                    .hasValueSatisfying(found -> assertThat(found.id()).isEqualTo(payment.id()));
        }

        @Test
        @DisplayName("should return empty when key was never used")
        void shouldReturnEmptyWhenKeyWasNeverUsed() {
            assertThat(paymentRepository.findByIdempotencyKey(UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsActiveByBookingId")
    class ExistsActiveByBookingId {

        @Test
        @DisplayName("should detect active payment for booking")
        void shouldDetectActivePaymentForBooking() {
            Payment payment = newPayment();
            payment.authorize("txn_123");
            paymentRepository.save(payment);

            assertThat(paymentRepository.existsActiveByBookingId(payment.bookingId())).isTrue();
        }

        @Test
        @DisplayName("should ignore failed payments")
        void shouldIgnoreFailedPayments() {
            Payment payment = newPayment();
            payment.fail("card_declined");
            paymentRepository.save(payment);

            assertThat(paymentRepository.existsActiveByBookingId(payment.bookingId())).isFalse();
        }

        @Test
        @DisplayName("should return false when booking has no payments")
        void shouldReturnFalseWhenBookingHasNoPayments() {
            assertThat(paymentRepository.existsActiveByBookingId(BookingId.generate())).isFalse();
        }
    }

    @Nested
    @DisplayName("findLatestByBookingId")
    class FindLatestByBookingId {

        @Test
        @DisplayName("should find latest payment for booking including refunded terminal state")
        void shouldFindLatestPaymentForBookingIncludingRefundedTerminalState() {
            BookingId bookingId = BookingId.generate();
            Payment older = newPayment(bookingId, Instant.parse("2026-03-01T00:00:00Z"));
            older.fail("card_declined");
            paymentRepository.save(older);
            Payment latest = newPayment(bookingId, Instant.parse("2026-03-02T00:00:00Z"));
            latest.authorize("txn_latest");
            latest.voidAuthorization();
            paymentRepository.save(latest);

            assertThat(paymentRepository.findLatestByBookingId(bookingId))
                    .hasValueSatisfying(found -> {
                        assertThat(found.id()).isEqualTo(latest.id());
                        assertThat(found.status()).isEqualTo(PaymentStatus.REFUNDED);
                    });
        }

        @Test
        @DisplayName("should return empty when booking has no payments")
        void shouldReturnEmptyWhenBookingHasNoPayments() {
            assertThat(paymentRepository.findLatestByBookingId(BookingId.generate())).isEmpty();
        }
    }

    @Test
    @DisplayName("should preserve refund idempotency keys when saving payment")
    void shouldPreserveRefundIdempotencyKeysWhenSavingPayment() {
        Payment payment = newPayment();
        payment.authorize("txn_existing");
        payment.capture(10000);
        IdempotencyKey firstRefundKey = IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK);
        IdempotencyKey secondRefundKey = IdempotencyKey.of(UUID.randomUUID(), FIXED_CLOCK);
        payment.refund(3000, firstRefundKey);
        payment.refund(1000, secondRefundKey);

        paymentRepository.save(payment);

        assertThat(paymentRepository.findById(payment.id()))
                .hasValueSatisfying(found -> {
                    assertThat(found.refundRequestAmounts())
                            .containsEntry(firstRefundKey.value(), 3000)
                            .containsEntry(secondRefundKey.value(), 1000);
                });
    }

    @Test
    @DisplayName("should translate duplicate idempotency key into ConflictException")
    void shouldTranslateDuplicateIdempotencyKeyIntoConflictException() {
        Payment first = newPayment();
        paymentRepository.save(first);

        Payment second = Payment.create(
                BookingId.generate(),
                UserId.generate(),
                Money.of(20000, "JPY"),
                first.idempotencyKey(),
                "same key, different payment",
                FIXED_CLOCK
        );

        assertThatThrownBy(() -> paymentRepository.save(second))
                .isInstanceOfSatisfying(ConflictException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo("payment_idempotency_key_conflict"));
    }

    private static Payment newPayment() {
        return newPayment(BookingId.generate(), FIXED_CLOCK.instant());
    }

    private static Payment newPayment(BookingId bookingId, Instant createdAt) {
        return Payment.create(
                bookingId,
                UserId.generate(),
                Money.of(10000, "JPY"),
                IdempotencyKey.of(UUID.randomUUID(), Clock.fixed(createdAt, ZoneOffset.UTC)),
                "meeting room",
                Clock.fixed(createdAt, ZoneOffset.UTC)
        );
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.booking.payment.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.booking.payment.adapter.persistence.repository")
    static class TestApplication {
    }
}
