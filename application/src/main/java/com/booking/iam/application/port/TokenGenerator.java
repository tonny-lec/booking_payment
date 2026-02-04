package com.booking.iam.application.port;

import com.booking.iam.domain.model.User;

import java.time.Duration;
import java.util.Objects;

/**
 * Port interface for generating access and refresh tokens.
 *
 * <p>This interface abstracts token generation logic (e.g., JWT signing)
 * from the application layer. Implementations are provided by the adapter
 * layer and may integrate with key management or security libraries.
 *
 * <p>Usage example:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class LoginUseCase {
 *     private final TokenGenerator tokenGenerator;
 *
 *     public TokenPair login(User user) {
 *         return tokenGenerator.generateTokens(user,
 *             Duration.ofHours(1),
 *             Duration.ofDays(7));
 *     }
 * }
 * }</pre>
 */
public interface TokenGenerator {

    /**
     * Generates an access token and a refresh token for the given user.
     *
     * @param user the authenticated user
     * @param accessTokenTtl access token time-to-live
     * @param refreshTokenTtl refresh token time-to-live
     * @return a token pair with access/refresh tokens and access token expiry
     */
    TokenPair generateTokens(User user, Duration accessTokenTtl, Duration refreshTokenTtl);

    /**
     * Token pair returned by the generator.
     *
     * @param accessToken the access token (JWT)
     * @param refreshToken the refresh token (opaque)
     * @param expiresInSeconds access token expiry in seconds
     */
    record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
        public TokenPair {
            Objects.requireNonNull(accessToken, "Access token must not be null");
            Objects.requireNonNull(refreshToken, "Refresh token must not be null");
        }
    }
}
