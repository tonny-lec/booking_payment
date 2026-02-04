package com.booking.iam.adapter.web.security;

import com.booking.iam.application.port.TokenGenerator;
import com.booking.iam.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JWT-based implementation of {@link TokenGenerator} using RS256.
 */
public class JwtTokenGenerator implements TokenGenerator {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final PrivateKey privateKey;
    private final String keyId;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    public JwtTokenGenerator(PrivateKey privateKey, String keyId, String issuer, String audience) {
        this(privateKey, keyId, issuer, audience, Clock.systemUTC());
    }

    public JwtTokenGenerator(PrivateKey privateKey, String keyId, String issuer, String audience, Clock clock) {
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey must not be null");
        this.keyId = Objects.requireNonNull(keyId, "keyId must not be null");
        this.issuer = Objects.requireNonNull(issuer, "issuer must not be null");
        this.audience = Objects.requireNonNull(audience, "audience must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public TokenPair generateTokens(User user, Duration accessTokenTtl, Duration refreshTokenTtl) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(accessTokenTtl, "accessTokenTtl must not be null");
        Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl must not be null");

        String accessToken = buildToken(user, TOKEN_TYPE_ACCESS, accessTokenTtl, true);
        String refreshToken = buildToken(user, TOKEN_TYPE_REFRESH, refreshTokenTtl, false);

        return new TokenPair(accessToken, refreshToken, accessTokenTtl.toSeconds());
    }

    private String buildToken(User user, String type, Duration ttl, boolean includeRoles) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(ttl);

        var builder = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("kid", keyId)
                .issuer(issuer)
                .subject(user.id().value().toString())
                .claim("aud", audience)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString());

        if (includeRoles) {
            builder.claim("roles", List.of("user"));
        }

        return builder.signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    /**
     * Parses a PKCS#8 PEM-encoded RSA private key.
     *
     * @param pem the PEM string (including header/footer)
     * @return the parsed {@link PrivateKey}
     */
    public static PrivateKey parsePrivateKey(String pem) {
        Objects.requireNonNull(pem, "pem must not be null");

        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(normalized);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse RSA private key", ex);
        }
    }
}
