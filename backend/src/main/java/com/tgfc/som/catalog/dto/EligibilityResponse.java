package com.tgfc.som.catalog.dto;

import java.util.List;

/**
 * 商品銷售資格驗證回應
 *
 * @param eligible 是否符合銷售資格
 * @param failureReason 失敗原因
 * @param failureLevel 失敗層級（1-6, 0 表示通過）
 * @param product 商品資訊（符合資格時返回）
 * @param availableServices 可用安裝服務列表
 * @param availableStockMethods 可用備貨方式列表
 * @param availableDeliveryMethods 可用運送方式列表
 */
public record EligibilityResponse(
    boolean eligible,
    String failureReason,
    int failureLevel,
    ProductInfo product,
    List<InstallationServiceInfo> availableServices,
    List<String> availableStockMethods,
    List<String> availableDeliveryMethods
) {

    /**
     * 建立成功回應
     */
    public static EligibilityResponse success(
            ProductInfo product,
            List<InstallationServiceInfo> services,
            List<String> stockMethods,
            List<String> deliveryMethods
    ) {
        return new EligibilityResponse(
            true, null, 0, product, services, stockMethods, deliveryMethods
        );
    }

    /**
     * 建立失敗回應
     */
    public static EligibilityResponse failure(int level, String reason) {
        return new EligibilityResponse(
            false, reason, level, null, List.of(), List.of(), List.of()
        );
    }

    /**
     * 6-Layer 驗證失敗訊息
     */
    public static EligibilityResponse formatNotValid() {
        return failure(1, "商品編號格式錯誤");
    }

    public static EligibilityResponse productNotFound() {
        return failure(2, "商品不存在");
    }

    public static EligibilityResponse systemProductNotAllowed() {
        return failure(3, "系統商品無法銷售");
    }

    public static EligibilityResponse invalidTaxType() {
        return failure(4, "稅別設定錯誤");
    }

    public static EligibilityResponse salesProhibited() {
        return failure(5, "商品已禁止銷售");
    }

    public static EligibilityResponse categoryRestricted() {
        return failure(6, "商品類別限制銷售");
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
