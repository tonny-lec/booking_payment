package com.booking.shared.adapter.web.metrics;

import com.booking.shared.metrics.MetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Micrometer-based implementation of {@link MetricsPort}.
 *
 * <p>This adapter translates application-layer metric operations to
 * Micrometer registry calls, enabling export to various monitoring systems
 * (Prometheus, Datadog, etc.).
 *
 * <p>Features:
 * <ul>
 *   <li>Counter metrics for counting events</li>
 *   <li>Gauge metrics for current values</li>
 *   <li>Timer/Histogram metrics for durations</li>
 *   <li>Automatic tag conversion from Map to Micrometer Tags</li>
 * </ul>
 */
@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicReference<Double>> gaugeValues = new ConcurrentHashMap<>();

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void incrementCounter(String name, Map<String, String> tags) {
        incrementCounter(name, 1.0, tags);
    }

    @Override
    public void incrementCounter(String name, double amount, Map<String, String> tags) {
        Counter.builder(name)
                .tags(toTags(tags))
                .register(meterRegistry)
                .increment(amount);
    }

    @Override
    public void recordGauge(String name, double value, Map<String, String> tags) {
        // Create a unique key for this gauge+tags combination
        String key = name + tags.toString();
        
        AtomicReference<Double> gaugeRef = gaugeValues.computeIfAbsent(key, k -> {
            AtomicReference<Double> ref = new AtomicReference<>(value);
            io.micrometer.core.instrument.Gauge.builder(name, ref, AtomicReference::get)
                    .tags(toTags(tags))
                    .register(meterRegistry);
            return ref;
        });
        
        gaugeRef.set(value);
    }

    @Override
    public Timer startTimer() {
        return new MicrometerTimer();
    }

    @Override
    public void recordDuration(String name, Timer timer, Map<String, String> tags) {
        recordDuration(name, timer.elapsed(), tags);
    }

    @Override
    public void recordDuration(String name, Duration duration, Map<String, String> tags) {
        io.micrometer.core.instrument.Timer.builder(name)
                .tags(toTags(tags))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }

    @Override
    public void recordHistogram(String name, double value, Map<String, String> tags) {
        io.micrometer.core.instrument.DistributionSummary.builder(name)
                .tags(toTags(tags))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(value);
    }

    /**
     * Converts a Map of tags to Micrometer Tags.
     */
    private Tags toTags(Map<String, String> tagMap) {
        if (tagMap == null || tagMap.isEmpty()) {
            return Tags.empty();
        }
        return Tags.of(tagMap.entrySet().stream()
                .map(e -> io.micrometer.core.instrument.Tag.of(e.getKey(), e.getValue()))
                .toList());
    }

    /**
     * Timer implementation that tracks elapsed time.
     */
    private static class MicrometerTimer implements Timer {
        private final long startTimeNanos;

        MicrometerTimer() {
            this.startTimeNanos = System.nanoTime();
        }

        @Override
        public Duration elapsed() {
            return Duration.ofNanos(System.nanoTime() - startTimeNanos);
        }
    }
}
