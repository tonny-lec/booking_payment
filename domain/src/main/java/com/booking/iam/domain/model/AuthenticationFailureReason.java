package com.booking.iam.domain.model;

/**
 * Enumeration of possible reasons for authentication failure.
 *
 * <p>These reasons are used to provide specific feedback about why
 * an authentication attempt failed, enabling appropriate responses
 * such as rate limiting, account lock notifications, etc.
 */
public enum AuthenticationFailureReason {

    /**
     * The provided credentials (email/password) are invalid.
     *
     * <p>This is the default reason when the password does not match.
     * For security, this same reason should be used when the user
     * does not exist (to prevent user enumeration).
     */
    INVALID_CREDENTIALS("invalid_credentials", "Invalid email or password"),

    /**
     * The account is locked due to too many failed login attempts
     * or administrative action.
     */
    ACCOUNT_LOCKED("account_locked", "Account is locked"),

    /**
     * The account is not in an active state (suspended, deactivated,
     * or pending verification).
     */
    ACCOUNT_NOT_ACTIVE("account_not_active", "Account is not active"),

    /**
     * Too many login attempts from this IP or for this account.
     * Rate limiting has been triggered.
     */
    RATE_LIMITED("rate_limited", "Too many login attempts");

    private final String code;
    private final String description;

    AuthenticationFailureReason(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the code for this failure reason.
     *
     * @return the failure reason code
     */
    public String code() {
        return code;
    }

    /**
     * Returns a human-readable description of this failure reason.
     *
     * @return the description
     */
    public String description() {
        return description;
    }
}
