package com.booking.shared.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * <p>This exception maps to HTTP 404 Not Found.
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ResourceNotFoundException("BOOKING_NOT_FOUND",
 *     "Booking with ID " + bookingId + " not found");
 * }</pre>
 */
public class ResourceNotFoundException extends DomainException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public ResourceNotFoundException(String resourceType, String resourceId, String message) {
        super(resourceType.toUpperCase() + "_NOT_FOUND", message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Returns the type of resource that was not found.
     *
     * @return the resource type (e.g., "Booking", "Payment", "User")
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Returns the ID of the resource that was not found.
     *
     * @return the resource ID
     */
    public String getResourceId() {
        return resourceId;
    }
}
