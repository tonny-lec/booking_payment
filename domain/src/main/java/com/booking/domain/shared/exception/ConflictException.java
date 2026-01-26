package com.booking.domain.shared.exception;

/**
 * Exception thrown when a conflict is detected.
 *
 * <p>This exception maps to HTTP 409 Conflict.
 *
 * <p>Use cases include:
 * <ul>
 *   <li>Booking time range conflicts with existing bookings</li>
 *   <li>Optimistic lock version mismatch</li>
 *   <li>Idempotency key conflicts</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ConflictException("BOOKING_CONFLICT",
 *     "The requested time range conflicts with an existing booking",
 *     conflictingBookingId);
 * }</pre>
 */
public class ConflictException extends DomainException {

    private final String conflictingResourceId;

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
        this.conflictingResourceId = null;
    }

    public ConflictException(String errorCode, String message, String conflictingResourceId) {
        super(errorCode, message);
        this.conflictingResourceId = conflictingResourceId;
    }

    public ConflictException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.conflictingResourceId = null;
    }

    /**
     * Returns the ID of the resource that caused the conflict.
     *
     * @return the conflicting resource ID, or null if not applicable
     */
    public String getConflictingResourceId() {
        return conflictingResourceId;
    }
}
