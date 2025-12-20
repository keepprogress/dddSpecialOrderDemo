package com.tgfc.som.catalog.domain;

/**
 * 採購權限 (holdOrder)
 *
 * 來源: product-query-spec.md Section 2.2.3
 */
public enum HoldOrderType {
    /**
     * 無 HOLD ORDER - PO商品可訂、DC商品可訂
     */
    N("N", "無HOLD ORDER", true, true),

    /**
     * 暫停採購及調撥 - PO商品不可訂、DC商品可訂
     */
    A("A", "暫停採購及調撥", false, true),

    /**
     * 暫停店對店調撥 - PO商品可訂、DC商品不可訂
     */
    B("B", "暫停店對店調撥", true, false),

    /**
     * 暫停所有採購調撥 - PO商品不可訂、DC商品不可訂
     */
    C("C", "暫停所有採購調撥", false, false),

    /**
     * 暫停但允許MD下單及調撥 - PO商品可訂、DC商品可訂
     */
    D("D", "暫停但允許MD下單及調撥", true, true),

    /**
     * 暫停但允許MD調撥 - PO商品可訂、DC商品可訂
     */
    E("E", "暫停但允許MD調撥", true, true);

    private final String code;
    private final String description;
    private final boolean poOrderable;
    private final boolean dcOrderable;

    HoldOrderType(String code, String description, boolean poOrderable, boolean dcOrderable) {
        this.code = code;
        this.description = description;
        this.poOrderable = poOrderable;
        this.dcOrderable = dcOrderable;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPoOrderable() {
        return poOrderable;
    }

    public boolean isDcOrderable() {
        return dcOrderable;
    }

    public static HoldOrderType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return N; // 預設無限制
        }
        for (HoldOrderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return N;
    }

    /**
     * 根據 DC_TYPE 判斷是否可訂購
     */
    public boolean isOrderable(DcType dcType) {
        if (dcType == null || dcType == DcType.XD) {
            return poOrderable; // PO 商品
        } else if (dcType == DcType.DC) {
            return dcOrderable; // DC 商品
        } else {
            return poOrderable; // VD 視為 PO
        }
    }
}
