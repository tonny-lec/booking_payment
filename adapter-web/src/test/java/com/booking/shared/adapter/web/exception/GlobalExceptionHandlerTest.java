package com.booking.shared.adapter.web.exception;

import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.exception.BusinessRuleViolationException;
import com.booking.shared.exception.ConflictException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;
import com.booking.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Tests verify that domain exceptions are correctly mapped to RFC 7807
 * ProblemDetail responses with appropriate HTTP status codes.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(new ApiErrorProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/bookings/123");
        webRequest = new ServletWebRequest(request);
    }

    @Nested
    @DisplayName("ResourceNotFoundException handling")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("should return 404 Not Found with correct ProblemDetail")
        void shouldReturn404NotFound() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Booking", "123", "Booking with ID 123 not found");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(404);
            assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
            assertThat(problem.getDetail()).contains("Booking with ID 123 not found");
            assertThat(problem.getType().toString()).contains("booking-not-found");
            assertThat(problem.getProperties()).containsEntry("resourceType", "Booking");
            assertThat(problem.getProperties()).containsEntry("resourceId", "123");
            assertThat(problem.getProperties()).containsKey("errorCode");
            assertThat(problem.getProperties()).containsKey("timestamp");
        }

        @Test
        @DisplayName("should handle exception without resource details")
        void shouldHandleExceptionWithoutResourceDetails() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "RESOURCE_NOT_FOUND", "Resource not found");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getProperties()).doesNotContainKey("resourceType");
            assertThat(problem.getProperties()).doesNotContainKey("resourceId");
        }
    }

    @Nested
    @DisplayName("BusinessRuleViolationException handling")
    class BusinessRuleViolationExceptionTests {

        @Test
        @DisplayName("should return 422 Unprocessable Entity")
        void shouldReturn422UnprocessableEntity() {
            // Given
            BusinessRuleViolationException ex = new BusinessRuleViolationException(
                    "BOOKING_IN_PAST", "Cannot create a booking for a past time");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleBusinessRuleViolation(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(422);
            assertThat(problem.getTitle()).isEqualTo("Business Rule Violation");
            assertThat(problem.getDetail()).isEqualTo("Cannot create a booking for a past time");
            assertThat(problem.getProperties()).containsEntry("errorCode", "BOOKING_IN_PAST");
        }
    }

    @Nested
    @DisplayName("ConflictException handling")
    class ConflictExceptionTests {

        @Test
        @DisplayName("should return 409 Conflict with conflicting resource ID")
        void shouldReturn409ConflictWithResourceId() {
            // Given
            ConflictException ex = new ConflictException(
                    "BOOKING_CONFLICT",
                    "The requested time range conflicts with an existing booking",
                    "456");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleConflict(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(409);
            assertThat(problem.getTitle()).isEqualTo("Conflict");
            assertThat(problem.getProperties()).containsEntry("conflictingResourceId", "456");
            assertThat(problem.getProperties()).containsEntry("errorCode", "BOOKING_CONFLICT");
        }

        @Test
        @DisplayName("should return 409 Conflict without conflicting resource ID")
        void shouldReturn409ConflictWithoutResourceId() {
            // Given
            ConflictException ex = new ConflictException(
                    "VERSION_MISMATCH", "Resource version mismatch");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleConflict(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getProperties()).doesNotContainKey("conflictingResourceId");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException handling")
    class UnauthorizedExceptionTests {

        @Test
        @DisplayName("should return 401 Unauthorized")
        void shouldReturn401Unauthorized() {
            // Given
            UnauthorizedException ex = new UnauthorizedException(
                    "INVALID_CREDENTIALS", "Invalid email or password");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleUnauthorized(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(401);
            assertThat(problem.getTitle()).isEqualTo("Unauthorized");
            assertThat(problem.getDetail()).isEqualTo("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("ForbiddenException handling")
    class ForbiddenExceptionTests {

        @Test
        @DisplayName("should return 403 Forbidden")
        void shouldReturn403Forbidden() {
            // Given
            ForbiddenException ex = new ForbiddenException(
                    "ACCESS_DENIED", "You do not have permission to cancel this booking");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleForbidden(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(403);
            assertThat(problem.getTitle()).isEqualTo("Forbidden");
            assertThat(problem.getDetail()).isEqualTo("You do not have permission to cancel this booking");
        }

        @Test
        @DisplayName("should return 423 Locked for account_locked")
        void shouldReturn423LockedForAccountLocked() {
            // Given
            ForbiddenException ex = new ForbiddenException(
                    "account_locked", "Account is locked");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleForbidden(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(423);
            assertThat(problem.getTitle()).isEqualTo("Locked");
        }

        @Test
        @DisplayName("should return 429 Too Many Requests for rate_limited")
        void shouldReturn429TooManyRequestsForRateLimited() {
            // Given
            ForbiddenException ex = new ForbiddenException(
                    "rate_limited", "Too many login attempts");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleForbidden(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(429);
            assertThat(problem.getTitle()).isEqualTo("Too Many Requests");
        }
    }

    @Nested
    @DisplayName("Generic exception handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 Internal Server Error for unexpected exceptions")
        void shouldReturn500ForUnexpectedExceptions() {
            // Given
            Exception ex = new RuntimeException("Unexpected database error");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleAllUncaughtException(ex, webRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getStatus()).isEqualTo(500);
            assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
            // Should NOT expose internal error details
            assertThat(problem.getDetail()).doesNotContain("database");
            assertThat(problem.getDetail()).contains("unexpected error");
        }
    }

    @Nested
    @DisplayName("ProblemDetail structure")
    class ProblemDetailStructureTests {

        @Test
        @DisplayName("should include RFC 7807 required fields")
        void shouldIncludeRequiredFields() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "BOOKING_NOT_FOUND", "Booking not found");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(ex, webRequest);

            // Then
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getType()).isNotNull();
            assertThat(problem.getTitle()).isNotNull();
            assertThat(problem.getStatus()).isNotNull();
            assertThat(problem.getDetail()).isNotNull();
        }

        @Test
        @DisplayName("should include custom properties")
        void shouldIncludeCustomProperties() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "BOOKING_NOT_FOUND", "Booking not found");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(ex, webRequest);

            // Then
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getProperties())
                    .containsKey("errorCode")
                    .containsKey("timestamp");
        }

        @Test
        @DisplayName("should set type URI correctly")
        void shouldSetTypeUriCorrectly() {
            // Given
            ConflictException ex = new ConflictException(
                    "BOOKING_CONFLICT", "Conflict detected");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleConflict(ex, webRequest);

            // Then
            ProblemDetail problem = response.getBody();
            assertThat(problem).isNotNull();
            assertThat(problem.getType().toString())
                    .startsWith("https://api.booking-payment.com/errors/")
                    .contains("booking-conflict");
        }
    }
}
