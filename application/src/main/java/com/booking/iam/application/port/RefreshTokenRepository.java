package com.booking.iam.application.port;

import com.booking.iam.domain.model.HashedToken;
import com.booking.iam.domain.model.RefreshToken;

import java.util.Optional;

/**
 * Port interface for refresh token persistence operations.
 *
 * <p>This repository abstracts refresh token storage for the IAM bounded context.
 * Implementations are provided by the adapter layer (e.g., JPA, JDBC).
 *
 * <p>Usage example:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class RefreshTokenUseCase {
 *     private final RefreshTokenRepository refreshTokenRepository;
 *
 *     public RefreshToken rotate(HashedToken tokenHash) {
 *         RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
 *             .orElseThrow(() -> new RefreshTokenNotFoundException());
 *         token.revoke();
 *         refreshTokenRepository.save(token);
 *         return token;
 *     }
 * }
 * }</pre>
 */
public interface RefreshTokenRepository {

    /**
     * Finds a refresh token by its hashed value.
     *
     * @param tokenHash the hashed token value
     * @return the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(HashedToken tokenHash);

    /**
     * Persists a refresh token entity.
     *
     * @param refreshToken the token to persist
     * @return the persisted token (if the implementation enriches fields)
     */
    RefreshToken save(RefreshToken refreshToken);
}
