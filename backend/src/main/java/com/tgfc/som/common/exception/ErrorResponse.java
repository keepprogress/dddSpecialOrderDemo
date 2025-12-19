package com.tgfc.som.common.exception;

import java.time.LocalDateTime;

/**
 * 錯誤回應 DTO
 */
public record ErrorResponse(
        String errorCode,
        String message,
        String path,
        LocalDateTime timestamp,
        String traceId
) {

    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(
                errorCode,
                message,
                path,
                LocalDateTime.now(),
                null
        );
    }

    public static ErrorResponse of(String errorCode, String message, String path, String traceId) {
        return new ErrorResponse(
                errorCode,
                message,
                path,
                LocalDateTime.now(),
                traceId
        );
    }
}
