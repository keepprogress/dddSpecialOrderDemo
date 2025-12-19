package com.tgfc.som.pricing.dto;

import java.math.BigDecimal;

/**
 * 會員折扣明細 VO
 *
 * 用於記錄價格試算中的會員折扣資訊
 *
 * @param skuNo 商品編號
 * @param discType 折扣類型（0/1/2/SPECIAL）
 * @param discTypeName 折扣類型名稱
 * @param originalPrice 原價（整數）
 * @param discountPrice 折扣價（整數）
 * @param discAmt 折扣金額（整數，負數）
 * @param discRate 折扣率（Type 0 用，如 0.95 表示 95 折）
 * @param markupRate 加成比例（Type 2 用，如 1.05 表示成本加成 5%）
 */
public record MemberDiscVO(
    String skuNo,
    String discType,
    String discTypeName,
    int originalPrice,
    int discountPrice,
    int discAmt,
    BigDecimal discRate,
    BigDecimal markupRate
) {

    /**
     * 建立 Type 0 (Discounting 折價) 折扣明細
     */
    public static MemberDiscVO ofType0(String skuNo, int originalPrice, BigDecimal discRate) {
        int discountPrice = BigDecimal.valueOf(originalPrice)
            .multiply(discRate)
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .intValue();
        int discAmt = discountPrice - originalPrice; // 負數

        return new MemberDiscVO(
            skuNo,
            "0",
            "折價 (Discounting)",
            originalPrice,
            discountPrice,
            discAmt,
            discRate,
            null
        );
    }

    /**
     * 建立 Type 1 (Down Margin 下降) 折扣明細
     */
    public static MemberDiscVO ofType1(String skuNo, int originalPrice, int discountPrice) {
        int discAmt = discountPrice - originalPrice; // 負數

        return new MemberDiscVO(
            skuNo,
            "1",
            "下降 (Down Margin)",
            originalPrice,
            discountPrice,
            discAmt,
            null,
            null
        );
    }

    /**
     * 建立 Type 2 (Cost Markup 成本加成) 折扣明細
     */
    public static MemberDiscVO ofType2(String skuNo, int originalPrice, int cost, BigDecimal markupRate) {
        int discountPrice = BigDecimal.valueOf(cost)
            .multiply(markupRate)
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .intValue();
        int discAmt = discountPrice - originalPrice; // 可能為負數

        return new MemberDiscVO(
            skuNo,
            "2",
            "成本加成 (Cost Markup)",
            originalPrice,
            discountPrice,
            discAmt,
            null,
            markupRate
        );
    }

    /**
     * 建立 SPECIAL (特殊會員) 折扣明細
     */
    public static MemberDiscVO ofSpecial(String skuNo, int originalPrice, int discountPrice) {
        int discAmt = discountPrice - originalPrice; // 負數

        return new MemberDiscVO(
            skuNo,
            "SPECIAL",
            "特殊會員",
            originalPrice,
            discountPrice,
            discAmt,
            null,
            null
        );
    }

    /**
     * 檢查折扣金額是否為負數（正常情況）
     */
    public boolean isValidDiscount() {
        return discAmt <= 0;
    }
}
