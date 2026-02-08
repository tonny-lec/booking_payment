package com.booking.booking.domain.model;

/**
 * Enumeration representing booking lifecycle states.
 */
public enum BookingStatus {

    /**
     * Booking is created and waiting for payment confirmation.
     */
    PENDING("PENDING", true, true, true),

    /**
     * Booking is confirmed after successful payment.
     */
    CONFIRMED("CONFIRMED", true, true, false),

    /**
     * Booking is cancelled and cannot be changed anymore.
     */
    CANCELLED("CANCELLED", false, false, false);

    private final String code;
    private final boolean modifiable;
    private final boolean cancellable;
    private final boolean confirmable;

    BookingStatus(String code, boolean modifiable, boolean cancellable, boolean confirmable) {
        this.code = code;
        this.modifiable = modifiable;
        this.cancellable = cancellable;
        this.confirmable = confirmable;
    }

    /**
     * Returns uppercase code for persistence and APIs.
     *
     * @return status code
     */
    public String code() {
        return code;
    }

    /**
     * Returns whether the booking can be modified in this state.
     *
     * @return true when updates are allowed
     */
    public boolean isModifiable() {
        return modifiable;
    }

    /**
     * Returns whether the booking can be cancelled in this state.
     *
     * @return true when cancellation is allowed
     */
    public boolean isCancellable() {
        return cancellable;
    }

    /**
     * Returns whether the booking can transition to CONFIRMED.
     *
     * @return true when confirmation is allowed
     */
    public boolean isConfirmable() {
        return confirmable;
    }

    /**
     * Returns whether this state is terminal.
     *
     * @return true when no further transition is allowed
     */
    public boolean isTerminal() {
        return this == CANCELLED;
    }

    /**
     * Looks up a status by code.
     *
     * @param code status code
     * @return matching BookingStatus
     */
    public static BookingStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Status code must not be null");
        }

        for (BookingStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown booking status code: " + code);
    }
}
