package com.booking.iam.domain.model;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Value object representing a hashed token (typically SHA-256).
 *
 * <p>HashedToken stores a cryptographic hash of a refresh token and never contains
 * the plaintext token value. This ensures that even if the database is compromised,
 * the actual tokens cannot be recovered.
 *
 * <p>Token hashing is delegated to an external hasher function (typically provided
 * by the infrastructure layer), keeping the domain layer free of framework dependencies.
 *
 * <p>Usage:
 * <pre>{@code
 * // Create from existing hash (e.g., loaded from database)
 * HashedToken hash = HashedToken.of("a1b2c3d4...");
 *
 * // Verify token using a hasher
 * boolean valid = hash.matches("rawToken", token -> sha256Hex(token));
 * }</pre>
 *
 * <p><b>Security note:</b> This class never logs or exposes the actual hash value
 * to prevent potential security issues in high-security contexts.
 *
 * @param value the hash string (typically SHA-256 hex-encoded, 64 characters)
 * @see RefreshToken
 */
public record HashedToken(String value) {

    /**
     * Pattern for valid SHA-256 hex-encoded hash (64 hexadecimal characters).
     */
    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    /**
     * Canonical constructor with null validation.
     *
     * @param value the hash value
     * @throws NullPointerException if value is null
     */
    public HashedToken {
        Objects.requireNonNull(value, "Token hash must not be null");
    }

    /**
     * Creates a HashedToken from an existing hash with format validation.
     *
     * <p>This method validates that the hash follows the SHA-256 hex format.
     * It should be used when loading hashes from storage.
     *
     * @param hash the SHA-256 hex-encoded hash string
     * @return a new HashedToken instance
     * @throws NullPointerException if hash is null
     * @throws IllegalArgumentException if hash is not a valid SHA-256 hex format
     */
    public static HashedToken of(String hash) {
        Objects.requireNonNull(hash, "Token hash must not be null");

        if (!SHA256_HEX_PATTERN.matcher(hash).matches()) {
            throw new IllegalArgumentException(
                    "Invalid token hash format: must be 64 hexadecimal characters (SHA-256)");
        }

        return new HashedToken(hash);
    }

    /**
     * Creates a HashedToken from a hash without strict validation.
     *
     * <p>This method performs minimal validation and should only be used
     * when the hash format is guaranteed to be valid (e.g., directly after
     * hashing with a trusted hasher).
     *
     * @param hash the hash string
     * @return a new HashedToken instance
     * @throws NullPointerException if hash is null
     * @throws IllegalArgumentException if hash is empty
     */
    public static HashedToken fromTrustedSource(String hash) {
        Objects.requireNonNull(hash, "Token hash must not be null");

        if (hash.isEmpty()) {
            throw new IllegalArgumentException("Token hash must not be empty");
        }

        return new HashedToken(hash);
    }

    /**
     * Verifies if the provided raw token matches this hash.
     *
     * <p>The actual hashing is delegated to the provided hasher function,
     * which typically implements SHA-256 hashing from the infrastructure layer.
     *
     * <p>Example:
     * <pre>{@code
     * // Using a SHA-256 hasher
     * boolean matches = hashedToken.matches(
     *     rawToken,
     *     token -> DigestUtils.sha256Hex(token)
     * );
     * }</pre>
     *
     * @param rawToken the plaintext token to verify
     * @param hasher function that computes hash from raw token
     * @return true if the token matches, false otherwise
     * @throws NullPointerException if rawToken or hasher is null
     */
    public boolean matches(String rawToken, Function<String, String> hasher) {
        Objects.requireNonNull(rawToken, "Raw token must not be null");
        Objects.requireNonNull(hasher, "Token hasher must not be null");

        String computedHash = hasher.apply(rawToken);
        return value.equalsIgnoreCase(computedHash);
    }

    /**
     * Returns a masked representation for safe logging.
     *
     * <p>Exposes only the first 8 characters to aid debugging while
     * protecting the full hash value.
     *
     * @return a masked string representation
     */
    public String masked() {
        if (value.length() >= 8) {
            return value.substring(0, 8) + "...";
        }
        return "***";
    }

    /**
     * Returns a masked representation for safe logging.
     *
     * @return a masked string representation
     */
    @Override
    public String toString() {
        return "HashedToken[" + masked() + "]";
    }
}
