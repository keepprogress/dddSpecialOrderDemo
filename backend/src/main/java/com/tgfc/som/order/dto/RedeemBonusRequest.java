package com.tgfc.som.order.dto;

import java.util.Objects;

/**
 * 紅利點數折抵請求
 *
 * @param skuNo 折抵商品編號
 * @param points 使用點數
 */
public record RedeemBonusRequest(
    String skuNo,
    int points
) {
    public RedeemBonusRequest {
        Objects.requireNonNull(skuNo, "商品編號不可為空");
        if (points <= 0) {
            throw new IllegalArgumentException("使用點數必須大於零");
        }
    }

    /**
     * 建立紅利折抵請求
     */
    public static RedeemBonusRequest of(String skuNo, int points) {
        return new RedeemBonusRequest(skuNo, points);
    }
}
