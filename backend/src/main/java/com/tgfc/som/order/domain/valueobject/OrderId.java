package com.tgfc.som.order.domain.valueobject;

import java.util.Objects;

/**
 * 訂單編號值物件
 *
 * 格式: 10 位數字流水號
 * 起始: 3000000000
 */
public record OrderId(String value) {

    private static final String FORMAT_PATTERN = "\\d{10}";
    private static final long MIN_VALUE = 3000000000L;

    public OrderId {
        Objects.requireNonNull(value, "訂單編號不可為空");
        if (!value.matches(FORMAT_PATTERN)) {
            throw new IllegalArgumentException("訂單編號必須為 10 位數字");
        }
        long numericValue = Long.parseLong(value);
        if (numericValue < MIN_VALUE) {
            throw new IllegalArgumentException("訂單編號必須大於等於 " + MIN_VALUE);
        }
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    public static OrderId of(long value) {
        return new OrderId(String.format("%010d", value));
    }

    public long asLong() {
        return Long.parseLong(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
