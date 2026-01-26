package com.booking.domain.shared.exception;

/**
 * Exception thrown when a business rule is violated.
 *
 * <p>This exception maps to HTTP 422 Unprocessable Entity.
 *
 * <p>Use this for cases where the request is syntactically valid but violates
 * business rules (e.g., booking in the past, invalid state transition).
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new BusinessRuleViolationException("BOOKING_IN_PAST",
 *     "Cannot create a booking for a past time");
 * }</pre>
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessRuleViolationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
