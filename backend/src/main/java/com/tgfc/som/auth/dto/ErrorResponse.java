package com.tgfc.som.auth.dto;

import java.time.LocalDateTime;

/**
 * 錯誤回應 (Constitution X: Java Record)
 *
 * @param timestamp  時間戳
 * @param status     HTTP 狀態碼
 * @param errorCode  錯誤代碼
 * @param message    錯誤訊息
 * @param details    詳細資訊
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String errorCode,
    String message,
    String details
) {
    /**
     * 建立錯誤回應
     */
    public static ErrorResponse of(int status, String errorCode, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, errorCode, message, null);
    }

    /**
     * 建立錯誤回應 (含詳細資訊)
     */
    public static ErrorResponse of(int status, String errorCode, String message, String details) {
        return new ErrorResponse(LocalDateTime.now(), status, errorCode, message, details);
    }
}
