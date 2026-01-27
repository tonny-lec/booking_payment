package com.booking.domain.iam.model;

/**
 * Enumeration representing the possible states of a user account.
 *
 * <p>The user status controls what actions a user can perform and
 * determines authentication/authorization behavior.
 *
 * <h2>State Transitions</h2>
 * <pre>
 *     PENDING_VERIFICATION
 *            │
 *            ▼ (email verified)
 *          ACTIVE ◄────────────────┐
 *            │                     │
 *     ┌──────┼──────┐              │
 *     │      │      │              │
 *     ▼      ▼      ▼              │
 *  LOCKED SUSPENDED DEACTIVATED   │
 *     │      │                     │
 *     └──────┴─────────────────────┘
 *         (admin action / unlock)
 * </pre>
 *
 * <h2>Business Rules</h2>
 * <ul>
 *   <li>Only ACTIVE users can authenticate</li>
 *   <li>LOCKED users must reset password or wait for unlock</li>
 *   <li>SUSPENDED users require admin intervention</li>
 *   <li>DEACTIVATED accounts can be reactivated by admin</li>
 * </ul>
 */
public enum UserStatus {

    /**
     * User has registered but email is not yet verified.
     *
     * <p>Users in this state cannot authenticate until they verify
     * their email address.
     */
    PENDING_VERIFICATION("pending_verification", false, "Email verification required"),

    /**
     * User account is active and can authenticate normally.
     *
     * <p>This is the normal operational state for a user.
     */
    ACTIVE("active", true, "Account is active"),

    /**
     * Account is temporarily locked due to security concerns.
     *
     * <p>Typically triggered by:
     * <ul>
     *   <li>Too many failed login attempts</li>
     *   <li>Suspicious activity detection</li>
     *   <li>Manual admin action</li>
     * </ul>
     *
     * <p>Users can unlock by:
     * <ul>
     *   <li>Waiting for automatic unlock (time-based)</li>
     *   <li>Resetting their password</li>
     *   <li>Contacting support</li>
     * </ul>
     */
    LOCKED("locked", false, "Account is locked"),

    /**
     * Account is suspended by administrator action.
     *
     * <p>Used for policy violations, payment issues, or other
     * administrative reasons. Requires explicit admin action to lift.
     */
    SUSPENDED("suspended", false, "Account is suspended"),

    /**
     * Account has been deactivated (soft delete).
     *
     * <p>User-initiated or admin-initiated account closure.
     * Can potentially be reactivated by admin if needed.
     */
    DEACTIVATED("deactivated", false, "Account is deactivated");

    private final String code;
    private final boolean canAuthenticate;
    private final String description;

    UserStatus(String code, boolean canAuthenticate, String description) {
        this.code = code;
        this.canAuthenticate = canAuthenticate;
        this.description = description;
    }

    /**
     * Returns the status code for persistence and API responses.
     *
     * @return the lowercase status code
     */
    public String code() {
        return code;
    }

    /**
     * Checks if users with this status can authenticate.
     *
     * @return true if authentication is allowed, false otherwise
     */
    public boolean canAuthenticate() {
        return canAuthenticate;
    }

    /**
     * Returns a human-readable description of this status.
     *
     * @return the status description
     */
    public String description() {
        return description;
    }

    /**
     * Checks if this status allows the user to perform normal operations.
     *
     * <p>Only ACTIVE users can perform normal operations like
     * creating bookings, making payments, etc.
     *
     * @return true if the user can perform normal operations
     */
    public boolean isOperational() {
        return this == ACTIVE;
    }

    /**
     * Checks if this status represents a restricted state.
     *
     * <p>Restricted states include LOCKED, SUSPENDED, and DEACTIVATED.
     *
     * @return true if the account is in a restricted state
     */
    public boolean isRestricted() {
        return this == LOCKED || this == SUSPENDED || this == DEACTIVATED;
    }

    /**
     * Checks if this status can transition to ACTIVE.
     *
     * <p>PENDING_VERIFICATION, LOCKED, and SUSPENDED can be activated.
     * DEACTIVATED requires special handling.
     *
     * @return true if transition to ACTIVE is possible
     */
    public boolean canTransitionToActive() {
        return this == PENDING_VERIFICATION || this == LOCKED || this == SUSPENDED;
    }

    /**
     * Finds a UserStatus by its code.
     *
     * @param code the status code
     * @return the matching UserStatus
     * @throws IllegalArgumentException if no matching status is found
     */
    public static UserStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Status code must not be null");
        }

        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown user status code: " + code);
    }
}
