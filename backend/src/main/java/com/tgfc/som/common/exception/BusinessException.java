package com.tgfc.som.common.exception;

/**
 * 業務邏輯例外
 *
 * 用於表示業務規則驗證失敗的情況
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String[] args;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new String[0];
    }

    public BusinessException(String errorCode, String message, String... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = new String[0];
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String[] getArgs() {
        return args;
    }

    // 常用業務異常工廠方法

    public static BusinessException orderNotFound(String orderId) {
        return new BusinessException("ORDER_NOT_FOUND", "訂單不存在: " + orderId, orderId);
    }

    public static BusinessException memberNotFound(String memberId) {
        return new BusinessException("MEMBER_NOT_FOUND", "會員不存在: " + memberId, memberId);
    }

    public static BusinessException productNotEligible(String skuNo, String reason) {
        return new BusinessException("PRODUCT_NOT_ELIGIBLE", "商品不符合銷售資格: " + reason, skuNo, reason);
    }

    public static BusinessException orderLineLimit(int limit) {
        return new BusinessException("ORDER_LINE_LIMIT", "訂單明細超過限制: " + limit + " 筆", String.valueOf(limit));
    }

    public static BusinessException calculationRequired() {
        return new BusinessException("CALCULATION_REQUIRED", "請先執行價格試算");
    }

    public static BusinessException invalidStatusTransition(String from, String to) {
        return new BusinessException("INVALID_STATUS_TRANSITION",
                "無效的狀態轉換: " + from + " -> " + to, from, to);
    }

    public static BusinessException couponNotApplicable(String reason) {
        return new BusinessException("COUPON_NOT_APPLICABLE", "優惠券無法使用: " + reason, reason);
    }

    public static BusinessException insufficientBonusPoints(int available, int required) {
        return new BusinessException("INSUFFICIENT_BONUS_POINTS",
                "紅利點數不足: 可用 " + available + ", 需要 " + required,
                String.valueOf(available), String.valueOf(required));
    }
}
