package com.booking.payment.domain.model;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Value object representing money in minor units (e.g. JPY: 1, USD/EUR: cent).
 *
 * @param amount amount in minor units
 * @param currency ISO 4217 currency code
 */
public record Money(int amount, String currency) {

    public Money {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        Objects.requireNonNull(currency, "currency must not be null");
        currency = normalizeAndValidateCurrency(currency);
    }

    /**
     * Creates Money from amount and currency.
     *
     * @param amount amount in minor units
     * @param currency ISO 4217 currency code
     * @return Money
     */
    public static Money of(int amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * Adds another amount with the same currency.
     *
     * @param other other amount
     * @return added money
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amount, other.amount), currency);
    }

    /**
     * Subtracts another amount with the same currency.
     *
     * @param other other amount
     * @return subtracted money
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        int result = amount - other.amount;
        if (result <= 0) {
            throw new IllegalArgumentException("result amount must be greater than zero");
        }
        return new Money(result, currency);
    }

    /**
     * Returns true when this amount is greater than another amount.
     *
     * @param other other amount
     * @return true if this amount is greater
     */
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount > other.amount;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + currency + " vs " + other.currency
            );
        }
    }

    private static String normalizeAndValidateCurrency(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("currency must not be empty");
        }
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("currency must be a valid ISO 4217 code", ex);
        }
        return normalized;
    }
}
