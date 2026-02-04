package com.booking.iam.adapter.web.security;

import com.booking.iam.application.port.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * BCrypt-based implementation of {@link PasswordEncoder}.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private static final int DEFAULT_COST = 12;

    private final BCryptPasswordEncoder delegate;

    public BCryptPasswordEncoderAdapter() {
        this(new BCryptPasswordEncoder(DEFAULT_COST));
    }

    public BCryptPasswordEncoderAdapter(BCryptPasswordEncoder delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
