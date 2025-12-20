package com.tgfc.som.catalog.domain;

/**
 * 採購屬性 (DC_TYPE)
 *
 * 來源: product-query-spec.md Section 2.3
 */
public enum DcType {
    /**
     * 交叉轉運 - 廠商凍結時直接鎖定不可訂購
     */
    XD("XD", "交叉轉運"),

    /**
     * 庫存持有 - 廠商凍結時需查 AOH 庫存量決定
     */
    DC("DC", "庫存持有"),

    /**
     * 供應商直送 - 廠商凍結時直接鎖定不可訂購
     */
    VD("VD", "供應商直送");

    private final String code;
    private final String description;

    DcType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DcType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (DcType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判斷廠商凍結時是否需要查 AOH
     */
    public boolean requiresAohCheckWhenVendorFrozen() {
        return this == DC;
    }
}
