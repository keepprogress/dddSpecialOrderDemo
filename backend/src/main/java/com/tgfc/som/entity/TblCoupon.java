package com.tgfc.som.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TBL_COUPON Entity (折價券主檔)
 *
 * 來源: docs/tables/TBL_COUPON.html
 *
 * 用於 12-Step 計價流程 Step 6: 折價券折扣計算
 */
public class TblCoupon {

    /**
     * 折價券編號 (PK)
     * DSC_SKU VARCHAR2(9)
     */
    private String dscSku;

    /**
     * 折扣類型
     * DISC_TYPE VARCHAR2(1)
     * 0=固定金額折扣, 1=折扣率
     */
    private String discType;

    /**
     * 折扣金額/折扣率
     * DISC_AMT NUMBER(7)
     */
    private BigDecimal discAmt;

    /**
     * 最低門檻金額
     * MIN_COND NUMBER(7)
     */
    private BigDecimal minCond;

    /**
     * 生效日
     * START_DATE DATE
     */
    private LocalDate startDate;

    /**
     * 失效日
     * END_DATE DATE
     */
    private LocalDate endDate;

    /**
     * 最大使用數量
     * MAX_QTY NUMBER(5)
     */
    private Integer maxQty;

    /**
     * 已使用數量
     * USE_QTY NUMBER(5)
     */
    private Integer useQty;

    /**
     * 是否可用於 SO
     * SO_FLAG VARCHAR2(1)
     * Y=可用, N=不可用
     */
    private String soFlag;

    /**
     * 折價券名稱
     * DSC_NAME VARCHAR2(100)
     */
    private String dscName;

    /**
     * 建立時間
     * CREATED_DATE DATE
     */
    private LocalDateTime createdDate;

    /**
     * 更新時間
     * MODIFIED_DATE DATE
     */
    private LocalDateTime modifiedDate;

    // Constructors
    public TblCoupon() {
    }

    // Getters and Setters
    public String getDscSku() {
        return dscSku;
    }

    public void setDscSku(String dscSku) {
        this.dscSku = dscSku;
    }

    public String getDiscType() {
        return discType;
    }

    public void setDiscType(String discType) {
        this.discType = discType;
    }

    public BigDecimal getDiscAmt() {
        return discAmt;
    }

    public void setDiscAmt(BigDecimal discAmt) {
        this.discAmt = discAmt;
    }

    public BigDecimal getMinCond() {
        return minCond;
    }

    public void setMinCond(BigDecimal minCond) {
        this.minCond = minCond;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getMaxQty() {
        return maxQty;
    }

    public void setMaxQty(Integer maxQty) {
        this.maxQty = maxQty;
    }

    public Integer getUseQty() {
        return useQty;
    }

    public void setUseQty(Integer useQty) {
        this.useQty = useQty;
    }

    public String getSoFlag() {
        return soFlag;
    }

    public void setSoFlag(String soFlag) {
        this.soFlag = soFlag;
    }

    public String getDscName() {
        return dscName;
    }

    public void setDscName(String dscName) {
        this.dscName = dscName;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    // Business Methods

    /**
     * 是否為固定金額折扣
     */
    public boolean isFixedAmountDiscount() {
        return "0".equals(discType);
    }

    /**
     * 是否為折扣率
     */
    public boolean isPercentageDiscount() {
        return "1".equals(discType);
    }

    /**
     * 是否可用於 SO
     */
    public boolean isAvailableForSo() {
        return "Y".equals(soFlag);
    }

    /**
     * 是否在有效期內
     */
    public boolean isValid(LocalDate checkDate) {
        if (startDate != null && checkDate.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && checkDate.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    /**
     * 是否還有剩餘使用額度
     */
    public boolean hasRemainingQuota() {
        if (maxQty == null || maxQty <= 0) {
            return true; // 無限制
        }
        return useQty == null || useQty < maxQty;
    }

    /**
     * 是否達到最低門檻
     */
    public boolean meetsMinimumCondition(BigDecimal orderTotal) {
        if (minCond == null || minCond.compareTo(BigDecimal.ZERO) <= 0) {
            return true; // 無最低門檻
        }
        return orderTotal.compareTo(minCond) >= 0;
    }

    /**
     * 計算折扣金額
     */
    public BigDecimal calculateDiscount(BigDecimal originalAmount) {
        if (discAmt == null) {
            return BigDecimal.ZERO;
        }

        if (isFixedAmountDiscount()) {
            // 固定金額折扣
            return discAmt.min(originalAmount);
        } else {
            // 折扣率 (discAmt 代表折扣百分比, 如 10 = 10%)
            return originalAmount.multiply(discAmt)
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
    }
}
