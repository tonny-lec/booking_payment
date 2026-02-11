package com.booking.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money")
class MoneyTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create money with normalized currency")
        void shouldCreateMoneyWithNormalizedCurrency() {
            Money money = Money.of(1500, " jpy ");

            assertThat(money.amount()).isEqualTo(1500);
            assertThat(money.currency()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("should reject amount less than one")
        void shouldRejectAmountLessThanOne() {
            assertThatThrownBy(() -> Money.of(0, "JPY"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("should reject invalid currency code")
        void shouldRejectInvalidCurrencyCode() {
            assertThatThrownBy(() -> Money.of(100, "ZZZ1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ISO 4217");
        }
    }

    @Nested
    @DisplayName("operations")
    class Operations {

        @Test
        @DisplayName("add should return summed amount for same currency")
        void addShouldReturnSummedAmountForSameCurrency() {
            Money left = Money.of(1200, "JPY");
            Money right = Money.of(300, "JPY");

            Money result = left.add(right);

            assertThat(result.amount()).isEqualTo(1500);
            assertThat(result.currency()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("subtract should return reduced amount for same currency")
        void subtractShouldReturnReducedAmountForSameCurrency() {
            Money left = Money.of(1200, "JPY");
            Money right = Money.of(200, "JPY");

            Money result = left.subtract(right);

            assertThat(result.amount()).isEqualTo(1000);
            assertThat(result.currency()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("subtract should reject when result is zero or negative")
        void subtractShouldRejectWhenResultIsZeroOrNegative() {
            Money left = Money.of(500, "JPY");
            Money right = Money.of(500, "JPY");

            assertThatThrownBy(() -> left.subtract(right))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("operations should reject currency mismatch")
        void operationsShouldRejectCurrencyMismatch() {
            Money jpy = Money.of(1000, "JPY");
            Money usd = Money.of(1000, "USD");

            assertThatThrownBy(() -> jpy.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency mismatch");

            assertThatThrownBy(() -> jpy.subtract(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency mismatch");
        }

        @Test
        @DisplayName("isGreaterThan should compare amounts in same currency")
        void isGreaterThanShouldCompareAmountsInSameCurrency() {
            Money high = Money.of(2000, "JPY");
            Money low = Money.of(1000, "JPY");

            assertThat(high.isGreaterThan(low)).isTrue();
            assertThat(low.isGreaterThan(high)).isFalse();
        }
    }
}
