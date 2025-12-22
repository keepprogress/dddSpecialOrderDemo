package com.tgfc.som.catalog.dto;

/**
 * 商品可訂購性結果
 *
 * 來源: product-query-spec.md Section 11.3
 *
 * @param orderable 是否可訂購
 * @param lockReason 鎖定原因（若不可訂購）
 * @param isDcVendorFrozen DC商品廠商凍結標記
 * @param isLargeFurniture 大型家具標記
 * @param isServiceSku 外包服務商品標記 (026-888)
 * @param stockAoh 庫存量 (僅 DC 商品廠商凍結時使用)
 */
public record OrderabilityResult(
    boolean orderable,
    String lockReason,
    boolean isDcVendorFrozen,
    boolean isLargeFurniture,
    boolean isServiceSku,
    int stockAoh
) {
    /**
     * 建立可訂購結果
     */
    public static OrderabilityResult canOrder() {
        return new OrderabilityResult(true, null, false, false, false, 0);
    }

    /**
     * 建立可訂購結果 (含大型家具/服務商品標記)
     */
    public static OrderabilityResult canOrder(boolean isLargeFurniture, boolean isServiceSku) {
        return new OrderabilityResult(true, null, false, isLargeFurniture, isServiceSku, 0);
    }

    /**
     * 建立不可訂購結果
     */
    public static OrderabilityResult notOrderable(String reason) {
        return new OrderabilityResult(false, reason, false, false, false, 0);
    }

    /**
     * 建立 DC 商品廠商凍結結果 (需查 AOH)
     */
    public static OrderabilityResult dcVendorFrozen(int stockAoh, boolean isLargeFurniture) {
        return new OrderabilityResult(true, null, true, isLargeFurniture, false, stockAoh);
    }

    /**
     * 判斷是否需要強制現貨
     * DC商品廠商凍結 + 非大型家具 + 庫存不足 → 強制現貨
     */
    public boolean requiresSpotStock(int requestedQuantity) {
        return isDcVendorFrozen && !isLargeFurniture && stockAoh < requestedQuantity;
    }
}
