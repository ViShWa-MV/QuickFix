package com.example.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Translates exceptions into consistent JSON responses with the right status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ApiError> handleSlotUnavailable(
            SlotUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(
            IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(
                HttpStatus.CONFLICT,
                "This booking was updated concurrently; please retry",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiError> handleOtp(
            OtpException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return build(
                HttpStatus.BAD_REQUEST,
                message.isBlank() ? "Validation failed" : message,
                request);
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<ApiError> handleCatalogDown(
            CatalogUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, HttpServletRequest request) {

        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
