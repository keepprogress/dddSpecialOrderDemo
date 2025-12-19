package com.tgfc.som.pricing.domain;

import com.tgfc.som.order.domain.valueobject.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 優惠券值物件
 *
 * 代表一張優惠券的完整資訊
 */
public record Coupon(
    String couponId,              // 優惠券編號
    String couponName,            // 優惠券名稱
    CouponType type,              // 優惠券類型
    Money discountAmount,         // 折扣金額（固定金額型）
    BigDecimal discountRate,      // 折扣率（百分比型，如 0.10 表示 10% 折扣）
    Money minimumOrderAmount,     // 訂單最低金額門檻
    Money maximumDiscount,        // 最大折扣金額上限
    LocalDate validFrom,          // 有效期間起
    LocalDate validTo,            // 有效期間迄
    List<String> applicableSkus,  // 適用商品編號清單（空表示全品項適用）
    List<String> excludedSkus,    // 排除商品編號清單
    boolean freeInstallation,     // 是否免安裝費
    int remainingQuantity         // 剩餘可用張數
) {
    public Coupon {
        Objects.requireNonNull(couponId, "優惠券編號不可為空");
        Objects.requireNonNull(couponName, "優惠券名稱不可為空");
        Objects.requireNonNull(type, "優惠券類型不可為空");
        applicableSkus = applicableSkus != null ? List.copyOf(applicableSkus) : List.of();
        excludedSkus = excludedSkus != null ? List.copyOf(excludedSkus) : List.of();
    }

    /**
     * 優惠券類型
     */
    public enum CouponType {
        FIXED_AMOUNT("F", "固定金額"),      // 直接折抵固定金額
        PERCENTAGE("P", "百分比折扣"),       // 按百分比折扣
        FREE_INSTALLATION("I", "免安裝費"); // 免除安裝費用

        private final String code;
        private final String name;

        CouponType(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 檢查優惠券是否在有效期間內
     */
    public boolean isValidPeriod() {
        LocalDate today = LocalDate.now();
        boolean afterStart = validFrom == null || !today.isBefore(validFrom);
        boolean beforeEnd = validTo == null || !today.isAfter(validTo);
        return afterStart && beforeEnd;
    }

    /**
     * 檢查優惠券是否還有剩餘數量
     */
    public boolean hasRemainingQuantity() {
        return remainingQuantity > 0;
    }

    /**
     * 檢查訂單金額是否達到門檻
     */
    public boolean meetsMinimumOrder(Money orderTotal) {
        if (minimumOrderAmount == null || minimumOrderAmount.isZero()) {
            return true;
        }
        return orderTotal.amount() >= minimumOrderAmount.amount();
    }

    /**
     * 檢查商品是否適用此優惠券
     */
    public boolean isApplicableToSku(String skuNo) {
        // 如果在排除清單中，不適用
        if (excludedSkus.contains(skuNo)) {
            return false;
        }
        // 如果適用清單為空，表示全品項適用
        if (applicableSkus.isEmpty()) {
            return true;
        }
        // 否則檢查是否在適用清單中
        return applicableSkus.contains(skuNo);
    }

    /**
     * 計算折扣金額
     *
     * @param applicableTotal 適用商品的總金額
     * @return 計算後的折扣金額
     */
    public Money calculateDiscount(Money applicableTotal) {
        Money discount = switch (type) {
            case FIXED_AMOUNT -> discountAmount != null ? discountAmount : Money.ZERO;
            case PERCENTAGE -> {
                if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0) {
                    yield Money.ZERO;
                }
                int discountAmt = applicableTotal.multiply(discountRate, java.math.RoundingMode.FLOOR).amount();
                yield Money.of(discountAmt);
            }
            case FREE_INSTALLATION -> Money.ZERO; // 免安裝費另外處理
        };

        // 不能超過最大折扣上限
        if (maximumDiscount != null && !maximumDiscount.isZero()) {
            if (discount.amount() > maximumDiscount.amount()) {
                discount = maximumDiscount;
            }
        }

        // 折扣不能超過商品總額（不可產生負數）
        if (discount.amount() > applicableTotal.amount()) {
            discount = applicableTotal;
        }

        return discount;
    }

    /**
     * 建立固定金額優惠券
     */
    public static Coupon fixedAmount(
            String couponId,
            String couponName,
            Money discountAmount,
            Money minimumOrderAmount,
            LocalDate validFrom,
            LocalDate validTo) {
        return new Coupon(
                couponId, couponName,
                CouponType.FIXED_AMOUNT,
                discountAmount, null,
                minimumOrderAmount, null,
                validFrom, validTo,
                List.of(), List.of(),
                false, 1
        );
    }

    /**
     * 建立百分比折扣優惠券
     */
    public static Coupon percentage(
            String couponId,
            String couponName,
            BigDecimal discountRate,
            Money maximumDiscount,
            Money minimumOrderAmount,
            LocalDate validFrom,
            LocalDate validTo) {
        return new Coupon(
                couponId, couponName,
                CouponType.PERCENTAGE,
                null, discountRate,
                minimumOrderAmount, maximumDiscount,
                validFrom, validTo,
                List.of(), List.of(),
                false, 1
        );
    }

    /**
     * 建立免安裝費優惠券
     */
    public static Coupon freeInstallation(
            String couponId,
            String couponName,
            LocalDate validFrom,
            LocalDate validTo) {
        return new Coupon(
                couponId, couponName,
                CouponType.FREE_INSTALLATION,
                null, null,
                null, null,
                validFrom, validTo,
                List.of(), List.of(),
                true, 1
        );
    }
}
