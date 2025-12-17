package com.tgfc.som.auth.dto;

import java.util.List;

/**
 * 使用者驗證回應 (Constitution X: Java Record)
 *
 * @param success      是否驗證成功
 * @param userName     使用者名稱 (成功時)
 * @param systemFlags  系統權限清單 (成功時)
 * @param errorCode    錯誤代碼 (失敗時)
 * @param errorMessage 錯誤訊息 (失敗時)
 */
public record UserValidationResponse(
    boolean success,
    String userName,
    List<String> systemFlags,
    String errorCode,
    String errorMessage
) {
    /**
     * 建立成功回應
     */
    public static UserValidationResponse success(String userName, List<String> systemFlags) {
        return new UserValidationResponse(true, userName, systemFlags, null, null);
    }

    /**
     * 建立失敗回應
     */
    public static UserValidationResponse fail(String errorCode, String errorMessage) {
        return new UserValidationResponse(false, null, null, errorCode, errorMessage);
    }
}
