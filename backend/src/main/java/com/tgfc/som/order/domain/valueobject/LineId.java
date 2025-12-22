package com.tgfc.som.order.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * 訂單行項編號值物件
 */
public record LineId(String value) {

    public LineId {
        Objects.requireNonNull(value, "行項編號不可為空");
        if (value.isBlank()) {
            throw new IllegalArgumentException("行項編號不可為空白");
        }
    }

    public static LineId of(String value) {
        return new LineId(value);
    }

    /**
     * 產生新的行項編號
     */
    public static LineId generate() {
        return new LineId(UUID.randomUUID().toString());
    }

    /**
     * 產生帶有序號的行項編號
     *
     * @param orderId  訂單編號
     * @param sequence 序號
     * @return 行項編號
     */
    public static LineId generate(OrderId orderId, int sequence) {
        Objects.requireNonNull(orderId, "訂單編號不可為空");
        return new LineId(orderId.value() + "-" + String.format("%03d", sequence));
    }

    @Override
    public String toString() {
        return value;
    }
}
