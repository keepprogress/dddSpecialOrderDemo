package com.tgfc.som.catalog.dto;

import java.math.BigDecimal;

/**
 * 安裝服務回應 DTO
 *
 * 用於 API 回應，將 Money 物件轉換為數值
 */
public record InstallationServiceResponse(
    String serviceType,       // 服務類型代碼
    String serviceName,       // 服務名稱
    String serviceSku,        // 服務商品 SKU
    int basePrice,            // 基礎價格（數值）
    boolean isMandatory,      // 是否為必要安裝
    BigDecimal discountBase,  // 標安成本折數
    BigDecimal discountExtra  // 非標成本折數
) {
    /**
     * 從 InstallationService 建立回應
     */
    public static InstallationServiceResponse from(InstallationService service) {
        return new InstallationServiceResponse(
            service.serviceType(),
            service.serviceName(),
            service.serviceSku(),
            service.basePrice() != null ? service.basePrice().amount() : 0,
            service.isMandatory(),
            service.discountBase(),
            service.discountExtra()
        );
    }
}
