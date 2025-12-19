package com.tgfc.som.member.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 會員折扣類型列舉
 *
 * 參考 SoConstant.java
 * 值：0=Discounting折價/1=DownMargin下降/2=CostMarkup成本加成
 *
 * 執行順序: Type 2 → 促銷 → Type 0 → Type 1 → Special
 */
public enum MemberDiscountType {

    /**
     * Type 0: Discounting (折價)
     * - 折扣率計算
     * - 不修改 actPosAmt，僅記錄於 memberDisc
     */
    DISCOUNTING("0", "Discounting", "折價", false, 3),

    /**
     * Type 1: Down Margin (下降)
     * - 固定折扣金額
     * - 直接修改 actPosAmt
     */
    DOWN_MARGIN("1", "Down Margin", "下降", true, 4),

    /**
     * Type 2: Cost Markup (成本加成)
     * - 成本加成計算
     * - 完全替換 actPosAmt
     * - 執行後必須重新分類商品
     */
    COST_MARKUP("2", "Cost Markup", "成本加成", true, 1),

    /**
     * Special: 特殊會員折扣
     * - VIP 全場折扣、員工價
     * - 僅當無其他折扣時執行
     */
    SPECIAL("SPECIAL", "Special", "特殊會員", true, 5);

    private final String code;
    private final String englishName;
    private final String chineseName;
    private final boolean modifiesActPosAmt;
    private final int executionOrder;

    MemberDiscountType(String code, String englishName, String chineseName,
                       boolean modifiesActPosAmt, int executionOrder) {
        this.code = code;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.modifiesActPosAmt = modifiesActPosAmt;
        this.executionOrder = executionOrder;
    }

    public String getCode() {
        return code;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public boolean isModifiesActPosAmt() {
        return modifiesActPosAmt;
    }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public static Optional<MemberDiscountType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }

    public static MemberDiscountType fromCodeOrThrow(String code) {
        return fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("無效的會員折扣類型代碼: " + code));
    }

    /**
     * 是否需要重新分類商品（Type 2 專用）
     */
    public boolean requiresReclassification() {
        return this == COST_MARKUP;
    }

    /**
     * 是否為一般會員折扣（排除特殊會員）
     */
    public boolean isRegularDiscount() {
        return this != SPECIAL;
    }

    public String getDisplayName() {
        return chineseName + " (" + englishName + ")";
    }
}
