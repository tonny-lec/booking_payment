package com.booking.iam.application.usecase;

import com.booking.iam.application.port.PasswordEncoder;
import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.port.TokenGenerator;
import com.booking.iam.application.port.UserRepository;
import com.booking.iam.application.security.TokenHasher;
import com.booking.iam.domain.model.AuthenticationFailureReason;
import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.HashedPassword;
import com.booking.iam.domain.model.RefreshToken;
import com.booking.iam.domain.model.User;
import com.booking.iam.domain.model.UserId;
import com.booking.iam.domain.model.UserStatus;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase")
class LoginUseCaseTest {

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "P@ssw0rd!";
    private static final String PASSWORD_HASH = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYlQBc/G2HHe";
    private static final Instant NOW = Instant.parse("2026-02-08T12:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenGenerator tokenGenerator;
    @Mock
    private PasswordEncoder passwordEncoder;

    private LoginUseCase useCase;
    private Email email;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        useCase = new LoginUseCase(
                userRepository,
                refreshTokenRepository,
                tokenGenerator,
                passwordEncoder,
                fixedClock
        );
        email = Email.of(EMAIL);
    }

    @Test
    @DisplayName("should issue token pair and persist refresh token on successful login")
    void shouldIssueTokensOnSuccessfulLogin() {
        User user = activeUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenGenerator.generateTokens(eq(user), eq(Duration.ofMinutes(15)), eq(Duration.ofDays(7))))
                .thenReturn(new TokenGenerator.TokenPair("access-token", "refresh-token", 900));

        LoginUseCase.TokenResponse response = useCase.execute(
                new LoginUseCase.LoginCommand(email, RAW_PASSWORD, "203.0.113.10", "JUnit")
        );

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);

        verify(userRepository).save(user);
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken persisted = tokenCaptor.getValue();
        assertThat(persisted.userId()).isEqualTo(user.id());
        assertThat(persisted.tokenHash()).isEqualTo(TokenHasher.hash("refresh-token"));
        assertThat(persisted.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("should throw unauthorized when user does not exist")
    void shouldThrowUnauthorizedWhenUserDoesNotExist() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new LoginUseCase.LoginCommand(email, RAW_PASSWORD, null, null)))
                .isInstanceOfSatisfying(UnauthorizedException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(AuthenticationFailureReason.INVALID_CREDENTIALS.code()));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, tokenGenerator, refreshTokenRepository);
    }

    @Test
    @DisplayName("should throw unauthorized on invalid credentials and persist failure state")
    void shouldThrowUnauthorizedOnInvalidCredentials() {
        User user = activeUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginUseCase.LoginCommand(email, RAW_PASSWORD, null, null)))
                .isInstanceOfSatisfying(UnauthorizedException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(AuthenticationFailureReason.INVALID_CREDENTIALS.code()));

        verify(userRepository).save(user);
        verifyNoInteractions(tokenGenerator, refreshTokenRepository);
    }

    @Test
    @DisplayName("should throw forbidden when account is suspended")
    void shouldThrowForbiddenWhenAccountIsSuspended() {
        User user = userWithStatus(UserStatus.SUSPENDED, null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(new LoginUseCase.LoginCommand(email, RAW_PASSWORD, null, null)))
                .isInstanceOfSatisfying(ForbiddenException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(AuthenticationFailureReason.ACCOUNT_NOT_ACTIVE.code()));

        verify(userRepository).save(user);
        verifyNoInteractions(tokenGenerator, refreshTokenRepository, passwordEncoder);
    }

    @Test
    @DisplayName("should throw forbidden when account is locked")
    void shouldThrowForbiddenWhenAccountIsLocked() {
        User user = userWithStatus(UserStatus.LOCKED, NOW.plus(Duration.ofMinutes(10)));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(new LoginUseCase.LoginCommand(email, RAW_PASSWORD, null, null)))
                .isInstanceOfSatisfying(ForbiddenException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(AuthenticationFailureReason.ACCOUNT_LOCKED.code()));

        verify(userRepository).save(user);
        verifyNoInteractions(tokenGenerator, refreshTokenRepository, passwordEncoder);
    }

    private User activeUser() {
        return userWithStatus(UserStatus.ACTIVE, null);
    }

    private User userWithStatus(UserStatus status, Instant lockedUntil) {
        return User.builder()
                .id(UserId.fromString(USER_ID))
                .email(email)
                .passwordHash(HashedPassword.of(PASSWORD_HASH))
                .status(status)
                .lockedUntil(lockedUntil)
                .createdAt(NOW.minus(Duration.ofDays(10)))
                .updatedAt(NOW.minus(Duration.ofDays(1)))
                .build();
    }
}
