package com.booking.iam.adapter.web.config;

import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.usecase.LogoutUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for IAM logout use case.
 */
@Configuration
public class IamLogoutUseCaseConfig {

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenRepository refreshTokenRepository) {
        return new LogoutUseCase(refreshTokenRepository);
    }
}
