package org.example.appointment_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error code enumeration for business exceptions.
 * Each error code maps to an HTTP status and a message.
 */
@Getter
public enum ErrorCode {

    // Common errors (1xxx)
    INVALID_PARAMETER(1001, "Invalid parameter", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "Resource not found", HttpStatus.NOT_FOUND),
    OPERATION_NOT_ALLOWED(1003, "Operation not allowed", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(1004, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    TOO_MANY_REQUESTS(1005, "Too many requests", HttpStatus.TOO_MANY_REQUESTS),

    // Authentication errors (2xxx)
    UNAUTHORIZED(2001, "Unauthorized access", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(2002, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(2003, "Session expired", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(2004, "Account is disabled", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED(2005, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2006, "Invalid token", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(2007, "Access denied", HttpStatus.FORBIDDEN),

    // User errors (3xxx)
    USER_NOT_FOUND(3001, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(3002, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(3003, "Email already exists", HttpStatus.CONFLICT),
    INVALID_USER_STATUS(3004, "Invalid user status", HttpStatus.BAD_REQUEST),

    // Merchant errors (4xxx)
    MERCHANT_PROFILE_NOT_FOUND(4001, "Merchant profile not found", HttpStatus.NOT_FOUND),
    MERCHANT_PROFILE_ALREADY_EXISTS(4002, "Merchant profile already exists", HttpStatus.CONFLICT),
    MERCHANT_NOT_ACTIVE(4003, "Merchant is not active", HttpStatus.FORBIDDEN),
    INVALID_MERCHANT_SETTINGS(4004, "Invalid merchant settings", HttpStatus.BAD_REQUEST),

    // Service item errors (5xxx)
    SERVICE_ITEM_NOT_FOUND(5001, "Service item not found", HttpStatus.NOT_FOUND),
    SERVICE_ITEM_NAME_EXISTS(5002, "Service item name already exists", HttpStatus.CONFLICT),
    SERVICE_ITEM_INACTIVE(5003, "Service item is inactive", HttpStatus.BAD_REQUEST),

    // Appointment task errors (6xxx)
    APPOINTMENT_TASK_NOT_FOUND(6001, "Appointment task not found", HttpStatus.NOT_FOUND),
    APPOINTMENT_TASK_INACTIVE(6002, "Appointment task is inactive", HttpStatus.BAD_REQUEST),
    APPOINTMENT_TASK_ALREADY_EXISTS(6003, "Appointment task already exists for this date", HttpStatus.CONFLICT),

    // Appointment slot errors (7xxx)
    APPOINTMENT_SLOT_NOT_FOUND(7001, "Appointment slot not found", HttpStatus.NOT_FOUND),
    APPOINTMENT_SLOT_FULL(7002, "Appointment slot is full", HttpStatus.BAD_REQUEST),
    APPOINTMENT_SLOT_HAS_BOOKINGS(7003, "Appointment slot has active bookings", HttpStatus.BAD_REQUEST),

    // Booking errors (8xxx)
    BOOKING_NOT_FOUND(8001, "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_EXISTS(8002, "Booking already exists for this slot", HttpStatus.CONFLICT),
    BOOKING_CANNOT_CANCEL(8003, "Booking cannot be cancelled", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_CONFIRM(8004, "Booking cannot be confirmed", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_COMPLETE(8005, "Booking cannot be completed", HttpStatus.BAD_REQUEST),
    BOOKING_CONCURRENT_MODIFICATION(8006, "Booking was modified by another user", HttpStatus.CONFLICT),

    // Signed link errors (9xxx)
    SIGNED_LINK_INVALID(9001, "Invalid signed link", HttpStatus.BAD_REQUEST),
    SIGNED_LINK_EXPIRED(9002, "Signed link has expired", HttpStatus.BAD_REQUEST),
    SIGNED_LINK_SIGNATURE_MISMATCH(9003, "Signed link signature mismatch", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
