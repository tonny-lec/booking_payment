package com.booking.iam.domain.event;

import com.booking.iam.domain.model.UserId;
import com.booking.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when a login attempt fails.
 *
 * <p>This event is published when authentication fails due to invalid credentials,
 * account lock, or rate limiting. It is intended for audit logging and security
 * monitoring.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Email is stored in masked form (e.g., u***@example.com)</li>
 *   <li>Client IP is stored in masked form for privacy</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * LoginFailed event = LoginFailed.create(
 *     userId,
 *     "user@example.com",
 *     LoginFailed.FailureReason.INVALID_CREDENTIALS,
 *     "192.168.1.100"
 * );
 * eventPublisher.publish(event);
 * }</pre>
 */
public record LoginFailed(
        UUID eventId,
        UserId userId,
        Instant occurredAt,
        String email,
        FailureReason reason,
        String clientIp
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public LoginFailed {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(occurredAt, "Occurred at must not be null");
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(reason, "Failure reason must not be null");
        // userId and clientIp can be null when the user is unknown
    }

    /**
     * Failure reason for the login attempt.
     */
    public enum FailureReason {
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED,
        RATE_LIMITED
    }

    /**
     * Creates a new LoginFailed event with auto-generated event ID and timestamp.
     *
     * <p>Email and client IP will be masked for privacy.
     *
     * @param userId the ID of the user if known (nullable)
     * @param email the email address used for login (will be masked)
     * @param reason the failure reason
     * @param clientIp the client IP address (will be masked)
     * @return a new LoginFailed event
     */
    public static LoginFailed create(UserId userId, String email, FailureReason reason, String clientIp) {
        return new LoginFailed(
                UUID.randomUUID(),
                userId,
                Instant.now(),
                maskEmail(email),
                reason,
                UserLoggedIn.maskIpAddress(clientIp)
        );
    }

    /**
     * Creates a LoginFailed event with all fields specified.
     *
     * <p>Use this factory method when reconstructing events from storage
     * or when you need to specify the event ID and timestamp explicitly.
     *
     * @param eventId the unique event identifier
     * @param userId the user ID if known (nullable)
     * @param occurredAt when the login failure occurred
     * @param email the masked email address
     * @param reason the failure reason
     * @param clientIp the masked client IP address
     * @return a new LoginFailed event
     */
    public static LoginFailed of(
            UUID eventId,
            UserId userId,
            Instant occurredAt,
            String email,
            FailureReason reason,
            String clientIp
    ) {
        return new LoginFailed(eventId, userId, occurredAt, email, reason, clientIp);
    }

    /**
     * Returns the aggregate ID (user ID) as a UUID, or null if unknown.
     *
     * @return the user ID as UUID, or null if not available
     */
    @Override
    public UUID aggregateId() {
        return userId == null ? null : userId.value();
    }

    /**
     * Masks an email address for privacy.
     *
     * <p>Example: user@example.com → u***@example.com
     *
     * @param email the email address to mask
     * @return the masked email, or null if input is null
     */
    static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return maskFallback(email);
        }

        String domain = email.substring(atIndex + 1);
        String firstChar = email.substring(0, 1);
        return firstChar + "***@" + domain;
    }

    private static String maskFallback(String value) {
        int halfLength = Math.max(1, value.length() / 2);
        return value.substring(0, halfLength) + "***";
    }

    @Override
    public String toString() {
        return "LoginFailed[eventId=" + eventId +
                ", userId=" + userId +
                ", occurredAt=" + occurredAt +
                ", reason=" + reason +
                ", email=" + email +
                "]";
    }
}
