package com.tgfc.som.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 建立訂單請求
 *
 * @param memberId 會員卡號（可選，使用臨時卡時為空）
 * @param customer 客戶資訊
 * @param address 安運地址
 * @param storeId 出貨店
 * @param channelId 通路代號
 * @param lines 初始行項（可選）
 */
public record CreateOrderRequest(
    String memberId,

    @NotNull(message = "客戶資訊不可為空")
    @Valid
    CustomerInfo customer,

    @NotNull(message = "安運地址不可為空")
    @Valid
    AddressInfo address,

    @NotBlank(message = "出貨店不可為空")
    String storeId,

    @NotBlank(message = "通路代號不可為空")
    String channelId,

    @Size(max = 50, message = "訂單行項不可超過 50 項")
    List<@Valid OrderLineInfo> lines
) {

    /**
     * 客戶資訊
     */
    public record CustomerInfo(
        String memberId,
        String cardType,
        @NotBlank(message = "姓名不可為空")
        String name,
        String gender,
        String phone,
        @NotBlank(message = "手機號碼不可為空")
        String cellPhone,
        String birthday,
        @NotBlank(message = "聯絡人不可為空")
        String contactName,
        @NotBlank(message = "聯絡電話不可為空")
        String contactPhone,
        String vipType,
        String discountType,
        boolean isTempCard
    ) {}

    /**
     * 地址資訊
     */
    public record AddressInfo(
        @NotBlank(message = "郵遞區號不可為空")
        @Size(min = 3, max = 5, message = "郵遞區號格式錯誤")
        String zipCode,

        @NotBlank(message = "地址不可為空")
        String fullAddress
    ) {}

    /**
     * 行項資訊（建立訂單時的初始行項）
     */
    public record OrderLineInfo(
        @NotBlank(message = "商品編號不可為空")
        String skuNo,

        int quantity,

        String deliveryMethod,

        String stockMethod
    ) {}
}
