package com.tgfc.som.common.exception;

/**
 * 重複提交異常
 *
 * 當冪等鍵在 5 秒內重複使用時拋出
 */
public class DuplicateSubmissionException extends RuntimeException {

    private final String idempotencyKey;
    private final String existingOrderId;

    public DuplicateSubmissionException(String message) {
        super(message);
        this.idempotencyKey = null;
        this.existingOrderId = null;
    }

    public DuplicateSubmissionException(String idempotencyKey, String existingOrderId) {
        super("重複提交，請稍後再試。原始訂單: " + existingOrderId);
        this.idempotencyKey = idempotencyKey;
        this.existingOrderId = existingOrderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExistingOrderId() {
        return existingOrderId;
    }
}
