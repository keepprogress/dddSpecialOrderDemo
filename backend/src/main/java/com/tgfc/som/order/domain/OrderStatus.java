package com.tgfc.som.order.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 訂單狀態列舉
 *
 * 參考 DataExchangeItf.java
 * 值：1=草稿/2=報價/3=已付款/4=有效/5=結案/6=作廢
 */
public enum OrderStatus {

    DRAFT("1", "草稿"),
    QUOTATION("2", "報價"),
    PAID("3", "已付款"),
    ACTIVE("4", "有效"),
    CLOSED("5", "結案"),
    CANCELLED("6", "作廢");

    private final String code;
    private final String name;

    OrderStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<OrderStatus> fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst();
    }

    public static OrderStatus fromCodeOrThrow(String code) {
        return fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("無效的訂單狀態代碼: " + code));
    }

    /**
     * 檢查狀態轉換是否合法
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case DRAFT -> target == QUOTATION || target == ACTIVE;
            case QUOTATION -> target == ACTIVE || target == DRAFT;
            case PAID -> target == ACTIVE || target == CLOSED || target == CANCELLED;
            case ACTIVE -> target == PAID || target == CLOSED || target == CANCELLED;
            case CLOSED, CANCELLED -> false;
        };
    }

    /**
     * 是否可使用紅利點數
     */
    public boolean canUseBonusPoints() {
        return this == ACTIVE || this == PAID;
    }

    /**
     * 是否為最終狀態
     */
    public boolean isFinal() {
        return this == CLOSED || this == CANCELLED;
    }

    /**
     * 是否可編輯
     */
    public boolean isEditable() {
        return this == DRAFT || this == QUOTATION;
    }

    /**
     * 是否可修改訂單內容
     */
    public boolean canModify() {
        return this == DRAFT || this == QUOTATION;
    }
}
