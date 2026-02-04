package com.booking.iam.application.security;

import com.booking.iam.domain.model.HashedToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Utility for hashing refresh tokens using SHA-256.
 */
public final class TokenHasher {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private TokenHasher() {
    }

    /**
     * Hashes a raw token using SHA-256 and returns a HashedToken.
     *
     * @param rawToken the raw refresh token
     * @return hashed token value object
     */
    public static HashedToken hash(String rawToken) {
        Objects.requireNonNull(rawToken, "Raw token must not be null");
        return HashedToken.fromTrustedSource(sha256Hex(rawToken));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
