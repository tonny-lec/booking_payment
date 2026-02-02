package com.booking.domain.iam.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of an authentication attempt.
 *
 * <p>This is a value object that encapsulates the outcome of calling
 * {@link User#authenticate}. It provides a type-safe way to handle
 * both successful and failed authentication attempts.
 *
 * <p>Usage:
 * <pre>{@code
 * AuthenticationResult result = user.authenticate(password, matcher);
 *
 * if (result.isSuccess()) {
 *     // Generate tokens, create session, etc.
 * } else {
 *     AuthenticationFailureReason reason = result.failureReason().get();
 *     // Handle failure based on reason
 * }
 * }</pre>
 */
public final class AuthenticationResult {

    private final boolean success;
    private final AuthenticationFailureReason failureReason;

    private AuthenticationResult(boolean success, AuthenticationFailureReason failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    /**
     * Creates a successful authentication result.
     *
     * @return a successful result
     */
    public static AuthenticationResult success() {
        return new AuthenticationResult(true, null);
    }

    /**
     * Creates a failed authentication result with the specified reason.
     *
     * @param reason the reason for failure
     * @return a failed result
     * @throws NullPointerException if reason is null
     */
    public static AuthenticationResult failure(AuthenticationFailureReason reason) {
        Objects.requireNonNull(reason, "Failure reason must not be null");
        return new AuthenticationResult(false, reason);
    }

    /**
     * Checks if the authentication was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if the authentication failed.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Returns the failure reason if authentication failed.
     *
     * @return an Optional containing the failure reason, or empty if successful
     */
    public Optional<AuthenticationFailureReason> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationResult that = (AuthenticationResult) o;
        return success == that.success && failureReason == that.failureReason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, failureReason);
    }

    @Override
    public String toString() {
        if (success) {
            return "AuthenticationResult[success]";
        }
        return "AuthenticationResult[failure: " + failureReason + "]";
    }
}
