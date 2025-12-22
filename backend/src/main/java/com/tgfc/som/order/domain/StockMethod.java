package com.tgfc.som.order.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 備貨方式列舉
 *
 * 對應資料庫欄位 TRADE_STATUS
 * 值：X=現貨/Y=訂購
 */
public enum StockMethod {

    IN_STOCK("X", "現貨"),
    PURCHASE_ORDER("Y", "訂購");

    private final String code;
    private final String name;

    StockMethod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<StockMethod> fromCode(String code) {
        return Arrays.stream(values())
                .filter(method -> method.code.equals(code))
                .findFirst();
    }

    public static StockMethod fromCodeOrThrow(String code) {
        return fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("無效的備貨方式代碼: " + code));
    }

    /**
     * 是否為現貨
     */
    public boolean isInStock() {
        return this == IN_STOCK;
    }

    /**
     * 是否需要訂購
     */
    public boolean isPurchaseOrder() {
        return this == PURCHASE_ORDER;
    }
}
