package com.booking.payment.adapter.persistence;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.PaymentId;
import org.hibernate.exception.ConstraintViolationException;
import jakarta.persistence.EntityManager;
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
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final Instant NOW = Instant.parse("2026-02-11T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired
    private JpaPaymentRepository paymentRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("save/find")
    class SaveFind {

        @Test
        @DisplayName("should persist payment and find by id and idempotency key")
        void shouldPersistPaymentAndFindByIdAndIdempotencyKey() {
            Payment payment = paymentRepository.save(newPendingPayment(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440021")
            ));

            Payment byId = paymentRepository.findById(payment.id()).orElseThrow();
            Payment byKey = paymentRepository.findByIdempotencyKey(payment.idempotencyKey()).orElseThrow();

            assertThat(byId.id()).isEqualTo(payment.id());
            assertThat(byId.bookingId()).isEqualTo(payment.bookingId());
            assertThat(byId.userId()).isEqualTo(payment.userId());
            assertThat(byId.money()).isEqualTo(payment.money());
            assertThat(byId.status()).isEqualTo(payment.status());

            assertThat(byKey.id()).isEqualTo(payment.id());
            assertThat(byKey.idempotencyKey().value()).isEqualTo(payment.idempotencyKey().value());
        }

        @Test
        @DisplayName("should update existing payment")
        void shouldUpdateExistingPayment() {
            Payment payment = paymentRepository.save(newPendingPayment(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440022")
            ));
            payment.authorize("stub-tx-002");
            payment.capture(1500);

            paymentRepository.save(payment);

            Payment reloaded = paymentRepository.findById(payment.id()).orElseThrow();
            assertThat(reloaded.status().code()).isEqualTo("CAPTURED");
            assertThat(reloaded.capturedAmount()).isEqualTo(1500);
        }
    }

    @Test
    @DisplayName("should enforce unique idempotency key constraint")
    void shouldEnforceUniqueIdempotencyKeyConstraint() {
        UUID sameKey = UUID.fromString("550e8400-e29b-41d4-a716-446655440023");
        paymentRepository.save(newPendingPayment(sameKey));

        assertThatThrownBy(() -> {
            paymentRepository.save(newPendingPayment(sameKey));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    @DisplayName("should return empty when payment id does not exist")
    void shouldReturnEmptyWhenPaymentIdDoesNotExist() {
        assertThat(paymentRepository.findById(PaymentId.generate())).isEmpty();
    }

    private Payment newPendingPayment(UUID key) {
        return Payment.create(
                BookingId.generate(),
                UserId.generate(),
                Money.of(2000, "JPY"),
                IdempotencyKey.of(key, FIXED_CLOCK),
                "integration test payment",
                FIXED_CLOCK
        );
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.booking.payment.adapter.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.booking.payment.adapter.persistence.repository")
    static class TestApplication {
    }
}
