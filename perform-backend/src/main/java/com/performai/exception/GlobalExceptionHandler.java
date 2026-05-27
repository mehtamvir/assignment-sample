/**
 * Perform AI Backend — Global Exception Handler
 *
 * Centralised error handling for all REST controllers.
 * Ensures every error response follows a consistent JSON structure.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepts exceptions thrown by any controller and maps them to
 * structured {@link ErrorResponse} JSON bodies with appropriate HTTP status codes.
 * Handled cases:
 *   JobNotFoundException            → 404 Not Found
 *   MethodArgumentNotValidException → 400 Bad Request
 *   All other Exception             → 500 Internal Server Error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Uniform error response shape returned for all error scenarios.
     *
     * @param status    HTTP status code (e.g. 404)
     * @param error     HTTP reason phrase (e.g. "Not Found")
     * @param message   human-readable description of the error
     * @param timestamp UTC time the error occurred
     */
    record ErrorResponse(int status, String error, String message, Instant timestamp) {}

    /**
     * Handles requests for non-existent analysis jobs.
     *
     * @param ex the thrown {@link JobNotFoundException}
     * @return {@code 404 Not Found} with error details
     */
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(JobNotFoundException ex) {
        log.warn("Job not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles bean validation failures (e.g. blank athlete name).
     * Collects all field-level violations into a single comma-separated message.
     *
     * @param ex the thrown {@link MethodArgumentNotValidException}
     * @return {@code 400 Bad Request} with a summary of all validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Catch-all handler for any unhandled exception.
     * Avoids leaking internal stack traces to the client.
     *
     * @param ex the unhandled exception
     * @return {@code 500 Internal Server Error} with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage()); // log full detail server-side only
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    /**
     * Builds a {@link ResponseEntity} wrapping an {@link ErrorResponse}.
     *
     * @param status  the HTTP status to return
     * @param message the error message to include
     * @return fully constructed error response
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message, Instant.now()));
    }
}
