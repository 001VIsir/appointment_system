package org.example.appointment_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 所有API错误的标准化错误响应格式。
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * 错误发生的时间戳。
     */
    private final LocalDateTime timestamp;

    /**
     * HTTP状态码。
     */
    private final int status;

    /**
     * 用于程序化处理的错误代码。
     */
    private final int code;

    /**
     * 简短的错误信息。
     */
    private final String error;

    /**
     * 详细的错误信息。
     */
    private final String message;

    /**
     * 引发错误的请求路径。
     */
    private final String path;

    /**
     * 用于日志关联的跟踪ID（可选）。
     */
    private final String traceId;

    /**
     * 验证错误（可选，用于验证失败）。
     */
    private final List<FieldError> fieldErrors;

    /**
     * 字段级别的验证错误。
     */
    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
        private final Object rejectedValue;
    }

    /**
     * 创建简单的错误响应。
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
     * 创建包含字段错误的错误响应。
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
