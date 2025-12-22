package com.tgfc.som.common.logging;

import org.slf4j.MDC;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * 結構化日誌工具類
 *
 * 提供 MDC 上下文管理，用於 JSON 日誌輸出
 * MDC 欄位：operatorId, actionType, orderId, duration, errorCode, traceId
 */
public final class StructuredLogging {

    private StructuredLogging() {
        // Utility class
    }

    public static final String KEY_OPERATOR_ID = "operatorId";
    public static final String KEY_ACTION_TYPE = "actionType";
    public static final String KEY_ORDER_ID = "orderId";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_TRACE_ID = "traceId";

    /**
     * 設定操作者 ID
     */
    public static void setOperatorId(String operatorId) {
        MDC.put(KEY_OPERATOR_ID, operatorId);
    }

    /**
     * 設定動作類型
     */
    public static void setActionType(String actionType) {
        MDC.put(KEY_ACTION_TYPE, actionType);
    }

    /**
     * 設定訂單編號
     */
    public static void setOrderId(String orderId) {
        MDC.put(KEY_ORDER_ID, orderId);
    }

    /**
     * 設定執行時間（毫秒）
     */
    public static void setDuration(long durationMs) {
        MDC.put(KEY_DURATION, String.valueOf(durationMs));
    }

    /**
     * 設定錯誤代碼
     */
    public static void setErrorCode(String errorCode) {
        MDC.put(KEY_ERROR_CODE, errorCode);
    }

    /**
     * 設定追蹤 ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(KEY_TRACE_ID, traceId);
    }

    /**
     * 清除所有 MDC 上下文
     */
    public static void clear() {
        MDC.remove(KEY_OPERATOR_ID);
        MDC.remove(KEY_ACTION_TYPE);
        MDC.remove(KEY_ORDER_ID);
        MDC.remove(KEY_DURATION);
        MDC.remove(KEY_ERROR_CODE);
        MDC.remove(KEY_TRACE_ID);
    }

    /**
     * 包裝執行並記錄執行時間
     *
     * @param actionType 動作類型
     * @param orderId 訂單編號
     * @param supplier 要執行的操作
     * @return 操作結果
     */
    public static <T> T withTiming(String actionType, String orderId, Supplier<T> supplier) {
        setActionType(actionType);
        if (orderId != null) {
            setOrderId(orderId);
        }

        long startTime = System.currentTimeMillis();
        try {
            return supplier.get();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            setDuration(duration);
        }
    }

    /**
     * 包裝執行並記錄執行時間（無返回值）
     */
    public static void withTiming(String actionType, String orderId, Runnable runnable) {
        withTiming(actionType, orderId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 建立訂單操作的日誌上下文
     */
    public static OrderLoggingContext forOrder(String orderId, String operatorId) {
        return new OrderLoggingContext(orderId, operatorId);
    }

    /**
     * 訂單日誌上下文類
     */
    public static class OrderLoggingContext implements AutoCloseable {
        private final long startTime;

        public OrderLoggingContext(String orderId, String operatorId) {
            this.startTime = System.currentTimeMillis();
            setOrderId(orderId);
            if (operatorId != null) {
                setOperatorId(operatorId);
            }
        }

        public OrderLoggingContext action(String actionType) {
            setActionType(actionType);
            return this;
        }

        public OrderLoggingContext error(String errorCode) {
            setErrorCode(errorCode);
            return this;
        }

        public void recordDuration() {
            setDuration(System.currentTimeMillis() - startTime);
        }

        @Override
        public void close() {
            recordDuration();
            clear();
        }
    }
}
