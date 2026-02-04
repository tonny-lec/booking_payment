package com.booking.shared.exception;

/**
 * Exception thrown when access to a resource is forbidden.
 *
 * <p>This exception maps to HTTP 403 Forbidden.
 *
 * <p>Use this when the user is authenticated but lacks permission
 * to perform the requested operation.
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ForbiddenException("ACCESS_DENIED",
 *     "You do not have permission to cancel this booking");
 * }</pre>
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ForbiddenException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
