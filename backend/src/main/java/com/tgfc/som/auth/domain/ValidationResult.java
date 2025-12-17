package com.tgfc.som.auth.domain;

/**
 * 驗證結果 (Constitution X: Java Record)
 *
 * @param success      是否驗證成功
 * @param errorCode    錯誤代碼 (失敗時)
 * @param errorMessage 錯誤訊息 (失敗時)
 * @param systemFlag   系統權限標記 (成功時)
 */
public record ValidationResult(
    boolean success,
    String errorCode,
    String errorMessage,
    String systemFlag
) {
    /**
     * 建立成功結果
     */
    public static ValidationResult ok(String systemFlag) {
        return new ValidationResult(true, null, null, systemFlag);
    }

    /**
     * 建立成功結果 (無系統權限)
     */
    public static ValidationResult ok() {
        return new ValidationResult(true, null, null, null);
    }

    /**
     * 建立失敗結果
     */
    public static ValidationResult error(String errorCode, String errorMessage) {
        return new ValidationResult(false, errorCode, errorMessage, null);
    }
}
