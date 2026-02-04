package com.booking.iam.application.usecase;

import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.security.TokenHasher;
import com.booking.iam.domain.model.RefreshToken;

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
                .ifPresent(this::revokeAndSave);
    }

    private void revokeAndSave(RefreshToken refreshToken) {
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }

    public record LogoutCommand(String refreshToken) {
        public LogoutCommand {
            Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        }
    }
}
