package com.booking.application.shared.metrics;

import java.time.Duration;
import java.util.Map;

/**
 * Port interface for recording application metrics.
 *
 * <p>This interface defines the contract for recording custom metrics from use cases
 * and domain services. Implementations are provided by the adapter layer (e.g., Micrometer).
 *
 * <p>Usage example:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class CreateBookingUseCase {
 *     private final MetricsPort metrics;
 *
 *     public Booking execute(CreateBookingCommand command) {
 *         Timer timer = metrics.startTimer();
 *         try {
 *             // ... create booking logic ...
 *             metrics.incrementCounter("booking_create_total", Map.of("status", "success"));
 *             return booking;
 *         } catch (Exception e) {
 *             metrics.incrementCounter("booking_create_total", Map.of("status", "failure"));
 *             throw e;
 *         } finally {
 *             metrics.recordDuration("booking_create_duration_seconds", timer, Map.of());
 *         }
 *     }
 * }
 * }</pre>
 */
public interface MetricsPort {

    /**
     * Increments a counter metric.
     *
     * @param name the metric name (e.g., "booking_create_total")
     * @param tags additional tags/labels for the metric
     */
    void incrementCounter(String name, Map<String, String> tags);

    /**
     * Increments a counter metric by a specific amount.
     *
     * @param name   the metric name
     * @param amount the amount to increment
     * @param tags   additional tags/labels for the metric
     */
    void incrementCounter(String name, double amount, Map<String, String> tags);

    /**
     * Records a gauge value.
     *
     * @param name  the metric name (e.g., "booking_active_count")
     * @param value the gauge value
     * @param tags  additional tags/labels for the metric
     */
    void recordGauge(String name, double value, Map<String, String> tags);

    /**
     * Starts a timer for measuring duration.
     *
     * @return a timer instance to be passed to {@link #recordDuration}
     */
    Timer startTimer();

    /**
     * Records the duration measured by a timer.
     *
     * @param name  the metric name (e.g., "booking_create_duration_seconds")
     * @param timer the timer started with {@link #startTimer}
     * @param tags  additional tags/labels for the metric
     */
    void recordDuration(String name, Timer timer, Map<String, String> tags);

    /**
     * Records a duration value directly.
     *
     * @param name     the metric name
     * @param duration the duration to record
     * @param tags     additional tags/labels for the metric
     */
    void recordDuration(String name, Duration duration, Map<String, String> tags);

    /**
     * Records a value in a histogram/distribution.
     *
     * @param name  the metric name
     * @param value the value to record
     * @param tags  additional tags/labels for the metric
     */
    void recordHistogram(String name, double value, Map<String, String> tags);

    /**
     * Timer interface for measuring durations.
     */
    interface Timer {
        /**
         * Returns the elapsed time since the timer was started.
         *
         * @return the elapsed duration
         */
        Duration elapsed();
    }
}
