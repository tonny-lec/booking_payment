package com.booking.domain.iam.event;

import com.booking.domain.iam.model.UserId;
import com.booking.domain.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when a user successfully logs in.
 *
 * <p>This event is published after successful authentication and can be used for:
 * <ul>
 *   <li>Audit logging - recording successful login attempts</li>
 *   <li>Security notifications - alerting users of new logins</li>
 *   <li>Analytics - tracking user activity patterns</li>
 * </ul>
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Client IP is stored in masked form (e.g., 192.168.1.***) for privacy</li>
 *   <li>User agent is stored as-is for debugging but should not contain PII</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * UserLoggedIn event = UserLoggedIn.create(
 *     userId,
 *     "192.168.1.100",
 *     "Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."
 * );
 * eventPublisher.publish(event);
 * }</pre>
 *
 * @see com.booking.domain.iam.model.User#authenticate
 */
public record UserLoggedIn(
        UUID eventId,
        UserId userId,
        Instant occurredAt,
        String clientIp,
        String userAgent
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public UserLoggedIn {
        Objects.requireNonNull(eventId, "Event ID must not be null");
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(occurredAt, "Occurred at must not be null");
        // clientIp and userAgent can be null/empty in some contexts
    }

    /**
     * Creates a new UserLoggedIn event with auto-generated event ID and timestamp.
     *
     * <p>The client IP will be automatically masked for privacy.
     *
     * @param userId the ID of the user who logged in
     * @param clientIp the client IP address (will be masked)
     * @param userAgent the user agent string from the request
     * @return a new UserLoggedIn event
     * @throws NullPointerException if userId is null
     */
    public static UserLoggedIn create(UserId userId, String clientIp, String userAgent) {
        return new UserLoggedIn(
                UUID.randomUUID(),
                userId,
                Instant.now(),
                maskIpAddress(clientIp),
                userAgent
        );
    }

    /**
     * Creates a UserLoggedIn event with all fields specified.
     *
     * <p>Use this factory method when reconstructing events from storage
     * or when you need to specify the event ID and timestamp explicitly.
     *
     * @param eventId the unique event identifier
     * @param userId the ID of the user who logged in
     * @param occurredAt when the login occurred
     * @param clientIp the client IP address (should already be masked)
     * @param userAgent the user agent string
     * @return a new UserLoggedIn event
     */
    public static UserLoggedIn of(
            UUID eventId,
            UserId userId,
            Instant occurredAt,
            String clientIp,
            String userAgent
    ) {
        return new UserLoggedIn(eventId, userId, occurredAt, clientIp, userAgent);
    }

    /**
     * Returns the aggregate ID (user ID) as a UUID.
     *
     * @return the user ID as UUID
     */
    @Override
    public UUID aggregateId() {
        return userId.value();
    }

    /**
     * Masks an IP address for privacy.
     *
     * <p>Examples:
     * <ul>
     *   <li>IPv4: 192.168.1.100 → 192.168.1.***</li>
     *   <li>IPv6: 2001:0db8:85a3:0000:0000:8a2e:0370:7334 → 2001:0db8:85a3:***</li>
     *   <li>IPv6 loopback: ::1 → ::***</li>
     * </ul>
     *
     * @param ip the IP address to mask
     * @return the masked IP address, or null if input is null
     */
    static String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        // IPv4: mask the last octet
        if (ip.contains(".")) {
            int lastDot = ip.lastIndexOf('.');
            if (lastDot > 0) {
                return ip.substring(0, lastDot + 1) + "***";
            }
        }

        // IPv6: mask after showing some prefix
        if (ip.contains(":")) {
            // Handle compressed notation (::)
            if (ip.startsWith("::")) {
                return "::***";
            }

            // Find position to cut: after third colon or at first ::
            int colonCount = 0;
            int cutPosition = -1;
            for (int i = 0; i < ip.length(); i++) {
                if (ip.charAt(i) == ':') {
                    colonCount++;
                    // Check for ::
                    if (i + 1 < ip.length() && ip.charAt(i + 1) == ':') {
                        cutPosition = i + 1;
                        break;
                    }
                    if (colonCount == 3) {
                        cutPosition = i;
                        break;
                    }
                }
            }

            if (cutPosition > 0) {
                return ip.substring(0, cutPosition + 1) + "***";
            }
        }

        // Unknown format: mask half
        int halfLength = ip.length() / 2;
        return ip.substring(0, halfLength) + "***";
    }

    @Override
    public String toString() {
        return "UserLoggedIn[eventId=" + eventId +
                ", userId=" + userId +
                ", occurredAt=" + occurredAt +
                ", clientIp=" + clientIp +
                "]";
    }
}
