package com.booking.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("of should create money with normalized currency")
    void ofShouldCreateMoneyWithNormalizedCurrency() {
        Money money = Money.of(10000, " jpy ");

        assertThat(money.amount()).isEqualTo(10000);
        assertThat(money.currency()).isEqualTo("JPY");
    }

    @ParameterizedTest(name = "amount {0} is rejected")
    @ValueSource(ints = {0, -1, -10000})
    @DisplayName("of should reject non-positive amount")
    void ofShouldRejectNonPositiveAmount(int amount) {
        assertThatThrownBy(() -> Money.of(amount, "JPY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    @DisplayName("of should reject non ISO 4217 currency")
    void ofShouldRejectNonIsoCurrency() {
        assertThatThrownBy(() -> Money.of(100, "ZZZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 4217");
    }

    @Test
    @DisplayName("add should sum amounts with same currency")
    void addShouldSumAmountsWithSameCurrency() {
        assertThat(Money.of(100, "JPY").add(Money.of(50, "JPY"))).isEqualTo(Money.of(150, "JPY"));
    }

    @Test
    @DisplayName("add should reject currency mismatch")
    void addShouldRejectCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of(100, "JPY").add(Money.of(50, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    @DisplayName("subtract should reject result of zero or less")
    void subtractShouldRejectResultOfZeroOrLess() {
        assertThatThrownBy(() -> Money.of(100, "JPY").subtract(Money.of(100, "JPY")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isGreaterThan should compare amounts")
    void isGreaterThanShouldCompareAmounts() {
        assertThat(Money.of(101, "JPY").isGreaterThan(Money.of(100, "JPY"))).isTrue();
        assertThat(Money.of(100, "JPY").isGreaterThan(Money.of(100, "JPY"))).isFalse();
    }
}
