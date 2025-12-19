package com.tgfc.som.catalog.dto;

import com.tgfc.som.order.domain.valueobject.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 安裝服務 DTO
 *
 * 定義商品可選的安裝服務類型與費用
 */
public record InstallationService(
    String serviceType,       // 服務類型代碼 (I, IA, IE, IC, IS, FI)
    String serviceName,       // 服務名稱
    String serviceSku,        // 服務商品 SKU
    Money basePrice,          // 基礎價格
    boolean isMandatory,      // 是否為必要安裝
    BigDecimal discountBase,  // 標安成本折數
    BigDecimal discountExtra  // 非標成本折數
) {
    /**
     * 服務類型常數
     */
    public static final String TYPE_INSTALLATION = "I";      // 一般安裝
    public static final String TYPE_ADVANCED = "IA";         // 進階安裝
    public static final String TYPE_EXTRA = "IE";            // 額外安裝
    public static final String TYPE_COMPLETE = "IC";         // 完整安裝
    public static final String TYPE_SPECIAL = "IS";          // 特殊安裝
    public static final String TYPE_FREE = "FI";             // 免費安裝

    /**
     * 計算安裝成本
     *
     * @return 成本金額
     */
    public Money calculateCost() {
        BigDecimal costRate = TYPE_EXTRA.equals(serviceType)
            ? discountExtra
            : discountBase;

        if (costRate == null) {
            costRate = BigDecimal.ONE;
        }

        return basePrice.multiply(costRate, RoundingMode.FLOOR);
    }

    /**
     * 是否為免費安裝
     */
    public boolean isFreeInstallation() {
        return TYPE_FREE.equals(serviceType) ||
               (basePrice != null && basePrice.isZero());
    }

    /**
     * 是否為額外安裝（非標準）
     */
    public boolean isExtraInstallation() {
        return TYPE_EXTRA.equals(serviceType);
    }

    /**
     * 是否為標準安裝
     */
    public boolean isStandardInstallation() {
        return TYPE_INSTALLATION.equals(serviceType);
    }

    /**
     * 建立一般安裝服務
     */
    public static InstallationService standard(String sku, Money price) {
        return new InstallationService(
            TYPE_INSTALLATION, "一般安裝", sku,
            price, false,
            new BigDecimal("0.85"), new BigDecimal("0.90")
        );
    }

    /**
     * 建立進階安裝服務
     */
    public static InstallationService advanced(String sku, Money price) {
        return new InstallationService(
            TYPE_ADVANCED, "進階安裝", sku,
            price, false,
            new BigDecimal("0.80"), new BigDecimal("0.85")
        );
    }

    /**
     * 建立必要安裝服務
     */
    public static InstallationService mandatory(String sku, Money price) {
        return new InstallationService(
            TYPE_INSTALLATION, "必要安裝", sku,
            price, true,
            new BigDecimal("0.85"), new BigDecimal("0.90")
        );
    }

    /**
     * 建立免費安裝服務
     */
    public static InstallationService free(String sku) {
        return new InstallationService(
            TYPE_FREE, "免費安裝", sku,
            Money.ZERO, false,
            BigDecimal.ONE, BigDecimal.ONE
        );
    }

    /**
     * 建立測試用安裝服務
     */
    public static InstallationService testService(String type, String name, int price) {
        return new InstallationService(
            type, name, "SVC" + type,
            new Money(price), false,
            new BigDecimal("0.85"), new BigDecimal("0.90")
        );
    }
}
