package com.booking.iam.adapter.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Bearer JWT verification filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicKey publicKey;
    private final String issuer;
    private final String audience;

    public JwtAuthenticationFilter(PublicKey publicKey, String issuer, String audience) {
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey must not be null");
        this.issuer = Objects.requireNonNull(issuer, "issuer must not be null");
        this.audience = Objects.requireNonNull(audience, "audience must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!issuer.equals(claims.getIssuer()) || !audienceMatches(claims.get("aud"))) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(subject, null, toAuthorities(claims.get("roles")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parses a PEM-encoded RSA public key.
     */
    public static PublicKey parsePublicKey(String pem) {
        Objects.requireNonNull(pem, "pem must not be null");

        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(normalized);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse RSA public key", ex);
        }
    }

    private boolean audienceMatches(Object audienceClaim) {
        if (audienceClaim instanceof String aud) {
            return audience.equals(aud);
        }

        if (audienceClaim instanceof Collection<?> values) {
            for (Object value : values) {
                if (audience.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<GrantedAuthority> toAuthorities(Object rolesClaim) {
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object roleObj : roles) {
            if (!(roleObj instanceof String role) || role.isBlank()) {
                continue;
            }
            String normalized = role.toUpperCase(Locale.ROOT);
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }
            authorities.add(new SimpleGrantedAuthority(normalized));
        }
        return authorities;
    }
}
