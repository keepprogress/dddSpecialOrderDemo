package com.tgfc.som.fulfillment.dto;

import com.tgfc.som.fulfillment.domain.WorkCategory;
import com.tgfc.som.order.domain.valueobject.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 工種資訊 DTO
 *
 * 包含工種的基本資訊與費率設定
 */
public record WorkType(
    String workTypeId,            // 工種代碼
    String workTypeName,          // 工種名稱
    WorkCategory category,        // 工種類別
    Money minimumWage,            // 最低工資
    BigDecimal basicDiscount,     // 標安成本折數
    BigDecimal advancedDiscount,  // 非標成本折數
    BigDecimal deliveryDiscount   // 運送成本折數
) {
    /**
     * 計算安裝成本
     *
     * @param basePrice 基礎價格
     * @param isBasic 是否為標準安裝
     * @return 計算後的安裝成本
     */
    public Money calculateInstallationCost(Money basePrice, boolean isBasic) {
        BigDecimal rate = isBasic ? basicDiscount : advancedDiscount;
        if (rate == null) {
            rate = BigDecimal.ONE;
        }
        return basePrice.multiply(rate, RoundingMode.FLOOR);
    }

    /**
     * 計算運送成本
     *
     * @param basePrice 基礎價格
     * @return 計算後的運送成本
     */
    public Money calculateDeliveryCost(Money basePrice) {
        BigDecimal rate = deliveryDiscount != null ? deliveryDiscount : BigDecimal.ONE;
        return basePrice.multiply(rate, RoundingMode.FLOOR);
    }

    /**
     * 檢查是否達到最低工資
     *
     * @param installationTotal 安裝費用總額
     * @return 是否達到最低工資
     */
    public boolean meetsMinimumWage(Money installationTotal) {
        if (isPureDelivery() || isHomeDelivery()) {
            return true; // 純運與宅配不檢查最低工資
        }
        if (minimumWage == null || minimumWage.isZero()) {
            return true;
        }
        return installationTotal.amount() >= minimumWage.amount();
    }

    /**
     * 計算最低工資差額
     *
     * @param installationTotal 安裝費用總額
     * @return 差額（正數表示需補足的金額）
     */
    public Money getMinimumWageGap(Money installationTotal) {
        if (meetsMinimumWage(installationTotal)) {
            return Money.ZERO;
        }
        return minimumWage.subtract(installationTotal);
    }

    /**
     * 是否為純運工種
     */
    public boolean isPureDelivery() {
        return "0000".equals(workTypeId) || category == WorkCategory.PURE_DELIVERY;
    }

    /**
     * 是否為宅配工種
     */
    public boolean isHomeDelivery() {
        return category == WorkCategory.HOME_DELIVERY;
    }

    /**
     * 是否為安裝工種
     */
    public boolean isInstallation() {
        return category == WorkCategory.INSTALLATION;
    }

    /**
     * 建立純運工種
     */
    public static WorkType pureDelivery() {
        return new WorkType(
            "0000", "純運",
            WorkCategory.PURE_DELIVERY,
            Money.ZERO,
            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE
        );
    }

    /**
     * 建立測試用工種
     */
    public static WorkType testWorkType(String id, String name, int minimumWageAmount) {
        return new WorkType(
            id, name,
            WorkCategory.INSTALLATION,
            new Money(minimumWageAmount),
            new BigDecimal("0.85"), new BigDecimal("0.90"), new BigDecimal("0.95")
        );
    }
}
