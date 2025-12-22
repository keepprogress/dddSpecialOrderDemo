package com.tgfc.som.fulfillment.dto;

import java.math.BigDecimal;

/**
 * 工種回應
 *
 * @param workTypeId 工種代碼
 * @param workTypeName 工種名稱
 * @param category 工種類別代碼
 * @param categoryName 工種類別名稱
 * @param minimumWage 最低工資
 * @param basicDiscount 基本折扣率
 * @param advancedDiscount 進階折扣率
 * @param deliveryDiscount 運送折扣率
 */
public record WorkTypeResponse(
    String workTypeId,
    String workTypeName,
    String category,
    String categoryName,
    int minimumWage,
    BigDecimal basicDiscount,
    BigDecimal advancedDiscount,
    BigDecimal deliveryDiscount
) {
    /**
     * 從 WorkType 建立回應
     */
    public static WorkTypeResponse from(WorkType workType) {
        return new WorkTypeResponse(
            workType.workTypeId(),
            workType.workTypeName(),
            workType.category().name(),
            workType.category().getName(),
            workType.minimumWage().amount(),
            workType.basicDiscount(),
            workType.advancedDiscount(),
            workType.deliveryDiscount()
        );
    }
}
