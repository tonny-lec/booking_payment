package com.booking.iam.application.usecase;

import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.port.TokenGenerator;
import com.booking.iam.application.port.UserRepository;
import com.booking.iam.application.security.TokenHasher;
import com.booking.iam.domain.model.AuthenticationFailureReason;
import com.booking.iam.domain.model.RefreshToken;
import com.booking.iam.domain.model.User;
import com.booking.shared.exception.UnauthorizedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Use case for rotating refresh tokens and issuing new access tokens.
 */
public class RefreshTokenUseCase {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String TOKEN_TYPE = "Bearer";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final Clock clock;

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenGenerator tokenGenerator
    ) {
        this(refreshTokenRepository, userRepository, tokenGenerator, Clock.systemUTC());
    }

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenGenerator tokenGenerator,
            Clock clock
    ) {
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Executes refresh token rotation and returns new tokens.
     *
     * @param command refresh token command
     * @return token response
     */
    public TokenResponse execute(RefreshTokenCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        RefreshToken existing = refreshTokenRepository.findByTokenHash(TokenHasher.hash(command.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException(
                        AuthenticationFailureReason.INVALID_CREDENTIALS.code(),
                        "Invalid refresh token"
                ));

        if (!existing.isValid()) {
            throw new UnauthorizedException(
                    AuthenticationFailureReason.INVALID_CREDENTIALS.code(),
                    "Refresh token is expired or revoked"
            );
        }

        User user = userRepository.findById(existing.userId())
                .orElseThrow(() -> new UnauthorizedException(
                        AuthenticationFailureReason.INVALID_CREDENTIALS.code(),
                        "User not found for refresh token"
                ));

        existing.revoke();
        refreshTokenRepository.save(existing);

        TokenGenerator.TokenPair tokens = tokenGenerator.generateTokens(
                user,
                ACCESS_TOKEN_TTL,
                REFRESH_TOKEN_TTL
        );

        Instant expiresAt = Instant.now(clock).plus(REFRESH_TOKEN_TTL);
        RefreshToken newToken = RefreshToken.create(
                user.id(),
                TokenHasher.hash(tokens.refreshToken()),
                expiresAt
        );
        refreshTokenRepository.save(newToken);

        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                TOKEN_TYPE,
                tokens.expiresInSeconds()
        );
    }

    public record RefreshTokenCommand(String refreshToken) {
        public RefreshTokenCommand {
            Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        }
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
        public TokenResponse {
            Objects.requireNonNull(accessToken, "accessToken must not be null");
            Objects.requireNonNull(refreshToken, "refreshToken must not be null");
            Objects.requireNonNull(tokenType, "tokenType must not be null");
        }
    }
}
