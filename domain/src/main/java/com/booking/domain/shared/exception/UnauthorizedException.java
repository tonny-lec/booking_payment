package com.booking.domain.shared.exception;

/**
 * Exception thrown when authentication fails or credentials are invalid.
 *
 * <p>This exception maps to HTTP 401 Unauthorized.
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new UnauthorizedException("INVALID_CREDENTIALS",
 *     "Invalid email or password");
 * }</pre>
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }

    public UnauthorizedException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
