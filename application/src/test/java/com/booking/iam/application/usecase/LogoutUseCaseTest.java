package com.booking.iam.application.usecase;

import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.security.TokenHasher;
import com.booking.iam.domain.model.RefreshToken;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LogoutUseCase")
class LogoutUseCaseTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final LogoutUseCase logoutUseCase = new LogoutUseCase(refreshTokenRepository);

    @Test
    @DisplayName("should revoke token when authenticated user owns the refresh token")
    void shouldRevokeWhenOwnedByAuthenticatedUser() {
        String rawToken = "refresh-token";
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        RefreshToken token = RefreshToken.create(
                UserId.fromString(userId),
                TokenHasher.hash(rawToken),
                Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawToken))).thenReturn(Optional.of(token));

        logoutUseCase.execute(new LogoutUseCase.LogoutCommand(rawToken, userId));

        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("should return without save when refresh token does not exist")
    void shouldIgnoreWhenTokenDoesNotExist() {
        String rawToken = "missing-refresh-token";
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawToken))).thenReturn(Optional.empty());

        logoutUseCase.execute(new LogoutUseCase.LogoutCommand(rawToken, userId));

        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should throw forbidden when refresh token belongs to different user")
    void shouldThrowForbiddenWhenUserMismatch() {
        String rawToken = "refresh-token";
        String tokenOwner = "550e8400-e29b-41d4-a716-446655440000";
        String requester = "660e8400-e29b-41d4-a716-446655440001";
        RefreshToken token = RefreshToken.create(
                UserId.fromString(tokenOwner),
                TokenHasher.hash(rawToken),
                Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawToken))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> logoutUseCase.execute(new LogoutUseCase.LogoutCommand(rawToken, requester)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not own");
        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
