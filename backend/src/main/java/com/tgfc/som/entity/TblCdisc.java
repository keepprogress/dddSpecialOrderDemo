package com.tgfc.som.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TBL_CDISC Entity (會員折扣設定)
 *
 * 來源: docs/tables/TBL_CDISC.html
 *
 * 用於 12-Step 計價流程 Step 4-8: 會員折扣計算
 *
 * 複合主鍵: (DISCOUNT_ID, CHANNEL_ID, SUB_DEPT_ID, CLASS_ID, SUB_CLASS_ID, SKU_NO)
 */
public class TblCdisc {

    /**
     * 折扣代號 (PK)
     * DISCOUNT_ID VARCHAR2(10)
     */
    private String discountId;

    /**
     * 通路代號 (PK)
     * CHANNEL_ID VARCHAR2(5)
     */
    private String channelId;

    /**
     * 大類 (PK)
     * SUB_DEPT_ID VARCHAR2(3)
     */
    private String subDeptId;

    /**
     * 中類 (PK)
     * CLASS_ID VARCHAR2(3)
     */
    private String classId;

    /**
     * 小類 (PK)
     * SUB_CLASS_ID VARCHAR2(3)
     */
    private String subClassId;

    /**
     * 商品編號 (PK)
     * SKU_NO VARCHAR2(9)
     * 值為 '*' 表示該層級全部商品
     */
    private String skuNo;

    /**
     * 折扣率 (00-99)
     * DISC_PER VARCHAR2(2)
     * 例: 05 = 5%, 10 = 10%
     */
    private String discPer;

    /**
     * 折扣類型
     * DISC_TYPE VARCHAR2(1)
     * 0=Discounting, 1=Down Margin, 2=Cost Markup
     */
    private String discType;

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
    public TblCdisc() {
    }

    // Getters and Setters
    public String getDiscountId() {
        return discountId;
    }

    public void setDiscountId(String discountId) {
        this.discountId = discountId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getSubDeptId() {
        return subDeptId;
    }

    public void setSubDeptId(String subDeptId) {
        this.subDeptId = subDeptId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getSubClassId() {
        return subClassId;
    }

    public void setSubClassId(String subClassId) {
        this.subClassId = subClassId;
    }

    public String getSkuNo() {
        return skuNo;
    }

    public void setSkuNo(String skuNo) {
        this.skuNo = skuNo;
    }

    public String getDiscPer() {
        return discPer;
    }

    public void setDiscPer(String discPer) {
        this.discPer = discPer;
    }

    public String getDiscType() {
        return discType;
    }

    public void setDiscType(String discType) {
        this.discType = discType;
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
     * 取得折扣率 (百分比)
     */
    public int getDiscountPercentage() {
        if (discPer == null || discPer.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(discPer);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 取得折扣率 (小數)
     * 例: 5% -> 0.05
     */
    public double getDiscountRate() {
        return getDiscountPercentage() / 100.0;
    }

    /**
     * 是否為 Type 0 (Discounting)
     */
    public boolean isType0() {
        return "0".equals(discType);
    }

    /**
     * 是否為 Type 1 (Down Margin)
     */
    public boolean isType1() {
        return "1".equals(discType);
    }

    /**
     * 是否為 Type 2 (Cost Markup)
     */
    public boolean isType2() {
        return "2".equals(discType);
    }

    /**
     * 是否為全商品設定 (SKU_NO = '*')
     */
    public boolean isAllSkus() {
        return "*".equals(skuNo);
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
     * 檢查是否適用於指定商品
     *
     * @param targetSubDeptId 目標大類
     * @param targetClassId 目標中類
     * @param targetSubClassId 目標小類
     * @param targetSkuNo 目標商品編號
     * @return 是否適用
     */
    public boolean appliesTo(String targetSubDeptId, String targetClassId,
                             String targetSubClassId, String targetSkuNo) {
        // 檢查大類
        if (!"*".equals(subDeptId) && !subDeptId.equals(targetSubDeptId)) {
            return false;
        }
        // 檢查中類
        if (!"*".equals(classId) && !classId.equals(targetClassId)) {
            return false;
        }
        // 檢查小類
        if (!"*".equals(subClassId) && !subClassId.equals(targetSubClassId)) {
            return false;
        }
        // 檢查商品編號
        if (!"*".equals(skuNo) && !skuNo.equals(targetSkuNo)) {
            return false;
        }
        return true;
    }
}
