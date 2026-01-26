package com.booking.adapter.web.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * HTTP metrics filter that captures RED metrics for all API requests.
 *
 * <p>RED Metrics captured:
 * <ul>
 *   <li><b>Rate</b>: {@code http_server_requests_total} - Total request count</li>
 *   <li><b>Errors</b>: {@code http_server_errors_total} - Error count (5xx responses)</li>
 *   <li><b>Duration</b>: {@code http_server_request_duration_seconds} - Request duration histogram</li>
 * </ul>
 *
 * <p>Labels:
 * <ul>
 *   <li>{@code method}: HTTP method (GET, POST, PUT, DELETE, etc.)</li>
 *   <li>{@code uri}: Request URI pattern</li>
 *   <li>{@code status}: HTTP status code</li>
 * </ul>
 *
 * @see <a href="https://www.weave.works/blog/the-red-method-key-metrics-for-microservices-architecture/">RED Method</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpMetricsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpMetricsFilter.class);

    private static final String METRIC_PREFIX = "http_server";
    private static final String REQUESTS_TOTAL = METRIC_PREFIX + "_requests_total";
    private static final String ERRORS_TOTAL = METRIC_PREFIX + "_errors_total";
    private static final String REQUEST_DURATION = METRIC_PREFIX + "_request_duration_seconds";

    private static final String TAG_METHOD = "method";
    private static final String TAG_URI = "uri";
    private static final String TAG_STATUS = "status";
    private static final String TAG_ERROR_TYPE = "error_type";

    private final MeterRegistry meterRegistry;

    public HttpMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip actuator endpoints
        String uri = request.getRequestURI();
        if (shouldSkip(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.nanoTime() - startTime;
            recordMetrics(request, response, duration);
        }
    }

    private boolean shouldSkip(String uri) {
        return uri.startsWith("/actuator") || 
               uri.equals("/health") || 
               uri.equals("/ready") || 
               uri.equals("/live");
    }

    private void recordMetrics(HttpServletRequest request, HttpServletResponse response, long durationNanos) {
        String method = request.getMethod();
        String uri = normalizeUri(request.getRequestURI());
        int status = response.getStatus();
        String statusStr = String.valueOf(status);

        // Rate: Total requests
        Counter.builder(REQUESTS_TOTAL)
                .description("Total HTTP requests")
                .tag(TAG_METHOD, method)
                .tag(TAG_URI, uri)
                .tag(TAG_STATUS, statusStr)
                .register(meterRegistry)
                .increment();

        // Errors: 4xx and 5xx responses
        if (status >= 400) {
            String errorType = categorizeError(status);
            Counter.builder(ERRORS_TOTAL)
                    .description("Total HTTP errors")
                    .tag(TAG_METHOD, method)
                    .tag(TAG_URI, uri)
                    .tag(TAG_STATUS, statusStr)
                    .tag(TAG_ERROR_TYPE, errorType)
                    .register(meterRegistry)
                    .increment();
        }

        // Duration: Request processing time
        Timer.builder(REQUEST_DURATION)
                .description("HTTP request duration in seconds")
                .tag(TAG_METHOD, method)
                .tag(TAG_URI, uri)
                .tag(TAG_STATUS, statusStr)
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(10),
                        Duration.ofMillis(25),
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(2500),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(10)
                )
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        if (log.isTraceEnabled()) {
            log.trace("Recorded metrics: method={}, uri={}, status={}, duration={}ms",
                    method, uri, status, durationNanos / 1_000_000);
        }
    }

    /**
     * Normalizes URI by replacing path variables with placeholders.
     * This prevents high cardinality from dynamic path segments.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code /bookings/123} → {@code /bookings/{id}}</li>
     *   <li>{@code /payments/abc-def} → {@code /payments/{id}}</li>
     * </ul>
     */
    private String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        
        // Replace UUID patterns
        uri = uri.replaceAll(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "{id}");
        
        // Replace numeric IDs
        uri = uri.replaceAll("/\\d+", "/{id}");
        
        return uri;
    }

    /**
     * Categorizes HTTP error status codes into error types.
     */
    private String categorizeError(int status) {
        return switch (status) {
            case 400 -> "bad_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 409 -> "conflict";
            case 422 -> "unprocessable_entity";
            case 429 -> "rate_limited";
            case 500 -> "internal_error";
            case 502 -> "bad_gateway";
            case 503 -> "service_unavailable";
            case 504 -> "gateway_timeout";
            default -> status >= 500 ? "server_error" : "client_error";
        };
    }
}
