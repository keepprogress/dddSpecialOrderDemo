package com.tgfc.som.auth.dto;

/**
 * 通用 API 回應 (Constitution X: Java Record)
 *
 * @param success 是否成功
 * @param message 訊息
 * @param data    資料
 * @param <T>     資料類型
 */
public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {
    /**
     * 建立成功回應
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    /**
     * 建立成功回應 (自訂訊息)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 建立失敗回應
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
