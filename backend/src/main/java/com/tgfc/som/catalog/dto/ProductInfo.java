package com.tgfc.som.catalog.dto;

import com.tgfc.som.catalog.domain.DcType;
import com.tgfc.som.catalog.domain.HoldOrderType;

/**
 * 商品資訊
 *
 * 來源: product-query-spec.md Section 1.1, 1.2
 *
 * @param skuNo 商品編號
 * @param skuName 商品名稱
 * @param skuType 商品類型 (NORM/DIEN/COUP 等)
 * @param subDeptId 大類 (子部門代碼)
 * @param classId 中類
 * @param subClassId 小類
 * @param taxType 稅別代碼 (0/1/2)
 * @param vendorId 主要供應商 ID
 * @param dcType 採購屬性 (XD/DC/VD)
 * @param holdOrderCode 採購權限代碼 (N/A/B/C/D/E)
 * @param marketPrice 市價
 * @param regularPrice 原價
 * @param posPrice POS 價格
 * @param cost 成本
 * @param allowSales 是否允許銷售
 * @param isSystemSku 是否為系統商品 (allowSales = 'N')
 * @param soFlag 特別訂購商品 (Y/N)
 * @param freeDelivery 是否免運 (FREE_DELIVER)
 * @param freeDeliveryShipping 是否免運費
 * @param allowDirectShipment 是否允許直送
 * @param openPrice 開放售價 (Y/N)
 * @param vendorStatus 廠商狀態 (A/D) - 來自 TBL_VENDOR_COMPANY
 * @param hasSkuCompany 是否在採購組織內 - 來自 TBL_SKU_COMPANY
 * @param stockAoh 庫存量 (AOH)
 * @param skuStatus 商品狀態 (A/D) - 來自 TBL_SKU
 * @param skuStoreStatus 門店商品狀態 (A/D) - 來自 TBL_SKU_STORE
 */
public record ProductInfo(
    // 基本資訊
    String skuNo,
    String skuName,
    String skuType,
    String subDeptId,
    String classId,
    String subClassId,
    String taxType,
    // 廠商與採購
    String vendorId,
    DcType dcType,
    HoldOrderType holdOrderType,
    // 價格
    int marketPrice,
    int regularPrice,
    int posPrice,
    int cost,
    // 銷售狀態
    boolean allowSales,
    boolean isSystemSku,
    boolean soFlag,
    // 運送
    boolean freeDelivery,
    boolean freeDeliveryShipping,
    boolean allowDirectShipment,
    boolean openPrice,
    // 驗證相關 (L7, L8)
    String vendorStatus,
    boolean hasSkuCompany,
    int stockAoh,
    // 商品狀態
    String skuStatus,
    String skuStoreStatus
) {

    /**
     * 是否可銷售（綜合判斷 L3, L5）
     */
    public boolean isEligibleForSale() {
        return allowSales && !isSystemSku;
    }

    /**
     * 取得有效售價
     */
    public int getEffectivePrice() {
        return posPrice > 0 ? posPrice : (regularPrice > 0 ? regularPrice : marketPrice);
    }

    /**
     * 是否為外包純服務商品 (026-888)
     * 來源: BzSkuInfoServices.java:748-750
     */
    public boolean isServiceSku() {
        return "026".equals(subDeptId) && "888".equals(classId);
    }

    /**
     * L7: 廠商是否凍結
     */
    public boolean isVendorFrozen() {
        return !"A".equals(vendorStatus);
    }

    /**
     * L7: DC 商品廠商凍結時需查 AOH
     */
    public boolean isDcVendorFrozen() {
        return isVendorFrozen() && dcType == DcType.DC;
    }

    /**
     * L8: 是否在採購組織內
     */
    public boolean isInPurchaseOrg() {
        return hasSkuCompany;
    }

    /**
     * 商品是否 Active
     */
    public boolean isActive() {
        return "A".equals(skuStatus);
    }

    /**
     * 門店商品是否 Active (DC 商品例外)
     */
    public boolean isStoreActive() {
        if ("A".equals(skuStoreStatus)) {
            return true;
        }
        // DC 商品門店停用仍可用 (特例)
        return dcType == DcType.DC;
    }

    /**
     * Builder for backward compatibility
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String skuNo;
        private String skuName;
        private String skuType = "NORM";
        private String subDeptId;
        private String classId;
        private String subClassId;
        private String taxType = "1";
        private String vendorId;
        private DcType dcType;
        private HoldOrderType holdOrderType = HoldOrderType.N;
        private int marketPrice;
        private int regularPrice;
        private int posPrice;
        private int cost;
        private boolean allowSales = true;
        private boolean isSystemSku = false;
        private boolean soFlag = false;
        private boolean freeDelivery = false;
        private boolean freeDeliveryShipping = false;
        private boolean allowDirectShipment = false;
        private boolean openPrice = false;
        private String vendorStatus = "A";
        private boolean hasSkuCompany = true;
        private int stockAoh = 0;
        private String skuStatus = "A";
        private String skuStoreStatus = "A";

        public Builder skuNo(String skuNo) { this.skuNo = skuNo; return this; }
        public Builder skuName(String skuName) { this.skuName = skuName; return this; }
        public Builder skuType(String skuType) { this.skuType = skuType; return this; }
        public Builder subDeptId(String subDeptId) { this.subDeptId = subDeptId; return this; }
        public Builder classId(String classId) { this.classId = classId; return this; }
        public Builder subClassId(String subClassId) { this.subClassId = subClassId; return this; }
        public Builder taxType(String taxType) { this.taxType = taxType; return this; }
        public Builder vendorId(String vendorId) { this.vendorId = vendorId; return this; }
        public Builder dcType(DcType dcType) { this.dcType = dcType; return this; }
        public Builder holdOrderType(HoldOrderType holdOrderType) { this.holdOrderType = holdOrderType; return this; }
        public Builder marketPrice(int marketPrice) { this.marketPrice = marketPrice; return this; }
        public Builder regularPrice(int regularPrice) { this.regularPrice = regularPrice; return this; }
        public Builder posPrice(int posPrice) { this.posPrice = posPrice; return this; }
        public Builder cost(int cost) { this.cost = cost; return this; }
        public Builder allowSales(boolean allowSales) { this.allowSales = allowSales; return this; }
        public Builder isSystemSku(boolean isSystemSku) { this.isSystemSku = isSystemSku; return this; }
        public Builder soFlag(boolean soFlag) { this.soFlag = soFlag; return this; }
        public Builder freeDelivery(boolean freeDelivery) { this.freeDelivery = freeDelivery; return this; }
        public Builder freeDeliveryShipping(boolean freeDeliveryShipping) { this.freeDeliveryShipping = freeDeliveryShipping; return this; }
        public Builder allowDirectShipment(boolean allowDirectShipment) { this.allowDirectShipment = allowDirectShipment; return this; }
        public Builder openPrice(boolean openPrice) { this.openPrice = openPrice; return this; }
        public Builder vendorStatus(String vendorStatus) { this.vendorStatus = vendorStatus; return this; }
        public Builder hasSkuCompany(boolean hasSkuCompany) { this.hasSkuCompany = hasSkuCompany; return this; }
        public Builder stockAoh(int stockAoh) { this.stockAoh = stockAoh; return this; }
        public Builder skuStatus(String skuStatus) { this.skuStatus = skuStatus; return this; }
        public Builder skuStoreStatus(String skuStoreStatus) { this.skuStoreStatus = skuStoreStatus; return this; }

        public ProductInfo build() {
            return new ProductInfo(
                skuNo, skuName, skuType, subDeptId, classId, subClassId, taxType,
                vendorId, dcType, holdOrderType,
                marketPrice, regularPrice, posPrice, cost,
                allowSales, isSystemSku, soFlag,
                freeDelivery, freeDeliveryShipping, allowDirectShipment, openPrice,
                vendorStatus, hasSkuCompany, stockAoh,
                skuStatus, skuStoreStatus
            );
        }
    }
}
