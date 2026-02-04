package com.booking.domain.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 *
 * <p>Domain events represent something significant that happened in the domain.
 * They are immutable and capture the state at the time the event occurred.
 *
 * <p>All domain events have:
 * <ul>
 *   <li>A unique event ID for idempotency and tracking</li>
 *   <li>An aggregate ID identifying the source aggregate</li>
 *   <li>A timestamp indicating when the event occurred</li>
 * </ul>
 *
 * <p>Events are named in past tense (e.g., UserLoggedIn, OrderPlaced) to indicate
 * that something has already happened.
 *
 * @see DomainEventPublisher
 */
public interface DomainEvent {

    /**
     * Returns the unique identifier for this event instance.
     *
     * <p>Used for:
     * <ul>
     *   <li>Idempotency - ensuring the same event is not processed twice</li>
     *   <li>Correlation - tracking events across systems</li>
     *   <li>Ordering - when combined with occurredAt</li>
     * </ul>
     *
     * @return the event ID as a UUID
     */
    UUID eventId();

    /**
     * Returns the identifier of the aggregate that raised this event.
     *
     * <p>This identifies which aggregate instance the event belongs to.
     * The type of aggregate can be inferred from the event type.
     *
     * @return the aggregate ID as a UUID, or null if not applicable
     */
    UUID aggregateId();

    /**
     * Returns the timestamp when this event occurred.
     *
     * <p>This represents the moment the event was created, not when it was
     * persisted or published.
     *
     * @return the occurrence timestamp
     */
    Instant occurredAt();

    /**
     * Returns the name of this event type.
     *
     * <p>Used for event routing and serialization. By default, returns
     * the simple class name.
     *
     * @return the event type name
     */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
