package com.tgfc.som.pricing.domain;

/**
 * 商品類型
 *
 * 用於 12-Step 計價流程的 Step 3: assortSku()
 * 將商品依類型分群，供後續會員折扣及試算記錄使用
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Section 1.3 Step 3</a>
 */
public enum GoodsType {

    // 一般商品 (lstGoodsSku)
    P("P", "一般商品", Category.PRODUCT),

    // 安裝商品 (lstInstallSku)
    I("I", "基本安裝", Category.INSTALLATION),
    IA("IA", "進階安裝", Category.INSTALLATION),
    IE("IE", "其他安裝", Category.INSTALLATION),
    IC("IC", "安裝調整", Category.INSTALLATION),
    IS("IS", "補安裝費", Category.INSTALLATION),

    // 免安商品 (lstFreeInstallSku)
    FI("FI", "免安折扣", Category.FREE_INSTALLATION),

    // 運送商品 (lstDeliverSku)
    DD("DD", "運送", Category.DELIVERY),

    // 直送商品 (lstDirectShipmentSku)
    VD("VD", "直送", Category.DIRECT_SHIPMENT),

    // 工種商品 (lstWorkTypeSku)
    D("D", "工種", Category.WORKTYPE),

    // 折扣類型 (用於試算記錄)
    VT("VT", "會員卡折扣", Category.DISCOUNT),
    CP("CP", "折價券", Category.DISCOUNT),
    CK("CK", "折扣券", Category.DISCOUNT),
    CI("CI", "酷卡折扣", Category.DISCOUNT),
    BP("BP", "紅利折抵", Category.DISCOUNT),
    TT("TT", "總額折扣", Category.DISCOUNT);

    private final String code;
    private final String name;
    private final Category category;

    GoodsType(String code, String name, Category category) {
        this.code = code;
        this.name = name;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    /**
     * 是否為一般商品 (P)
     */
    public boolean isProduct() {
        return this.category == Category.PRODUCT;
    }

    /**
     * 是否為安裝商品 (I/IA/IE/IC/IS)
     */
    public boolean isInstallation() {
        return this.category == Category.INSTALLATION;
    }

    /**
     * 是否為免安商品 (FI)
     */
    public boolean isFreeInstallation() {
        return this.category == Category.FREE_INSTALLATION;
    }

    /**
     * 是否為運送商品 (DD)
     */
    public boolean isDelivery() {
        return this.category == Category.DELIVERY;
    }

    /**
     * 是否為直送商品 (VD)
     */
    public boolean isDirectShipment() {
        return this.category == Category.DIRECT_SHIPMENT;
    }

    /**
     * 是否為工種商品 (D)
     */
    public boolean isWorkType() {
        return this.category == Category.WORKTYPE;
    }

    /**
     * 是否可參與會員折扣
     * 根據規格 Section 2: 一般商品(P)、安裝商品(I類)、運送商品(DD/VD) 可參與會員折扣
     */
    public boolean canHaveMemberDiscount() {
        return this.category == Category.PRODUCT
            || this.category == Category.INSTALLATION
            || this.category == Category.DELIVERY
            || this.category == Category.DIRECT_SHIPMENT;
    }

    /**
     * 根據代碼查詢 GoodsType
     */
    public static GoodsType fromCode(String code) {
        if (code == null) {
            return P; // 預設為一般商品
        }
        for (GoodsType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return P; // 預設為一般商品
    }

    /**
     * 商品類型分類
     */
    public enum Category {
        PRODUCT,           // 一般商品
        INSTALLATION,      // 安裝商品
        FREE_INSTALLATION, // 免安商品
        DELIVERY,          // 運送商品
        DIRECT_SHIPMENT,   // 直送商品
        WORKTYPE,          // 工種商品
        DISCOUNT           // 折扣類型
    }
}
