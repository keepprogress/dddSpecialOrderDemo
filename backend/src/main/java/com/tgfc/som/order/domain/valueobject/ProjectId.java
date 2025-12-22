package com.tgfc.som.order.domain.valueobject;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 專案代號值物件
 *
 * 格式: 店別(5碼) + 年(2碼) + 月日(4碼) + 流水號(5碼)
 * 範例: 1234524121800001
 */
public record ProjectId(String value) {

    private static final String FORMAT_PATTERN = "\\d{16}";

    public ProjectId {
        Objects.requireNonNull(value, "專案代號不可為空");
        if (!value.matches(FORMAT_PATTERN)) {
            throw new IllegalArgumentException("專案代號必須為 16 位數字");
        }
    }

    public static ProjectId of(String value) {
        return new ProjectId(value);
    }

    /**
     * 建立專案代號
     *
     * @param storeId  店別代號 (5 碼)
     * @param date     日期
     * @param sequence 流水號
     * @return 專案代號
     */
    public static ProjectId generate(String storeId, LocalDate date, int sequence) {
        Objects.requireNonNull(storeId, "店別代號不可為空");
        Objects.requireNonNull(date, "日期不可為空");

        if (!storeId.matches("\\d{5}")) {
            throw new IllegalArgumentException("店別代號必須為 5 碼數字");
        }

        String year = String.format("%02d", date.getYear() % 100);
        String monthDay = String.format("%02d%02d", date.getMonthValue(), date.getDayOfMonth());
        String seq = String.format("%05d", sequence);

        return new ProjectId(storeId + year + monthDay + seq);
    }

    /**
     * 取得店別代號 (前 5 碼)
     */
    public String getStoreId() {
        return value.substring(0, 5);
    }

    /**
     * 取得年份 (第 6-7 碼)
     */
    public String getYear() {
        return value.substring(5, 7);
    }

    /**
     * 取得月日 (第 8-11 碼)
     */
    public String getMonthDay() {
        return value.substring(7, 11);
    }

    /**
     * 取得流水號 (後 5 碼)
     */
    public String getSequence() {
        return value.substring(11, 16);
    }

    @Override
    public String toString() {
        return value;
    }
}
