package com.booking.iam.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a validated email address.
 *
 * <p>Email addresses are validated against a simplified RFC 5322 pattern and
 * normalized to lowercase for case-insensitive comparison. This ensures that
 * "User@Example.com" and "user@example.com" are treated as the same email.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Must match the email pattern (local-part@domain)</li>
 *   <li>Maximum length: 255 characters</li>
 *   <li>Local part: alphanumeric, dots, underscores, hyphens, plus signs</li>
 *   <li>Domain: alphanumeric with dots, at least one dot required</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * Email email = Email.of("user@example.com");
 * String normalized = email.value(); // Always lowercase
 * }</pre>
 *
 * @param value the normalized (lowercase) email address
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5322">RFC 5322</a>
 */
public record Email(String value) {

    /**
     * Maximum allowed length for an email address.
     */
    public static final int MAX_LENGTH = 255;

    /**
     * Simplified RFC 5322 email pattern.
     *
     * <p>This pattern validates:
     * <ul>
     *   <li>Local part: allows alphanumeric, dots, underscores, hyphens, plus signs</li>
     *   <li>Domain: requires at least one dot with alphanumeric segments</li>
     * </ul>
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * Canonical constructor - should not be called directly.
     * Use {@link #of(String)} factory method instead.
     *
     * @param value the normalized email value
     * @throws NullPointerException if value is null
     */
    public Email {
        Objects.requireNonNull(value, "Email must not be null");
    }

    /**
     * Creates an Email from a string value.
     *
     * <p>The email is validated against the pattern and normalized to lowercase.
     *
     * @param value the email address string
     * @return a new Email instance with normalized (lowercase) value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is empty, exceeds max length, or is invalid format
     */
    public static Email of(String value) {
        Objects.requireNonNull(value, "Email must not be null");

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Email must not be empty");
        }

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Email must not exceed " + MAX_LENGTH + " characters");
        }

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Email format is invalid: " + maskEmail(trimmed));
        }

        // Normalize to lowercase for case-insensitive comparison
        return new Email(trimmed.toLowerCase());
    }

    /**
     * Returns the local part of the email (before the @ symbol).
     *
     * @return the local part
     */
    public String localPart() {
        int atIndex = value.indexOf('@');
        return value.substring(0, atIndex);
    }

    /**
     * Returns the domain part of the email (after the @ symbol).
     *
     * @return the domain part
     */
    public String domain() {
        int atIndex = value.indexOf('@');
        return value.substring(atIndex + 1);
    }

    /**
     * Masks the email for logging purposes to protect PII.
     *
     * <p>Example: "user@example.com" becomes "u***@example.com"
     *
     * @return the masked email string
     */
    public String masked() {
        return maskEmail(value);
    }

    /**
     * Masks an email string for safe logging.
     */
    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * Returns a masked representation for safe logging.
     *
     * <p>Use masked version in toString to prevent accidental PII exposure in logs.
     *
     * @return a masked string representation
     */
    @Override
    public String toString() {
        return "Email[" + masked() + "]";
    }
}
