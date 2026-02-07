package com.booking.iam.adapter.web.config;

import com.booking.iam.adapter.web.security.JwtTokenGenerator;
import com.booking.iam.application.port.PasswordEncoder;
import com.booking.iam.application.port.RefreshTokenRepository;
import com.booking.iam.application.port.TokenGenerator;
import com.booking.iam.application.port.UserRepository;
import com.booking.iam.application.usecase.LoginUseCase;
import com.booking.iam.application.usecase.RefreshTokenUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

/**
 * Spring wiring for IAM web-facing use cases.
 */
@Configuration
public class IamWebUseCaseConfig {

    private static final Logger log = LoggerFactory.getLogger(IamWebUseCaseConfig.class);

    @Bean
    public TokenGenerator tokenGenerator(JwtProperties jwtProperties) {
        PrivateKey privateKey = resolvePrivateKey(jwtProperties);
        return new JwtTokenGenerator(
                privateKey,
                jwtProperties.getKeyId(),
                jwtProperties.getIssuer(),
                jwtProperties.getAudience()
        );
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder
    ) {
        return new LoginUseCase(
                userRepository,
                refreshTokenRepository,
                tokenGenerator,
                passwordEncoder
        );
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenGenerator tokenGenerator
    ) {
        return new RefreshTokenUseCase(
                refreshTokenRepository,
                userRepository,
                tokenGenerator
        );
    }

    private PrivateKey resolvePrivateKey(JwtProperties jwtProperties) {
        String pem = jwtProperties.getPrivateKeyPem();
        if (pem != null && !pem.isBlank()) {
            return JwtTokenGenerator.parsePrivateKey(pem);
        }

        log.warn("app.security.jwt.private-key-pem is not set. Using ephemeral RSA key for local runtime.");
        return generateEphemeralPrivateKey();
    }

    private PrivateKey generateEphemeralPrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair().getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA key generation algorithm is not available", ex);
        }
    }
}
