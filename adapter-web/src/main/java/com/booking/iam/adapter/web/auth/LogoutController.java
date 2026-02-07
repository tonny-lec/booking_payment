package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.LogoutUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * IAM logout endpoint controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private final LogoutUseCase logoutUseCase;

    public LogoutController(LogoutUseCase logoutUseCase) {
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "logoutUseCase must not be null");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutUseCase.execute(new LogoutUseCase.LogoutCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }
}
