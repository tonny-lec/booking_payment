package com.booking.domain.iam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AuthenticationFailureReason}.
 */
@DisplayName("AuthenticationFailureReason")
class AuthenticationFailureReasonTest {

    @ParameterizedTest
    @EnumSource(AuthenticationFailureReason.class)
    @DisplayName("all values should have non-null code")
    void allValuesShouldHaveNonNullCode(AuthenticationFailureReason reason) {
        assertThat(reason.code()).isNotNull().isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(AuthenticationFailureReason.class)
    @DisplayName("all values should have non-null description")
    void allValuesShouldHaveNonNullDescription(AuthenticationFailureReason reason) {
        assertThat(reason.description()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("INVALID_CREDENTIALS should have correct code")
    void invalidCredentialsShouldHaveCorrectCode() {
        assertThat(AuthenticationFailureReason.INVALID_CREDENTIALS.code())
                .isEqualTo("invalid_credentials");
    }

    @Test
    @DisplayName("ACCOUNT_LOCKED should have correct code")
    void accountLockedShouldHaveCorrectCode() {
        assertThat(AuthenticationFailureReason.ACCOUNT_LOCKED.code())
                .isEqualTo("account_locked");
    }

    @Test
    @DisplayName("ACCOUNT_NOT_ACTIVE should have correct code")
    void accountNotActiveShouldHaveCorrectCode() {
        assertThat(AuthenticationFailureReason.ACCOUNT_NOT_ACTIVE.code())
                .isEqualTo("account_not_active");
    }

    @Test
    @DisplayName("RATE_LIMITED should have correct code")
    void rateLimitedShouldHaveCorrectCode() {
        assertThat(AuthenticationFailureReason.RATE_LIMITED.code())
                .isEqualTo("rate_limited");
    }

    @ParameterizedTest
    @EnumSource(AuthenticationFailureReason.class)
    @DisplayName("all codes should be lowercase with underscores")
    void allCodesShouldBeLowercaseWithUnderscores(AuthenticationFailureReason reason) {
        assertThat(reason.code())
                .matches("^[a-z_]+$");
    }
}
