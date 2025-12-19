package com.tgfc.som.fulfillment.domain;

/**
 * 工種類別枚舉
 *
 * 定義不同類型的工種分類
 */
public enum WorkCategory {
    INSTALLATION("I", "安裝工種"),
    DELIVERY("D", "運送工種"),
    HOME_DELIVERY("H", "宅配工種"),
    PURE_DELIVERY("P", "純運工種");

    private final String code;
    private final String name;

    WorkCategory(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根據代碼取得工種類別
     */
    public static WorkCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("無效的工種類別代碼: " + code);
    }

    /**
     * 是否為安裝類工種
     */
    public boolean isInstallation() {
        return this == INSTALLATION;
    }

    /**
     * 是否為運送類工種（含宅配、純運）
     */
    public boolean isDelivery() {
        return this == DELIVERY || this == HOME_DELIVERY || this == PURE_DELIVERY;
    }

    /**
     * 是否需要檢查最低工資
     */
    public boolean requiresMinimumWageCheck() {
        return this == INSTALLATION || this == DELIVERY;
    }

    /**
     * 是否為純運（無安裝）
     */
    public boolean isPureDelivery() {
        return this == PURE_DELIVERY;
    }

    /**
     * 是否為宅配
     */
    public boolean isHomeDelivery() {
        return this == HOME_DELIVERY;
    }
}
