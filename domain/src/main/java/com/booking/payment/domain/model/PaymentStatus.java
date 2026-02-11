package com.booking.payment.domain.model;

/**
 * Enumeration representing payment lifecycle states.
 */
public enum PaymentStatus {

    /**
     * Payment is created and waiting for gateway authorization.
     */
    PENDING("PENDING"),

    /**
     * Payment is authorized by gateway.
     */
    AUTHORIZED("AUTHORIZED"),

    /**
     * Payment amount is captured.
     */
    CAPTURED("CAPTURED"),

    /**
     * Payment is refunded or voided.
     */
    REFUNDED("REFUNDED"),

    /**
     * Payment failed.
     */
    FAILED("FAILED");

    private final String code;

    PaymentStatus(String code) {
        this.code = code;
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
     * Returns whether this state can authorize.
     *
     * @return true when authorize transition is allowed
     */
    public boolean isAuthorizable() {
        return this == PENDING;
    }

    /**
     * Returns whether this state can capture.
     *
     * @return true when capture transition is allowed
     */
    public boolean isCapturable() {
        return this == AUTHORIZED;
    }

    /**
     * Returns whether this state can refund/void.
     *
     * @return true when refund transition is allowed
     */
    public boolean isRefundable() {
        return this == AUTHORIZED || this == CAPTURED;
    }

    /**
     * Returns whether this state is terminal.
     *
     * @return true when no further transition is expected
     */
    public boolean isTerminal() {
        return this == REFUNDED || this == FAILED;
    }

    /**
     * Looks up status by code.
     *
     * @param code status code
     * @return matching status
     */
    public static PaymentStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Status code must not be null");
        }
        for (PaymentStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status code: " + code);
    }
}
