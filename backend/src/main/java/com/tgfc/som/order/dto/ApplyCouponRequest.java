package com.tgfc.som.order.dto;

import java.util.Objects;

/**
 * 套用優惠券請求
 *
 * @param couponId 優惠券編號
 * @param quantity 使用張數（預設為 1）
 */
public record ApplyCouponRequest(
    String couponId,
    Integer quantity
) {
    public ApplyCouponRequest {
        Objects.requireNonNull(couponId, "優惠券編號不可為空");
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
    }

    /**
     * 建立單張優惠券請求
     */
    public static ApplyCouponRequest of(String couponId) {
        return new ApplyCouponRequest(couponId, 1);
    }

    /**
     * 建立多張優惠券請求
     */
    public static ApplyCouponRequest of(String couponId, int quantity) {
        return new ApplyCouponRequest(couponId, quantity);
    }
}
