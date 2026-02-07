package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.RefreshTokenUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * IAM token refresh endpoint controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class RefreshTokenController {

    private final RefreshTokenUseCase refreshTokenUseCase;

    public RefreshTokenController(RefreshTokenUseCase refreshTokenUseCase) {
        this.refreshTokenUseCase = Objects.requireNonNull(refreshTokenUseCase, "refreshTokenUseCase must not be null");
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenUseCase.TokenResponse response = refreshTokenUseCase.execute(
                new RefreshTokenUseCase.RefreshTokenCommand(request.refreshToken())
        );

        return ResponseEntity.ok(new TokenResponse(
                response.accessToken(),
                response.refreshToken(),
                response.tokenType(),
                response.expiresIn()
        ));
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
    }
}
