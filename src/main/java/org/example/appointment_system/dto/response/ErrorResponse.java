package org.example.appointment_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format for all API errors.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    private final LocalDateTime timestamp;

    /**
     * HTTP status code.
     */
    private final int status;

    /**
     * Error code for programmatic handling.
     */
    private final int code;

    /**
     * Short error message.
     */
    private final String error;

    /**
     * Detailed error message.
     */
    private final String message;

    /**
     * Request path that caused the error.
     */
    private final String path;

    /**
     * Trace ID for log correlation (optional).
     */
    private final String traceId;

    /**
     * Validation errors (optional, for validation failures).
     */
    private final List<FieldError> fieldErrors;

    /**
     * Field-level validation error.
     */
    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
        private final Object rejectedValue;
    }

    /**
     * Create a simple error response.
     */
    public static ErrorResponse of(int status, int code, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .code(code)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create an error response with field errors.
     */
    public static ErrorResponse of(int status, int code, String error, String message,
                                   String path, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .code(code)
                .error(error)
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
}
