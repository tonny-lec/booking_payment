package com.booking.iam.application.usecase;

import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.security.TokenHasher;
import com.booking.iam.domain.model.RefreshToken;
import com.booking.shared.exception.ForbiddenException;

import java.util.Objects;

/**
 * Use case for revoking refresh tokens on logout.
 */
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository must not be null");
    }

    /**
     * Revokes the refresh token if it exists. This operation is idempotent.
     *
     * @param command logout command
     */
    public void execute(LogoutCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        refreshTokenRepository.findByTokenHash(TokenHasher.hash(command.refreshToken()))
                .ifPresent(refreshToken -> revokeIfOwnedByUser(refreshToken, command.authenticatedUserId()));
    }

    private void revokeIfOwnedByUser(RefreshToken refreshToken, String authenticatedUserId) {
        if (!refreshToken.userId().asString().equals(authenticatedUserId)) {
            throw new ForbiddenException(
                    "logout_token_owner_mismatch",
                    "Authenticated user does not own the refresh token"
            );
        }
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }

    public record LogoutCommand(String refreshToken, String authenticatedUserId) {
        public LogoutCommand {
            Objects.requireNonNull(refreshToken, "refreshToken must not be null");
            Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
        }
    }
}
