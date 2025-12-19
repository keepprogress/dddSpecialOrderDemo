package com.tgfc.som.order.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 運送方式列舉
 *
 * 參考 SoConstant.java
 * 值：N=運送/D=純運/V=直送/C=當場自取/F=宅配/P=下次自取
 */
public enum DeliveryMethod {

    MANAGED("N", "運送", true, true),
    PURE_DELIVERY("D", "純運", false, true),
    DIRECT_SHIPMENT("V", "直送", false, false),
    IMMEDIATE_PICKUP("C", "當場自取", false, false),
    HOME_DELIVERY("F", "宅配", false, true),
    LATER_PICKUP("P", "下次自取", false, false);

    private final String code;
    private final String name;
    private final boolean requiresWorkType;
    private final boolean requiresDelivery;

    DeliveryMethod(String code, String name, boolean requiresWorkType, boolean requiresDelivery) {
        this.code = code;
        this.name = name;
        this.requiresWorkType = requiresWorkType;
        this.requiresDelivery = requiresDelivery;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isRequiresWorkType() {
        return requiresWorkType;
    }

    public boolean isRequiresDelivery() {
        return requiresDelivery;
    }

    public static Optional<DeliveryMethod> fromCode(String code) {
        return Arrays.stream(values())
                .filter(method -> method.code.equals(code))
                .findFirst();
    }

    public static DeliveryMethod fromCodeOrThrow(String code) {
        return fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("無效的運送方式代碼: " + code));
    }

    /**
     * 檢查與備貨方式的相容性
     */
    public boolean isCompatibleWith(StockMethod stockMethod) {
        return switch (this) {
            case DIRECT_SHIPMENT -> stockMethod == StockMethod.PURCHASE_ORDER;
            case IMMEDIATE_PICKUP -> stockMethod == StockMethod.IN_STOCK;
            default -> true;
        };
    }

    /**
     * 是否需要安裝服務
     */
    public boolean canHaveInstallation() {
        return this == MANAGED;
    }

    /**
     * 是否為純運或宅配（不檢查最低工資）
     */
    public boolean skipMinimumWageCheck() {
        return this == PURE_DELIVERY || this == HOME_DELIVERY;
    }
}
