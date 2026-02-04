package com.booking.iam.application.port;

/**
 * Port interface for password hashing and verification.
 *
 * <p>This interface abstracts password encoding (e.g., BCrypt) from the
 * application layer. Implementations are provided by the adapter layer.
 *
 * <p>Usage example:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class RegisterUserUseCase {
 *     private final PasswordEncoder passwordEncoder;
 *
 *     public HashedPassword register(String rawPassword) {
 *         String hash = passwordEncoder.encode(rawPassword);
 *         return HashedPassword.fromTrustedSource(hash);
 *     }
 * }
 * }</pre>
 */
public interface PasswordEncoder {

    /**
     * Encodes a raw password.
     *
     * @param rawPassword the plaintext password
     * @return the encoded password hash
     */
    String encode(String rawPassword);

    /**
     * Verifies that a raw password matches an encoded hash.
     *
     * @param rawPassword the plaintext password
     * @param encodedPassword the stored password hash
     * @return true if the password matches, false otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
