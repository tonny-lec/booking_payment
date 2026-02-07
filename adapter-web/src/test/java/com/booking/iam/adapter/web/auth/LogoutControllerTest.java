package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.LogoutUseCase;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
import com.booking.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LogoutController")
class LogoutControllerTest {

    private LogoutUseCase logoutUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        logoutUseCase = mock(LogoutUseCase.class);
        LogoutController controller = new LogoutController(logoutUseCase);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorProperties()))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("should return 204 and invoke use case for authenticated user")
    void shouldReturn204AndInvokeUseCase() throws Exception {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(() -> userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).execute(any(LogoutUseCase.LogoutCommand.class));
    }

    @Test
    @DisplayName("should return 401 when principal is missing")
    void shouldReturn401WhenPrincipalMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("should return 403 when refresh token owner mismatches")
    void shouldReturn403WhenRefreshTokenOwnerMismatches() throws Exception {
        doThrow(new ForbiddenException("logout_token_owner_mismatch", "Authenticated user does not own the refresh token"))
                .when(logoutUseCase)
                .execute(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(() -> "550e8400-e29b-41d4-a716-446655440000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("should return 400 when refresh token is blank")
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(() -> "550e8400-e29b-41d4-a716-446655440000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
