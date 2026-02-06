package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.LoginUseCase;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LoginController")
class LoginControllerTest {

    private LoginUseCase loginUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        loginUseCase = mock(LoginUseCase.class);
        LoginController controller = new LoginController(loginUseCase);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorProperties()))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("should return 200 with token response on successful login")
    void shouldReturn200WhenLoginSucceeds() throws Exception {
        when(loginUseCase.execute(any())).thenReturn(
                new LoginUseCase.TokenResponse(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        900
                )
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("should return 401 when credentials are invalid")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        when(loginUseCase.execute(any())).thenThrow(
                new UnauthorizedException("invalid_credentials", "Invalid email or password")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("should return 423 when account is locked")
    void shouldReturn423WhenAccountIsLocked() throws Exception {
        when(loginUseCase.execute(any())).thenThrow(
                new ForbiddenException("account_locked", "Account is locked")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "locked@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423));
    }

    @Test
    @DisplayName("should return 403 when account is not active")
    void shouldReturn403WhenAccountIsNotActive() throws Exception {
        when(loginUseCase.execute(any())).thenThrow(
                new ForbiddenException("account_not_active", "Account is not active")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "inactive@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("should return 429 with Retry-After when rate limited")
    void shouldReturn429WhenRateLimited() throws Exception {
        when(loginUseCase.execute(any())).thenThrow(
                new ForbiddenException("rate_limited", "Too many login attempts")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
