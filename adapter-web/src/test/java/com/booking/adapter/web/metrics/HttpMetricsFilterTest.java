package com.booking.adapter.web.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link HttpMetricsFilter}.
 */
@DisplayName("HttpMetricsFilter")
class HttpMetricsFilterTest {

    private MeterRegistry meterRegistry;
    private HttpMetricsFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new HttpMetricsFilter(meterRegistry);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Nested
    @DisplayName("Request counting (Rate)")
    class RequestCounting {

        @Test
        @DisplayName("should count successful requests")
        void shouldCountSuccessfulRequests() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/bookings");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            
            double count = meterRegistry.get("http_server_requests_total")
                    .tag("method", "GET")
                    .tag("uri", "/bookings")
                    .tag("status", "200")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should count multiple requests")
        void shouldCountMultipleRequests() throws ServletException, IOException {
            // Given
            request.setMethod("POST");
            request.setRequestURI("/bookings");
            response.setStatus(201);

            // When - make 3 requests
            filter.doFilter(request, response, filterChain);
            filter.doFilter(request, response, filterChain);
            filter.doFilter(request, response, filterChain);

            // Then
            double count = meterRegistry.get("http_server_requests_total")
                    .tag("method", "POST")
                    .tag("uri", "/bookings")
                    .tag("status", "201")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("Error counting (Errors)")
    class ErrorCounting {

        @Test
        @DisplayName("should count client errors (4xx)")
        void shouldCountClientErrors() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/bookings/123");
            response.setStatus(404);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            double errorCount = meterRegistry.get("http_server_errors_total")
                    .tag("method", "GET")
                    .tag("status", "404")
                    .tag("error_type", "not_found")
                    .counter()
                    .count();
            assertThat(errorCount).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should count server errors (5xx)")
        void shouldCountServerErrors() throws ServletException, IOException {
            // Given
            request.setMethod("POST");
            request.setRequestURI("/payments");
            response.setStatus(500);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            double errorCount = meterRegistry.get("http_server_errors_total")
                    .tag("method", "POST")
                    .tag("status", "500")
                    .tag("error_type", "internal_error")
                    .counter()
                    .count();
            assertThat(errorCount).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should not count successful responses as errors")
        void shouldNotCountSuccessAsError() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/bookings");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then - no error counter should exist
            assertThat(meterRegistry.find("http_server_errors_total")
                    .tag("status", "200")
                    .counter())
                    .isNull();
        }

        @Test
        @DisplayName("should categorize conflict errors correctly")
        void shouldCategorizeConflictErrors() throws ServletException, IOException {
            // Given
            request.setMethod("POST");
            request.setRequestURI("/bookings");
            response.setStatus(409);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            double errorCount = meterRegistry.get("http_server_errors_total")
                    .tag("error_type", "conflict")
                    .counter()
                    .count();
            assertThat(errorCount).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Duration recording (Duration)")
    class DurationRecording {

        @Test
        @DisplayName("should record request duration")
        void shouldRecordRequestDuration() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/bookings");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            long count = meterRegistry.get("http_server_request_duration_seconds")
                    .tag("method", "GET")
                    .tag("uri", "/bookings")
                    .timer()
                    .count();
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("URI normalization")
    class UriNormalization {

        @Test
        @DisplayName("should normalize UUID path parameters")
        void shouldNormalizeUuidPaths() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/bookings/550e8400-e29b-41d4-a716-446655440000");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            double count = meterRegistry.get("http_server_requests_total")
                    .tag("uri", "/bookings/{id}")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should normalize numeric path parameters")
        void shouldNormalizeNumericPaths() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/bookings/12345");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            double count = meterRegistry.get("http_server_requests_total")
                    .tag("uri", "/bookings/{id}")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Actuator endpoint exclusion")
    class ActuatorExclusion {

        @Test
        @DisplayName("should skip actuator endpoints")
        void shouldSkipActuatorEndpoints() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/actuator/health");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then - no metrics should be recorded
            assertThat(meterRegistry.find("http_server_requests_total")
                    .tag("uri", "/actuator/health")
                    .counter())
                    .isNull();
        }

        @Test
        @DisplayName("should skip health endpoint")
        void shouldSkipHealthEndpoint() throws ServletException, IOException {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/health");
            response.setStatus(200);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(meterRegistry.find("http_server_requests_total")
                    .tag("uri", "/health")
                    .counter())
                    .isNull();
        }
    }
}
