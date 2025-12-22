package com.tgfc.som.order.domain.valueobject;

import java.util.Objects;

/**
 * 安運地址值物件
 */
public record DeliveryAddress(
        String zipCode,
        String fullAddress
) {

    public DeliveryAddress {
        Objects.requireNonNull(zipCode, "郵遞區號不可為空");
        Objects.requireNonNull(fullAddress, "地址不可為空");

        if (!zipCode.matches("\\d{3}")) {
            throw new IllegalArgumentException("郵遞區號必須為 3 碼數字");
        }

        if (fullAddress.isBlank()) {
            throw new IllegalArgumentException("地址不可為空白");
        }
    }

    public static DeliveryAddress of(String zipCode, String fullAddress) {
        return new DeliveryAddress(zipCode, fullAddress);
    }

    /**
     * 取得完整地址（含郵遞區號）
     */
    public String getFullAddressWithZip() {
        return zipCode + " " + fullAddress;
    }

    @Override
    public String toString() {
        return getFullAddressWithZip();
    }
}
