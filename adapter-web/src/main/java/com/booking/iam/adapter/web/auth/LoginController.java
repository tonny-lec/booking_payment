package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.LoginUseCase;
import com.booking.iam.domain.model.Email;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * IAM login endpoint controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final LoginUseCase loginUseCase;

    public LoginController(LoginUseCase loginUseCase) {
        this.loginUseCase = Objects.requireNonNull(loginUseCase, "loginUseCase must not be null");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        Email email = parseEmail(request.email());
        LoginUseCase.LoginCommand command = new LoginUseCase.LoginCommand(
                email,
                request.password(),
                resolveClientIp(servletRequest),
                userAgent
        );

        LoginUseCase.TokenResponse response = loginUseCase.execute(command);
        return ResponseEntity.ok(new TokenResponse(
                response.accessToken(),
                response.refreshToken(),
                response.tokenType(),
                response.expiresIn()
        ));
    }

    private Email parseEmail(String rawEmail) {
        try {
            return Email.of(rawEmail);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format", ex);
        }
    }

    private String resolveClientIp(HttpServletRequest servletRequest) {
        String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return servletRequest.getRemoteAddr();
    }

    public record LoginRequest(
            @NotBlank
            @jakarta.validation.constraints.Email
            @Size(max = com.booking.iam.domain.model.Email.MAX_LENGTH)
            String email,

            @NotBlank
            @Size(min = 8)
            String password
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
    }
}
