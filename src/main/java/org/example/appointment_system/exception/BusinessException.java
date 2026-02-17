package org.example.appointment_system.exception;

import lombok.Getter;

/**
 * Custom business exception that carries an error code.
 * Used throughout the application for business logic errors.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    /**
     * Create a business exception with an error code.
     *
     * @param errorCode the error code
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * Create a business exception with an error code and custom message.
     *
     * @param errorCode the error code
     * @param message   custom error message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = message;
    }

    /**
     * Create a business exception with an error code and cause.
     *
     * @param errorCode the error code
     * @param cause     the cause of the exception
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * Create a business exception with an error code, custom message, and cause.
     *
     * @param errorCode the error code
     * @param message   custom error message
     * @param cause     the cause of the exception
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detail = message;
    }

    /**
     * Get the HTTP status code for this exception.
     *
     * @return HTTP status code
     */
    public int getHttpStatus() {
        return errorCode.getHttpStatus().value();
    }

    /**
     * Get the error code value.
     *
     * @return error code integer value
     */
    public int getCode() {
        return errorCode.getCode();
    }

    /**
     * Static factory method to create a resource not found exception.
     *
     * @param resourceName name of the resource
     * @param id           resource identifier
     * @return BusinessException with RESOURCE_NOT_FOUND code
     */
    public static BusinessException notFound(String resourceName, Object id) {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                String.format("%s not found with id: %s", resourceName, id)
        );
    }

    /**
     * Static factory method to create an invalid parameter exception.
     *
     * @param parameterName name of the invalid parameter
     * @param reason        reason for invalidity
     * @return BusinessException with INVALID_PARAMETER code
     */
    public static BusinessException invalidParameter(String parameterName, String reason) {
        return new BusinessException(
                ErrorCode.INVALID_PARAMETER,
                String.format("Invalid parameter '%s': %s", parameterName, reason)
        );
    }

    /**
     * Static factory method to create an unauthorized exception.
     *
     * @return BusinessException with UNAUTHORIZED code
     */
    public static BusinessException unauthorized() {
        return new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * Static factory method to create an access denied exception.
     *
     * @return BusinessException with ACCESS_DENIED code
     */
    public static BusinessException accessDenied() {
        return new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
