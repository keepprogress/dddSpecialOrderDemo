package com.tgfc.som.pricing.dto;

import com.tgfc.som.order.domain.valueobject.Money;

import java.util.Objects;

/**
 * 紅利點數折抵記錄
 *
 * 記錄會員使用紅利點數折抵的明細
 *
 * @param memberId 會員編號
 * @param skuNo 折抵商品編號
 * @param skuName 折抵商品名稱
 * @param pointsUsed 使用點數
 * @param discountAmount 折抵金額
 * @param exchangeRate 兌換比率（每點可折抵金額）
 * @param remainingPoints 使用後剩餘點數
 */
public record BonusRedemption(
    String memberId,
    String skuNo,
    String skuName,
    int pointsUsed,
    Money discountAmount,
    int exchangeRate,
    int remainingPoints
) {
    public BonusRedemption {
        Objects.requireNonNull(memberId, "會員編號不可為空");
        Objects.requireNonNull(skuNo, "商品編號不可為空");
        if (pointsUsed <= 0) {
            throw new IllegalArgumentException("使用點數必須大於零");
        }
    }

    /**
     * 計算折抵金額
     *
     * @param points 使用點數
     * @param exchangeRate 兌換比率
     * @return 折抵金額
     */
    public static Money calculateDiscountAmount(int points, int exchangeRate) {
        return Money.of(points * exchangeRate);
    }

    /**
     * 建立紅利折抵記錄
     */
    public static BonusRedemption create(
            String memberId,
            String skuNo,
            String skuName,
            int pointsUsed,
            int exchangeRate,
            int remainingPoints) {
        Money discountAmount = calculateDiscountAmount(pointsUsed, exchangeRate);
        return new BonusRedemption(
                memberId, skuNo, skuName,
                pointsUsed, discountAmount,
                exchangeRate, remainingPoints
        );
    }

    /**
     * 驗證是否有足夠點數
     */
    public static boolean hasEnoughPoints(int availablePoints, int requestedPoints) {
        return availablePoints >= requestedPoints;
    }
}
