package com.tgfc.som.pricing.dto;

import com.tgfc.som.order.domain.valueobject.Money;

import java.util.List;

/**
 * 優惠券驗證結果
 *
 * @param valid 是否有效
 * @param failureReason 驗證失敗原因（valid=false 時有值）
 * @param discountAmount 計算後的折扣金額
 * @param applicableSkus 適用的商品編號清單
 * @param freeInstallation 是否免安裝費
 */
public record CouponValidation(
    boolean valid,
    String failureReason,
    Money discountAmount,
    List<String> applicableSkus,
    boolean freeInstallation
) {
    /**
     * 建立驗證成功的結果
     */
    public static CouponValidation success(Money discountAmount, List<String> applicableSkus, boolean freeInstallation) {
        return new CouponValidation(true, null, discountAmount, applicableSkus, freeInstallation);
    }

    /**
     * 建立驗證失敗的結果
     */
    public static CouponValidation failure(String reason) {
        return new CouponValidation(false, reason, Money.ZERO, List.of(), false);
    }

    /**
     * 驗證失敗：優惠券不存在
     */
    public static CouponValidation notFound() {
        return failure("優惠券不存在");
    }

    /**
     * 驗證失敗：優惠券已過期
     */
    public static CouponValidation expired() {
        return failure("優惠券已過期");
    }

    /**
     * 驗證失敗：優惠券尚未生效
     */
    public static CouponValidation notYetValid() {
        return failure("優惠券尚未生效");
    }

    /**
     * 驗證失敗：優惠券已用完
     */
    public static CouponValidation exhausted() {
        return failure("優惠券已使用完畢");
    }

    /**
     * 驗證失敗：訂單金額未達門檻
     */
    public static CouponValidation belowMinimum(int minimumAmount, int currentAmount) {
        return failure(String.format("訂單金額未達門檻（需 %d 元，目前 %d 元）", minimumAmount, currentAmount));
    }

    /**
     * 驗證失敗：無適用商品
     */
    public static CouponValidation noApplicableProducts() {
        return failure("購物車中無適用此優惠券的商品");
    }
}
