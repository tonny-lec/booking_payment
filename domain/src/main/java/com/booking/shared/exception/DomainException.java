package com.booking.shared.exception;

/**
 * Base exception for all domain layer exceptions.
 *
 * <p>Domain exceptions represent business rule violations or domain-specific errors.
 * They should be caught and translated to appropriate HTTP responses by the adapter layer.
 *
 * <p>Subclasses should provide specific error codes and messages for different scenarios.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code for this exception.
     *
     * <p>Error codes follow the pattern: {@code DOMAIN_ERROR_TYPE}
     * (e.g., {@code BOOKING_CONFLICT}, {@code PAYMENT_FAILED})
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
