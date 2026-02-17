package org.example.appointment_system.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.example.appointment_system.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("Should handle BusinessException")
    void handleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), response.getBody().getCode());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void handleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        // Use correct FieldError constructor: (objectName, field, defaultMessage)
        List<FieldError> fieldErrors = List.of(
                new FieldError("object", "username", "must not be blank"),
                new FieldError("object", "email", "must be valid")
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), response.getBody().getCode());
        assertNotNull(response.getBody().getFieldErrors());
        assertEquals(2, response.getBody().getFieldErrors().size());
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException")
    void handleConstraintViolationException() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be valid");
        when(violation.getInvalidValue()).thenReturn("invalid-email");
        violations.add(violation);

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("CONSTRAINT_VIOLATION", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException")
    void handleMissingParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException(
                "userId", "Long");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParameterException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("userId"));
    }

    @Test
    @DisplayName("Should handle BadCredentialsException")
    void handleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INVALID_CREDENTIALS.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("Should handle DisabledException")
    void handleDisabledException() {
        DisabledException ex = new DisabledException("Account disabled");

        ResponseEntity<ErrorResponse> response = handler.handleDisabledException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.ACCOUNT_DISABLED.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("Should handle AccessDeniedException")
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.ACCESS_DENIED.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("Should handle OptimisticLockingFailureException")
    void handleOptimisticLockingFailureException() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Version mismatch");

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailureException(ex, request);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("CONCURRENT_MODIFICATION", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException")
    void handleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("DATA_INTEGRITY_VIOLATION", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException")
    void handleMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupportedException(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGenericException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException")
    void handleMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Invalid JSON", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadableException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST_BODY", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle NoHandlerFoundException")
    void handleNoHandlerFoundException() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/not-found", null);

        ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle BusinessException with cause")
    void handleBusinessExceptionWithCause() {
        Throwable cause = new RuntimeException("Database error");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "Processing failed", cause);

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException with unique constraint")
    void handleDataIntegrityViolationWithUniqueConstraint() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry 'test' for key 'uk_username'");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
