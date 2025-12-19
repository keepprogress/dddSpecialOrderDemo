package com.tgfc.som.catalog.dto;

/**
 * 商品資訊
 *
 * @param skuNo 商品編號
 * @param skuName 商品名稱
 * @param category 商品類別
 * @param categoryName 類別名稱
 * @param taxType 稅別代碼
 * @param marketPrice 市價
 * @param registeredPrice 登錄價
 * @param posPrice POS 價格
 * @param cost 成本
 * @param allowSales 是否允許銷售
 * @param holdOrder 是否暫停銷售
 * @param isSystemSku 是否為系統商品
 * @param isNegativeSku 是否為負數商品
 * @param freeDelivery 是否免運
 * @param freeDeliveryShipping 是否免運費
 * @param allowDirectShipment 是否允許直送
 * @param allowHomeDelivery 是否允許宅配
 */
public record ProductInfo(
    String skuNo,
    String skuName,
    String category,
    String categoryName,
    String taxType,
    int marketPrice,
    int registeredPrice,
    int posPrice,
    int cost,
    boolean allowSales,
    boolean holdOrder,
    boolean isSystemSku,
    boolean isNegativeSku,
    boolean freeDelivery,
    boolean freeDeliveryShipping,
    boolean allowDirectShipment,
    boolean allowHomeDelivery
) {

    /**
     * 是否可銷售（綜合判斷）
     */
    public boolean isEligibleForSale() {
        return allowSales && !holdOrder && !isSystemSku;
    }

    /**
     * 取得有效售價
     */
    public int getEffectivePrice() {
        return posPrice > 0 ? posPrice : (registeredPrice > 0 ? registeredPrice : marketPrice);
    }
}
