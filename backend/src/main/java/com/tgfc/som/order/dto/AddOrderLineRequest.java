package com.tgfc.som.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 新增訂單行項請求
 *
 * @param skuNo 商品編號
 * @param quantity 數量
 * @param deliveryMethod 運送方式代碼（N/D/V/C/F/P）
 * @param stockMethod 備貨方式代碼（X/Y）
 */
public record AddOrderLineRequest(
    @NotBlank(message = "商品編號不可為空")
    String skuNo,

    @Min(value = 1, message = "數量必須大於 0")
    int quantity,

    @NotBlank(message = "運送方式不可為空")
    String deliveryMethod,

    @NotBlank(message = "備貨方式不可為空")
    String stockMethod
) {

    /**
     * 建立預設請求
     */
    public static AddOrderLineRequest of(String skuNo, int quantity) {
        return new AddOrderLineRequest(skuNo, quantity, "N", "X");
    }
}
