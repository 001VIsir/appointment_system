package org.example.appointment_system.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessException.
 */
class BusinessExceptionTest {

    @Test
    @DisplayName("Should create exception with error code only")
    void createWithErrorCodeOnly() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatus());
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        assertNull(ex.getDetail());
    }

    @Test
    @DisplayName("Should create exception with error code and custom message")
    void createWithErrorCodeAndMessage() {
        String customMessage = "User with ID 123 not found";
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND, customMessage);

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        assertEquals(customMessage, ex.getMessage());
        assertEquals(customMessage, ex.getDetail());
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should create exception with error code and cause")
    void createWithErrorCodeAndCause() {
        Throwable cause = new RuntimeException("Database error");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, cause);

        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        assertEquals(cause, ex.getCause());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should create exception with error code, message, and cause")
    void createWithAllParameters() {
        String customMessage = "Failed to process request";
        Throwable cause = new RuntimeException("Connection timeout");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, customMessage, cause);

        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        assertEquals(customMessage, ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Should create notFound exception using factory method")
    void createNotFoundFactory() {
        BusinessException ex = BusinessException.notFound("User", 123L);

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("User"));
        assertTrue(ex.getMessage().contains("123"));
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should create invalidParameter exception using factory method")
    void createInvalidParameterFactory() {
        BusinessException ex = BusinessException.invalidParameter("email", "must be a valid email");

        assertEquals(ErrorCode.INVALID_PARAMETER, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("email"));
        assertTrue(ex.getMessage().contains("must be a valid email"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should create unauthorized exception using factory method")
    void createUnauthorizedFactory() {
        BusinessException ex = BusinessException.unauthorized();

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should create accessDenied exception using factory method")
    void createAccessDeniedFactory() {
        BusinessException ex = BusinessException.accessDenied();

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should verify all error codes have correct HTTP status")
    void verifyErrorCodesHttpStatus() {
        assertEquals(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER.getHttpStatus());
        assertEquals(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus());
        assertEquals(HttpStatus.FORBIDDEN, ErrorCode.OPERATION_NOT_ALLOWED.getHttpStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getHttpStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS.getHttpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getHttpStatus());
        assertEquals(HttpStatus.CONFLICT, ErrorCode.USERNAME_ALREADY_EXISTS.getHttpStatus());
    }

    @Test
    @DisplayName("Should verify error codes have unique values")
    void verifyErrorCodeUniqueness() {
        java.util.Set<Integer> codes = new java.util.HashSet<>();
        for (ErrorCode code : ErrorCode.values()) {
            assertTrue(codes.add(code.getCode()),
                    "Duplicate error code: " + code.getCode() + " for " + code.name());
        }
    }
}
