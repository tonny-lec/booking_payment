package com.booking.iam.adapter.web.security;

import com.booking.iam.adapter.web.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = SecurityConfigTest.TestConfig.class)
@WebAppConfiguration
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    private static final String ISSUER = "booking-payment";
    private static final String AUDIENCE = "booking-payment-api";
    private static final KeyPair KEY_PAIR = generateKeyPair();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("should permit login endpoint without authentication")
    void shouldPermitLoginWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should require authentication for logout endpoint")
    void shouldRequireAuthenticationForLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should allow logout with valid bearer token")
    void shouldAllowLogoutWithValidBearerToken() throws Exception {
        String token = createAccessToken();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setIssuer(ISSUER);
            properties.setAudience(AUDIENCE);
            return properties;
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtProperties properties) {
            return new JwtAuthenticationFilter(
                    KEY_PAIR.getPublic(),
                    properties.getIssuer(),
                    properties.getAudience()
            );
        }

        @Bean
        TestAuthController testAuthController() {
            return new TestAuthController();
        }
    }

    @RestController
    @RequestMapping("/api/v1/auth")
    static class TestAuthController {
        @PostMapping("/login")
        ResponseEntity<Void> login() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/refresh")
        ResponseEntity<Void> refresh() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/logout")
        ResponseEntity<Void> logout() {
            return ResponseEntity.noContent().build();
        }
    }

    private static String createAccessToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("kid", "test-key")
                .issuer(ISSUER)
                .subject("user-123")
                .claim("aud", AUDIENCE)
                .claim("type", "access")
                .claim("roles", List.of("user"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .id(UUID.randomUUID().toString())
                .signWith(KEY_PAIR.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm is not available", ex);
        }
    }
}
