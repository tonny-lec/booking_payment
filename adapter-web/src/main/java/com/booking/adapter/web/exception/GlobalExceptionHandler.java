package com.booking.adapter.web.exception;

import com.booking.domain.shared.exception.BusinessRuleViolationException;
import com.booking.domain.shared.exception.ConflictException;
import com.booking.domain.shared.exception.DomainException;
import com.booking.domain.shared.exception.ForbiddenException;
import com.booking.domain.shared.exception.ResourceNotFoundException;
import com.booking.domain.shared.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API endpoints.
 *
 * <p>This handler converts domain exceptions and other errors into RFC 7807
 * Problem Detail responses ({@code application/problem+json}).
 *
 * <p>Exception to HTTP status mapping:
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404 Not Found</li>
 *   <li>{@link BusinessRuleViolationException} → 422 Unprocessable Entity</li>
 *   <li>{@link ConflictException} → 409 Conflict</li>
 *   <li>{@link UnauthorizedException} → 401 Unauthorized</li>
 *   <li>{@link ForbiddenException} → 403 Forbidden</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request</li>
 *   <li>{@link ConstraintViolationException} → 400 Bad Request</li>
 *   <li>{@link Exception} (fallback) → 500 Internal Server Error</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807</a>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_TYPE_BASE_URI = "https://api.booking-payment.com/errors/";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String ERROR_CODE_KEY = "errorCode";
    private static final String TIMESTAMP_KEY = "timestamp";

    // =========================================================================
    // Domain Exception Handlers
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.NOT_FOUND,
                ex.getErrorCode(),
                "Resource Not Found",
                ex.getMessage(),
                request
        );

        if (ex.getResourceType() != null) {
            problem.setProperty("resourceType", ex.getResourceType());
        }
        if (ex.getResourceId() != null) {
            problem.setProperty("resourceId", ex.getResourceId());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, WebRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getErrorCode(),
                "Business Rule Violation",
                ex.getMessage(),
                request
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictException ex, WebRequest request) {

        log.warn("Conflict detected: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.CONFLICT,
                ex.getErrorCode(),
                "Conflict",
                ex.getMessage(),
                request
        );

        if (ex.getConflictingResourceId() != null) {
            problem.setProperty("conflictingResourceId", ex.getConflictingResourceId());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(
            UnauthorizedException ex, WebRequest request) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getErrorCode(),
                "Unauthorized",
                ex.getMessage(),
                request
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(
            ForbiddenException ex, WebRequest request) {

        log.warn("Access forbidden: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.FORBIDDEN,
                ex.getErrorCode(),
                "Forbidden",
                ex.getMessage(),
                request
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(
            DomainException ex, WebRequest request) {

        log.error("Unhandled domain exception: {}", ex.getMessage(), ex);

        ProblemDetail problem = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getErrorCode(),
                "Domain Error",
                ex.getMessage(),
                request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // =========================================================================
    // Validation Exception Handlers
    // =========================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Validation failed: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Validation Failed",
                "Request validation failed. Check the 'errors' field for details.",
                request
        );

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());

        ProblemDetail problem = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "Constraint Violation",
                "Request validation failed. Check the 'errors' field for details.",
                request
        );

        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            violations.put(path, violation.getMessage());
        });
        problem.setProperty("errors", violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // =========================================================================
    // Fallback Handler
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllUncaughtException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error occurred", ex);

        ProblemDetail problem = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Creates a ProblemDetail with standard fields.
     *
     * @param status    HTTP status code
     * @param errorCode application-specific error code
     * @param title     short summary of the error
     * @param detail    detailed explanation
     * @param request   the current web request
     * @return configured ProblemDetail instance
     */
    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String errorCode,
            String title,
            String detail,
            WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setType(URI.create(ERROR_TYPE_BASE_URI + errorCode.toLowerCase().replace("_", "-")));
        problem.setTitle(title);

        // Set instance to the request URI if available
        String requestUri = getRequestUri(request);
        if (requestUri != null) {
            problem.setInstance(URI.create(requestUri));
        }

        // Add custom properties
        problem.setProperty(ERROR_CODE_KEY, errorCode);
        problem.setProperty(TIMESTAMP_KEY, Instant.now().toString());

        // Add traceId from MDC if available
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null) {
            problem.setProperty(TRACE_ID_KEY, traceId);
        }

        return problem;
    }

    /**
     * Extracts the request URI from the WebRequest.
     */
    private String getRequestUri(WebRequest request) {
        try {
            // WebRequest doesn't directly expose URI, use description
            String description = request.getDescription(false);
            if (description != null && description.startsWith("uri=")) {
                return description.substring(4);
            }
        } catch (Exception e) {
            log.debug("Could not extract request URI", e);
        }
        return null;
    }
}
