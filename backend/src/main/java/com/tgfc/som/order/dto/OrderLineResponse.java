package com.tgfc.som.order.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 訂單行項回應
 *
 * @param lineId 行項編號
 * @param serialNo 序號
 * @param skuNo 商品編號
 * @param skuName 商品名稱
 * @param quantity 數量
 * @param unitPrice 單價（原始 POS 價格）
 * @param actualUnitPrice 實際單價（折扣後）
 * @param deliveryMethod 運送方式代碼
 * @param deliveryMethodName 運送方式名稱
 * @param stockMethod 備貨方式代碼
 * @param stockMethodName 備貨方式名稱
 * @param taxType 稅別代碼
 * @param taxTypeName 稅別名稱
 * @param subtotal 小計
 * @param memberDisc 會員折扣金額
 * @param bonusDisc 紅利折抵金額
 * @param couponDisc 優惠券折抵金額
 * @param workTypeId 工種代碼
 * @param workTypeName 工種名稱
 * @param serviceTypes 安裝服務類型清單
 * @param hasInstallation 是否有安裝服務
 * @param installationCost 安裝費用
 * @param deliveryCost 運送費用
 * @param deliveryDate 預計出貨日
 * @param receiverName 收件人姓名
 * @param receiverPhone 收件人電話
 * @param deliveryAddress 配送地址
 */
public record OrderLineResponse(
    String lineId,
    int serialNo,
    String skuNo,
    String skuName,
    int quantity,
    int unitPrice,
    int actualUnitPrice,
    String deliveryMethod,
    String deliveryMethodName,
    String stockMethod,
    String stockMethodName,
    String taxType,
    String taxTypeName,
    int subtotal,
    int memberDisc,
    int bonusDisc,
    int couponDisc,
    // Installation & Delivery fields
    String workTypeId,
    String workTypeName,
    List<String> serviceTypes,
    boolean hasInstallation,
    int installationCost,
    int deliveryCost,
    LocalDate deliveryDate,
    String receiverName,
    String receiverPhone,
    String deliveryAddress
) {}
