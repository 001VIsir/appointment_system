package org.example.appointment_system.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Handles all exceptions and returns standardized error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle BusinessException - custom business logic errors.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception at {}: {} - {}",
                request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(ex.getHttpStatus())
                .code(ex.getCode())
                .error(ex.getErrorCode().name())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_PARAMETER.getCode())
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.FieldError.builder()
                        .field(extractFieldName(violation))
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_PARAMETER.getCode())
                .error("CONSTRAINT_VIOLATION")
                .message("Constraint violation")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle missing request parameter.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_PARAMETER.getCode())
                .error("MISSING_PARAMETER")
                .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle method argument type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_PARAMETER.getCode())
                .error("TYPE_MISMATCH")
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle HTTP message not readable (invalid JSON).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Message not readable at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_PARAMETER.getCode())
                .error("INVALID_REQUEST_BODY")
                .message("Invalid request body format")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle HTTP method not supported.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not supported at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .code(ErrorCode.OPERATION_NOT_ALLOWED.getCode())
                .error("METHOD_NOT_ALLOWED")
                .message(String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle no handler found (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found at {}", request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .error("NOT_FOUND")
                .message("The requested resource was not found")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle bad credentials (wrong username/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .code(ErrorCode.INVALID_CREDENTIALS.getCode())
                .error(ErrorCode.INVALID_CREDENTIALS.name())
                .message(ErrorCode.INVALID_CREDENTIALS.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle disabled account.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Disabled account at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .code(ErrorCode.ACCOUNT_DISABLED.getCode())
                .error(ErrorCode.ACCOUNT_DISABLED.name())
                .message(ErrorCode.ACCOUNT_DISABLED.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .code(ErrorCode.UNAUTHORIZED.getCode())
                .error(ErrorCode.UNAUTHORIZED.name())
                .message(ErrorCode.UNAUTHORIZED.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle access denied (insufficient permissions).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .code(ErrorCode.ACCESS_DENIED.getCode())
                .error(ErrorCode.ACCESS_DENIED.name())
                .message(ErrorCode.ACCESS_DENIED.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle optimistic locking failure (concurrent modification).
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .code(ErrorCode.BOOKING_CONCURRENT_MODIFICATION.getCode())
                .error("CONCURRENT_MODIFICATION")
                .message("The resource was modified by another user. Please try again.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle data integrity violation (unique constraint, foreign key).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Data integrity violation";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage().toLowerCase();
            if (causeMessage.contains("unique") || causeMessage.contains("duplicate")) {
                message = "A record with this value already exists";
            } else if (causeMessage.contains("foreign key") || causeMessage.contains("constraint")) {
                message = "Referenced resource does not exist";
            }
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .code(ErrorCode.OPERATION_NOT_ALLOWED.getCode())
                .error("DATA_INTEGRITY_VIOLATION")
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle all other uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(ErrorCode.INTERNAL_ERROR.getCode())
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extract field name from constraint violation.
     */
    private String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        // Remove method prefix if present (e.g., "methodName.fieldName" -> "fieldName")
        int dotIndex = propertyPath.lastIndexOf('.');
        return dotIndex >= 0 ? propertyPath.substring(dotIndex + 1) : propertyPath;
    }
}
