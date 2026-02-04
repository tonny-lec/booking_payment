package com.booking.iam.application.usecase;

import com.booking.iam.application.port.PasswordEncoder;
import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.port.TokenGenerator;
import com.booking.iam.application.port.UserRepository;
import com.booking.iam.application.security.TokenHasher;
import com.booking.iam.domain.model.AuthenticationFailureReason;
import com.booking.iam.domain.model.AuthenticationResult;
import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.RefreshToken;
import com.booking.iam.domain.model.User;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.UnauthorizedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Use case for user login and token issuance.
 */
public class LoginUseCase {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public LoginUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this(userRepository, refreshTokenRepository, tokenGenerator, passwordEncoder, Clock.systemUTC());
    }

    public LoginUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository must not be null");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Executes login and returns issued tokens.
     *
     * @param command login command
     * @return token response
     */
    public TokenResponse execute(LoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UnauthorizedException(
                        AuthenticationFailureReason.INVALID_CREDENTIALS.code(),
                        AuthenticationFailureReason.INVALID_CREDENTIALS.description()
                ));

        AuthenticationResult result = user.authenticate(command.password(), passwordEncoder::matches);

        if (result.isFailure()) {
            userRepository.save(user);
            throw mapFailure(result.failureReason().orElse(AuthenticationFailureReason.INVALID_CREDENTIALS));
        }

        userRepository.save(user);

        TokenGenerator.TokenPair tokens = tokenGenerator.generateTokens(
                user,
                ACCESS_TOKEN_TTL,
                REFRESH_TOKEN_TTL
        );

        Instant expiresAt = Instant.now(clock).plus(REFRESH_TOKEN_TTL);
        RefreshToken refreshToken = RefreshToken.create(
                user.id(),
                TokenHasher.hash(tokens.refreshToken()),
                expiresAt
        );
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                TOKEN_TYPE,
                tokens.expiresInSeconds()
        );
    }

    private RuntimeException mapFailure(AuthenticationFailureReason reason) {
        return switch (reason) {
            case ACCOUNT_LOCKED, ACCOUNT_NOT_ACTIVE, RATE_LIMITED -> new ForbiddenException(
                    reason.code(),
                    reason.description()
            );
            case INVALID_CREDENTIALS -> new UnauthorizedException(
                    reason.code(),
                    reason.description()
            );
        };
    }

    public record LoginCommand(
            Email email,
            String password,
            String clientIp,
            String userAgent
    ) {
        public LoginCommand {
            Objects.requireNonNull(email, "email must not be null");
            Objects.requireNonNull(password, "password must not be null");
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
