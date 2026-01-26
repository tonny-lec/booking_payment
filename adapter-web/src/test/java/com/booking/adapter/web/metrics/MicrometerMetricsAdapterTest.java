package com.booking.adapter.web.metrics;

import com.booking.application.shared.metrics.MetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerMetricsAdapter}.
 */
@DisplayName("MicrometerMetricsAdapter")
class MicrometerMetricsAdapterTest {

    private MeterRegistry meterRegistry;
    private MicrometerMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        adapter = new MicrometerMetricsAdapter(meterRegistry);
    }

    @Nested
    @DisplayName("Counter metrics")
    class CounterMetrics {

        @Test
        @DisplayName("should increment counter by 1")
        void shouldIncrementCounterByOne() {
            // When
            adapter.incrementCounter("booking_create_total", Map.of("status", "success"));

            // Then
            double count = meterRegistry.get("booking_create_total")
                    .tag("status", "success")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should increment counter by specified amount")
        void shouldIncrementCounterByAmount() {
            // When
            adapter.incrementCounter("booking_create_total", 5.0, Map.of("status", "success"));

            // Then
            double count = meterRegistry.get("booking_create_total")
                    .tag("status", "success")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(5.0);
        }

        @Test
        @DisplayName("should accumulate counter increments")
        void shouldAccumulateIncrements() {
            // When
            adapter.incrementCounter("booking_create_total", Map.of("status", "success"));
            adapter.incrementCounter("booking_create_total", Map.of("status", "success"));
            adapter.incrementCounter("booking_create_total", Map.of("status", "success"));

            // Then
            double count = meterRegistry.get("booking_create_total")
                    .tag("status", "success")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(3.0);
        }

        @Test
        @DisplayName("should handle counters with different tags separately")
        void shouldHandleDifferentTagsSeparately() {
            // When
            adapter.incrementCounter("booking_create_total", Map.of("status", "success"));
            adapter.incrementCounter("booking_create_total", Map.of("status", "failure"));

            // Then
            double successCount = meterRegistry.get("booking_create_total")
                    .tag("status", "success")
                    .counter()
                    .count();
            double failureCount = meterRegistry.get("booking_create_total")
                    .tag("status", "failure")
                    .counter()
                    .count();

            assertThat(successCount).isEqualTo(1.0);
            assertThat(failureCount).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should handle empty tags")
        void shouldHandleEmptyTags() {
            // When
            adapter.incrementCounter("simple_counter", Map.of());

            // Then
            double count = meterRegistry.get("simple_counter")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Gauge metrics")
    class GaugeMetrics {

        @Test
        @DisplayName("should record gauge value")
        void shouldRecordGaugeValue() {
            // When
            adapter.recordGauge("booking_active_count", 42.0, Map.of("status", "confirmed"));

            // Then
            double value = meterRegistry.get("booking_active_count")
                    .tag("status", "confirmed")
                    .gauge()
                    .value();
            assertThat(value).isEqualTo(42.0);
        }

        @Test
        @DisplayName("should update gauge value")
        void shouldUpdateGaugeValue() {
            // When
            adapter.recordGauge("booking_active_count", 10.0, Map.of());
            adapter.recordGauge("booking_active_count", 20.0, Map.of());
            adapter.recordGauge("booking_active_count", 15.0, Map.of());

            // Then - should have latest value
            double value = meterRegistry.get("booking_active_count")
                    .gauge()
                    .value();
            assertThat(value).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("Timer metrics")
    class TimerMetrics {

        @Test
        @DisplayName("should record duration from timer")
        void shouldRecordDurationFromTimer() throws InterruptedException {
            // Given
            MetricsPort.Timer timer = adapter.startTimer();
            Thread.sleep(10); // Ensure some time passes

            // When
            adapter.recordDuration("booking_create_duration_seconds", timer, Map.of("status", "success"));

            // Then
            long count = meterRegistry.get("booking_create_duration_seconds")
                    .tag("status", "success")
                    .timer()
                    .count();
            assertThat(count).isEqualTo(1);

            double totalTime = meterRegistry.get("booking_create_duration_seconds")
                    .tag("status", "success")
                    .timer()
                    .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS);
            assertThat(totalTime).isGreaterThan(0);
        }

        @Test
        @DisplayName("should record duration directly")
        void shouldRecordDurationDirectly() {
            // When
            adapter.recordDuration("payment_gateway_latency_seconds", 
                    Duration.ofMillis(250), Map.of("operation", "authorize"));

            // Then
            long count = meterRegistry.get("payment_gateway_latency_seconds")
                    .tag("operation", "authorize")
                    .timer()
                    .count();
            assertThat(count).isEqualTo(1);

            double totalTime = meterRegistry.get("payment_gateway_latency_seconds")
                    .tag("operation", "authorize")
                    .timer()
                    .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS);
            assertThat(totalTime).isCloseTo(250.0, org.assertj.core.data.Offset.offset(10.0));
        }

        @Test
        @DisplayName("timer should track elapsed time")
        void timerShouldTrackElapsedTime() throws InterruptedException {
            // Given
            MetricsPort.Timer timer = adapter.startTimer();
            Thread.sleep(50);

            // When
            Duration elapsed = timer.elapsed();

            // Then
            assertThat(elapsed.toMillis()).isGreaterThanOrEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Histogram metrics")
    class HistogramMetrics {

        @Test
        @DisplayName("should record histogram values")
        void shouldRecordHistogramValues() {
            // When
            adapter.recordHistogram("payment_amount", 100.0, Map.of("currency", "JPY"));
            adapter.recordHistogram("payment_amount", 200.0, Map.of("currency", "JPY"));
            adapter.recordHistogram("payment_amount", 150.0, Map.of("currency", "JPY"));

            // Then
            long count = meterRegistry.get("payment_amount")
                    .tag("currency", "JPY")
                    .summary()
                    .count();
            assertThat(count).isEqualTo(3);

            double total = meterRegistry.get("payment_amount")
                    .tag("currency", "JPY")
                    .summary()
                    .totalAmount();
            assertThat(total).isEqualTo(450.0);
        }
    }
}
