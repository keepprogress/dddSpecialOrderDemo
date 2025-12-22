package com.tgfc.som.order.dto;

import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 更新訂單行項請求
 *
 * 用於設定安裝服務與運送配置
 *
 * @param quantity 數量（若需更新）
 * @param stockMethod 備貨方式代碼（X/Y）
 * @param deliveryMethod 運送方式代碼（N/D/V/C/F/P）
 * @param workTypeId 工種代碼
 * @param serviceTypes 安裝服務類型清單
 * @param receiverName 收件人姓名
 * @param receiverPhone 收件人電話
 * @param deliveryAddress 配送地址
 * @param deliveryZipCode 配送郵遞區號
 * @param deliveryNote 配送備註
 */
public record UpdateOrderLineRequest(
    @Min(value = 1, message = "數量必須大於 0")
    Integer quantity,

    String stockMethod,
    String deliveryMethod,
    String workTypeId,
    List<String> serviceTypes,

    String receiverName,
    String receiverPhone,
    String deliveryAddress,
    String deliveryZipCode,
    String deliveryNote
) {
    /**
     * 建立僅更新數量的請求
     */
    public static UpdateOrderLineRequest ofQuantity(int quantity) {
        return new UpdateOrderLineRequest(
            quantity, null, null, null, null,
            null, null, null, null, null
        );
    }

    /**
     * 建立安裝配置請求
     */
    public static UpdateOrderLineRequest ofInstallation(
            String workTypeId,
            List<String> serviceTypes) {
        return new UpdateOrderLineRequest(
            null, null, null, workTypeId, serviceTypes,
            null, null, null, null, null
        );
    }

    /**
     * 建立運送配置請求
     */
    public static UpdateOrderLineRequest ofDelivery(
            String stockMethod,
            String deliveryMethod,
            String workTypeId,
            String receiverName,
            String receiverPhone,
            String address,
            String zipCode) {
        return new UpdateOrderLineRequest(
            null, stockMethod, deliveryMethod, workTypeId, null,
            receiverName, receiverPhone, address, zipCode, null
        );
    }

    /**
     * 是否包含安裝配置
     */
    public boolean hasInstallationConfig() {
        return workTypeId != null || (serviceTypes != null && !serviceTypes.isEmpty());
    }

    /**
     * 是否包含運送配置
     */
    public boolean hasDeliveryConfig() {
        return deliveryMethod != null || receiverName != null || deliveryAddress != null;
    }
}
