package com.tgfc.som.order.domain.valueobject;

import com.tgfc.som.order.domain.TaxType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額值物件
 *
 * 所有金額計算結果四捨五入取整數
 */
public record Money(int amount) {

    public static final Money ZERO = new Money(0);

    public Money {
        // 允許負數（折扣）
    }

    public static Money of(int amount) {
        return new Money(amount);
    }

    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "金額不可為空");
        return new Money(amount.setScale(0, RoundingMode.HALF_UP).intValue());
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "加數不可為空");
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "減數不可為空");
        return new Money(this.amount - other.amount);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount * multiplier);
    }

    public Money multiply(BigDecimal rate) {
        return multiply(rate, RoundingMode.HALF_UP);
    }

    public Money multiply(BigDecimal rate, RoundingMode roundingMode) {
        Objects.requireNonNull(rate, "比例不可為空");
        BigDecimal result = BigDecimal.valueOf(amount).multiply(rate);
        return new Money(result.setScale(0, roundingMode).intValue());
    }

    public Money negate() {
        return new Money(-this.amount);
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public boolean isGreaterThan(Money other) {
        Objects.requireNonNull(other, "比較對象不可為空");
        return this.amount > other.amount;
    }

    public boolean isLessThan(Money other) {
        Objects.requireNonNull(other, "比較對象不可為空");
        return this.amount < other.amount;
    }

    public Money min(Money other) {
        Objects.requireNonNull(other, "比較對象不可為空");
        return this.amount <= other.amount ? this : other;
    }

    public Money max(Money other) {
        Objects.requireNonNull(other, "比較對象不可為空");
        return this.amount >= other.amount ? this : other;
    }

    /**
     * 確保金額不為負數（用於封頂處理）
     */
    public Money ensureNonNegative() {
        return isNegative() ? ZERO : this;
    }

    /**
     * 計算稅額
     *
     * @param taxType 稅別
     * @return 稅額（應稅時為含稅價 - 原價）
     */
    public Money calculateTax(TaxType taxType) {
        if (taxType == null || taxType == TaxType.TAX_FREE || taxType == TaxType.ZERO_TAX) {
            return ZERO;
        }
        // 應稅: 稅額 = 含稅價 - 未稅價 = 原價 * 0.05 / 1.05 (四捨五入)
        // 假設金額已為含稅價，稅額 = 金額 - (金額 / 1.05)
        BigDecimal taxRate = taxType.getRate();
        BigDecimal untaxedAmount = BigDecimal.valueOf(amount).divide(taxRate, 0, RoundingMode.HALF_UP);
        return new Money(amount - untaxedAmount.intValue());
    }

    /**
     * 計算含稅金額
     *
     * @param taxType 稅別
     * @return 含稅金額
     */
    public Money withTax(TaxType taxType) {
        if (taxType == null || taxType == TaxType.TAX_FREE || taxType == TaxType.ZERO_TAX) {
            return this;
        }
        return multiply(taxType.getRate());
    }

    @Override
    public String toString() {
        return String.valueOf(amount);
    }
}
