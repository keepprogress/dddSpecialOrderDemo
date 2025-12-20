package com.tgfc.som.pricing.domain;

/**
 * 促銷類型
 *
 * 對應 8 種 Event 類型 (A-H)，用於 12-Step 計價流程的 Step 5: promotionCalculation()
 * 注意: OMS 為唯一促銷決策者，SOM 僅執行 OMS 指定的促銷類型
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Section 21.7</a>
 */
public enum PromotionType {

    /**
     * A: 印花價 (單品促銷)
     * 公式: newPrice = CEIL(posAmt × (1 - discRate)) 或 fixedPrice
     * 短路條件: 無符合商品
     */
    A("A", "印花價", "單品特定售價"),

    /**
     * B: 滿額加價購
     * 公式: 優惠組數 = FLOOR(發票金額 / 條件金額)
     * 短路條件: 金額未達標
     */
    B("B", "滿額加價購", "發票金額達標後加價購"),

    /**
     * C: 滿金額優惠
     * 公式: 全商品折扣 = 達標後統一折扣率
     * 短路條件: 金額/數量未達標
     */
    C("C", "滿金額優惠", "全面折扣"),

    /**
     * D: 買M享N (重複促銷)
     * 公式: 優惠數量 = (總數量 / M) × N
     * 短路條件: 數量不足 M
     */
    D("D", "買M享N", "重複促銷"),

    /**
     * E: A群組享B優惠
     * 公式: A群組達標 → B群組享折扣
     * 短路條件: A群組未達標
     */
    E("E", "A群組享B優惠", "跨群組促銷"),

    /**
     * F: 合購價
     * 公式: 所有群組達標 → 合購價
     * 短路條件: 任一群組未達標
     */
    F("F", "合購價", "多群組條件"),

    /**
     * G: 共用商品合購 (最複雜)
     * 公式: 多級距共用商品 → 取最大級距
     * 短路條件: 無符合級距
     */
    G("G", "共用商品合購", "多級距共用商品"),

    /**
     * H: 拆價合購
     * 公式: 單品拆價 → 多級距合購
     * 短路條件: 無符合群組
     */
    H("H", "拆價合購", "多級距");

    private final String code;
    private final String name;
    private final String description;

    PromotionType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否為單品促銷
     */
    public boolean isSingleItemPromotion() {
        return this == A;
    }

    /**
     * 是否為滿額/滿件促銷
     */
    public boolean isThresholdPromotion() {
        return this == B || this == C || this == D;
    }

    /**
     * 是否為合購促銷
     */
    public boolean isBundlePromotion() {
        return this == E || this == F || this == G || this == H;
    }

    /**
     * 根據代碼查詢 PromotionType
     */
    public static PromotionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PromotionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
