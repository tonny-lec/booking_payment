package com.booking.domain.iam.model;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * Value object representing a hashed password.
 *
 * <p>HashedPassword stores a bcrypt hash and never contains plaintext passwords.
 * The hash format is validated to ensure it follows the bcrypt specification.
 *
 * <p>Password verification is delegated to an external encoder (typically provided
 * by the infrastructure layer) through a functional interface, keeping the domain
 * layer free of framework dependencies.
 *
 * <p>Usage:
 * <pre>{@code
 * // Create from existing hash (e.g., loaded from database)
 * HashedPassword hash = HashedPassword.of("$2a$12$...");
 *
 * // Verify password using an encoder
 * boolean valid = hash.matches("rawPassword", (raw, hashed) -> encoder.matches(raw, hashed));
 * }</pre>
 *
 * <p><b>Security note:</b> This class never logs or exposes the actual hash value
 * to prevent accidental exposure of sensitive data.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bcrypt">Bcrypt</a>
 */
public final class HashedPassword {

    /**
     * Pattern for valid bcrypt hash formats.
     *
     * <p>Bcrypt hashes follow the format: $2[aby]$cost$salt+hash
     * <ul>
     *   <li>$2a$, $2b$, $2y$ - version identifiers</li>
     *   <li>cost - 2 digit work factor (04-31)</li>
     *   <li>22 characters of salt (base64)</li>
     *   <li>31 characters of hash (base64)</li>
     * </ul>
     *
     * <p>Cost factor validation: 04-31 (0[4-9]|[12][0-9]|3[01])
     */
    private static final Pattern BCRYPT_PATTERN = Pattern.compile(
            "^\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}$"
    );

    private final String value;

    private HashedPassword(String value) {
        this.value = value;
    }

    /**
     * Creates a HashedPassword from an existing bcrypt hash.
     *
     * <p>This method validates that the hash follows the bcrypt format.
     * It should be used when loading hashes from storage.
     *
     * @param hash the bcrypt hash string
     * @return a new HashedPassword instance
     * @throws NullPointerException if hash is null
     * @throws IllegalArgumentException if hash is not a valid bcrypt format
     */
    public static HashedPassword of(String hash) {
        Objects.requireNonNull(hash, "Password hash must not be null");

        if (!BCRYPT_PATTERN.matcher(hash).matches()) {
            throw new IllegalArgumentException("Invalid bcrypt hash format");
        }

        return new HashedPassword(hash);
    }

    /**
     * Creates a HashedPassword from a hash without strict validation.
     *
     * <p>This method performs minimal validation and should only be used
     * when the hash format is guaranteed to be valid (e.g., directly after
     * encoding with a trusted encoder).
     *
     * @param hash the bcrypt hash string
     * @return a new HashedPassword instance
     * @throws NullPointerException if hash is null
     * @throws IllegalArgumentException if hash is empty
     */
    public static HashedPassword fromTrustedSource(String hash) {
        Objects.requireNonNull(hash, "Password hash must not be null");

        if (hash.isEmpty()) {
            throw new IllegalArgumentException("Password hash must not be empty");
        }

        return new HashedPassword(hash);
    }

    /**
     * Verifies if the provided raw password matches this hash.
     *
     * <p>The actual comparison is delegated to the provided matcher function,
     * which typically wraps a password encoder from the infrastructure layer.
     *
     * <p>Example:
     * <pre>{@code
     * // Using Spring's BCryptPasswordEncoder
     * boolean matches = hashedPassword.matches(
     *     rawPassword,
     *     (raw, hashed) -> passwordEncoder.matches(raw, hashed)
     * );
     * }</pre>
     *
     * @param rawPassword the plaintext password to verify
     * @param matcher function that compares raw password with hash
     * @return true if the password matches, false otherwise
     * @throws NullPointerException if rawPassword or matcher is null
     */
    public boolean matches(String rawPassword, BiPredicate<String, String> matcher) {
        Objects.requireNonNull(rawPassword, "Raw password must not be null");
        Objects.requireNonNull(matcher, "Password matcher must not be null");

        return matcher.test(rawPassword, value);
    }

    /**
     * Returns the bcrypt hash value.
     *
     * <p><b>Warning:</b> This method exposes the hash for persistence purposes.
     * Never log or display this value.
     *
     * @return the bcrypt hash string
     */
    public String value() {
        return value;
    }

    /**
     * Returns the bcrypt cost factor (work factor) from the hash.
     *
     * @return the cost factor as an integer (e.g., 12 for $2a$12$...)
     */
    public int costFactor() {
        // Cost factor is at position 4-5 in the hash (after "$2a$")
        return Integer.parseInt(value.substring(4, 6));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashedPassword that = (HashedPassword) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Returns a masked representation for safe logging.
     *
     * <p>Never exposes the actual hash to prevent security issues.
     *
     * @return a masked string representation
     */
    @Override
    public String toString() {
        return "HashedPassword[PROTECTED]";
    }
}
