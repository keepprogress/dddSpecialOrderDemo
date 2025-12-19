package com.tgfc.som.order.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 訂單回應
 *
 * @param orderId 訂單編號
 * @param projectId 專案代號
 * @param status 訂單狀態代碼
 * @param statusName 訂單狀態名稱
 * @param customer 客戶資訊
 * @param address 安運地址
 * @param storeId 出貨店
 * @param channelId 通路代號
 * @param lines 訂單行項列表
 * @param calculation 價格試算結果
 * @param createdAt 建立時間
 * @param updatedAt 更新時間
 */
public record OrderResponse(
    String orderId,
    String projectId,
    String status,
    String statusName,
    CustomerInfo customer,
    AddressInfo address,
    String storeId,
    String channelId,
    List<OrderLineResponse> lines,
    CalculationInfo calculation,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * 客戶資訊
     */
    public record CustomerInfo(
        String memberId,
        String cardType,
        String name,
        String gender,
        String phone,
        String cellPhone,
        String birthday,
        String contactName,
        String contactPhone,
        String vipType,
        String discountType,
        boolean isTempCard
    ) {}

    /**
     * 地址資訊
     */
    public record AddressInfo(
        String zipCode,
        String fullAddress
    ) {}

    /**
     * 價格試算資訊（簡化版）
     */
    public record CalculationInfo(
        int productTotal,
        int installationTotal,
        int deliveryTotal,
        int memberDiscount,
        int directShipmentTotal,
        int couponDiscount,
        int taxAmount,
        int grandTotal,
        boolean promotionSkipped,
        List<String> warnings,
        LocalDateTime calculatedAt
    ) {}
}
