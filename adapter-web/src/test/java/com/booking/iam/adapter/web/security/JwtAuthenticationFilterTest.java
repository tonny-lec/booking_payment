package com.booking.iam.adapter.web.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String ISSUER = "booking-payment";
    private static final String AUDIENCE = "booking-payment-api";

    private KeyPair keyPair;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        filter = new JwtAuthenticationFilter(keyPair.getPublic(), ISSUER, AUDIENCE);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should authenticate request with valid bearer token")
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + createTokenSignedBy(keyPair, "access"));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user-123");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should leave context unauthenticated when token signature is invalid")
    void shouldNotAuthenticateWhenSignatureInvalid() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();

        request.addHeader("Authorization", "Bearer " + createTokenSignedBy(otherKeyPair, "access"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should leave context unauthenticated when token type is refresh")
    void shouldNotAuthenticateWhenTokenTypeIsRefresh() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + createTokenSignedBy(keyPair, "refresh"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should leave context unauthenticated when authorization header is missing")
    void shouldNotAuthenticateWhenHeaderMissing() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private String createTokenSignedBy(KeyPair signingKeyPair, String tokenType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("kid", "test-key")
                .issuer(ISSUER)
                .subject("user-123")
                .claim("aud", AUDIENCE)
                .claim("type", tokenType)
                .claim("roles", List.of("user"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .id(UUID.randomUUID().toString())
                .signWith(signingKeyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }
}
