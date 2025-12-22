package com.tgfc.som.order.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * 稅別列舉
 *
 * 參考 CommonConstant.java
 * 值：0=零稅/1=應稅/2=免稅
 */
public enum TaxType {

    ZERO_TAX("0", "零稅", BigDecimal.ONE),
    TAXABLE("1", "應稅", new BigDecimal("1.05")),
    TAX_FREE("2", "免稅", BigDecimal.ONE);

    private final String code;
    private final String name;
    private final BigDecimal rate;

    TaxType(String code, String name, BigDecimal rate) {
        this.code = code;
        this.name = name;
        this.rate = rate;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public static Optional<TaxType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }

    public static TaxType fromCodeOrThrow(String code) {
        return fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("無效的稅別代碼: " + code));
    }

    /**
     * 計算含稅金額
     */
    public BigDecimal calculateTaxIncluded(BigDecimal amount) {
        return amount.multiply(rate);
    }

    /**
     * 計算稅額
     */
    public BigDecimal calculateTax(BigDecimal amount) {
        return amount.multiply(rate.subtract(BigDecimal.ONE));
    }

    /**
     * 是否需要計稅
     */
    public boolean isTaxable() {
        return this == TAXABLE;
    }
}
