package com.tgfc.som.catalog.dto;

import java.util.List;

/**
 * 商品銷售資格驗證回應
 *
 * 來源: product-query-spec.md, spec.md FR-010
 *
 * @param eligible 是否符合銷售資格
 * @param failureReason 失敗原因
 * @param failureLevel 失敗層級（1-8, 0 表示通過）
 * @param product 商品資訊（符合資格時返回）
 * @param orderability 可訂購性結果
 * @param availableServices 可用安裝服務列表
 * @param availableStockMethods 可用備貨方式列表
 * @param availableDeliveryMethods 可用運送方式列表
 * @param isLargeFurniture 是否為大型家具
 * @param isServiceSku 是否為外包服務商品 (026-888)
 */
public record EligibilityResponse(
    boolean eligible,
    String failureReason,
    int failureLevel,
    ProductInfo product,
    OrderabilityResult orderability,
    List<InstallationServiceInfo> availableServices,
    List<String> availableStockMethods,
    List<String> availableDeliveryMethods,
    boolean isLargeFurniture,
    boolean isServiceSku
) {

    /**
     * 建立成功回應
     */
    public static EligibilityResponse success(
            ProductInfo product,
            OrderabilityResult orderability,
            List<InstallationServiceInfo> services,
            List<String> stockMethods,
            List<String> deliveryMethods,
            boolean isLargeFurniture,
            boolean isServiceSku
    ) {
        return new EligibilityResponse(
            true, null, 0, product, orderability,
            services, stockMethods, deliveryMethods,
            isLargeFurniture, isServiceSku
        );
    }

    /**
     * 建立成功回應（簡化版，向後兼容）
     */
    public static EligibilityResponse success(
            ProductInfo product,
            List<InstallationServiceInfo> services,
            List<String> stockMethods,
            List<String> deliveryMethods
    ) {
        return new EligibilityResponse(
            true, null, 0, product, OrderabilityResult.canOrder(),
            services, stockMethods, deliveryMethods,
            false, false
        );
    }

    /**
     * 建立失敗回應
     */
    public static EligibilityResponse failure(int level, String reason) {
        return new EligibilityResponse(
            false, reason, level, null, null,
            List.of(), List.of(), List.of(),
            false, false
        );
    }

    // ========== 8-Layer 驗證失敗訊息 ==========

    /**
     * L1: 格式驗證失敗
     */
    public static EligibilityResponse formatNotValid() {
        return failure(1, "商品編號格式錯誤");
    }

    /**
     * L2: 商品不存在
     */
    public static EligibilityResponse productNotFound() {
        return failure(2, "商品不存在");
    }

    /**
     * L3: 系統商品不可銷售
     */
    public static EligibilityResponse systemProductNotAllowed() {
        return failure(3, "系統商品無法銷售");
    }

    /**
     * L4: 稅別無效
     */
    public static EligibilityResponse invalidTaxType() {
        return failure(4, "稅別設定錯誤");
    }

    /**
     * L5: 銷售禁止
     */
    public static EligibilityResponse salesProhibited() {
        return failure(5, "商品已禁止銷售");
    }

    /**
     * L6: 類別限制
     */
    public static EligibilityResponse categoryRestricted() {
        return failure(6, "商品類別限制銷售");
    }

    /**
     * L7: 廠商凍結（非 DC 商品）
     */
    public static EligibilityResponse vendorFrozen() {
        return failure(7, "廠商已凍結，商品無法訂購");
    }

    /**
     * L7: 無廠商 ID
     */
    public static EligibilityResponse noVendorId() {
        return failure(7, "該SKU未設定廠商ID，無法新增");
    }

    /**
     * L8: 不在採購組織內
     */
    public static EligibilityResponse notInPurchaseOrg() {
        return failure(8, "商品不在門市採購組織內");
    }

    /**
     * 安裝服務資訊
     */
    public record InstallationServiceInfo(
        String serviceType,
        String serviceName,
        String serviceSku,
        int basePrice,
        boolean isMandatory
    ) {}
}
