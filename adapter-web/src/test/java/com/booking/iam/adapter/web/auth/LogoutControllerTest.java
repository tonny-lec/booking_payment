package com.booking.iam.adapter.web.auth;

import com.booking.iam.application.usecase.LogoutUseCase;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("should return 204 and invoke use case")
    void shouldReturn204AndInvokeUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
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
    @DisplayName("should return 400 when refresh token is blank")
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
