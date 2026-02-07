package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.RefreshTokenUseCase;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RefreshTokenController")
class RefreshTokenControllerTest {

    private RefreshTokenUseCase refreshTokenUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        refreshTokenUseCase = mock(RefreshTokenUseCase.class);
        RefreshTokenController controller = new RefreshTokenController(refreshTokenUseCase);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorProperties()))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("should return 200 with token response on successful refresh")
    void shouldReturn200WhenRefreshSucceeds() throws Exception {
        when(refreshTokenUseCase.execute(any())).thenReturn(
                new RefreshTokenUseCase.TokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        "Bearer",
                        900
                )
        );

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("should return 401 when refresh token is invalid")
    void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        when(refreshTokenUseCase.execute(any())).thenThrow(
                new UnauthorizedException("invalid_credentials", "Invalid refresh token")
        );

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-refresh-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("should return 400 when refresh token is blank")
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
