package com.tgfc.som.auth.dto;

/**
 * 驗證錯誤回應 (Constitution X: Java Record)
 *
 * @param errorCode    錯誤代碼
 * @param errorMessage 錯誤訊息
 */
public record ValidationErrorResponse(
    String errorCode,
    String errorMessage
) {
    /**
     * 建立驗證錯誤回應
     */
    public static ValidationErrorResponse of(String errorCode, String errorMessage) {
        return new ValidationErrorResponse(errorCode, errorMessage);
    }
}
