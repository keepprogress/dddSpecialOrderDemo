package com.tgfc.som.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 訂單明細實體 (TBL_ORDER_DETL)
 *
 * 對應資料庫表：TBL_ORDER_DETL
 */
public class OrderDetl {
    private String lineId;
    private String orderId;
    private Integer serialNo;
    private String skuNo;
    private String skuName;
    private Integer quantity;
    private BigDecimal posAmt;
    private BigDecimal actPosAmt;
    private String taxType;
    private String deliveryFlag;
    private String stockMethod;
    private Date deliveryDate;
    private BigDecimal memberDisc;
    private BigDecimal bonusDisc;
    private Date createdAt;
    private Date updatedAt;

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId == null ? null : lineId.trim();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId == null ? null : orderId.trim();
    }

    public Integer getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Integer serialNo) {
        this.serialNo = serialNo;
    }

    public String getSkuNo() {
        return skuNo;
    }

    public void setSkuNo(String skuNo) {
        this.skuNo = skuNo == null ? null : skuNo.trim();
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName == null ? null : skuName.trim();
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPosAmt() {
        return posAmt;
    }

    public void setPosAmt(BigDecimal posAmt) {
        this.posAmt = posAmt;
    }

    public BigDecimal getActPosAmt() {
        return actPosAmt;
    }

    public void setActPosAmt(BigDecimal actPosAmt) {
        this.actPosAmt = actPosAmt;
    }

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType == null ? null : taxType.trim();
    }

    public String getDeliveryFlag() {
        return deliveryFlag;
    }

    public void setDeliveryFlag(String deliveryFlag) {
        this.deliveryFlag = deliveryFlag == null ? null : deliveryFlag.trim();
    }

    public String getStockMethod() {
        return stockMethod;
    }

    public void setStockMethod(String stockMethod) {
        this.stockMethod = stockMethod == null ? null : stockMethod.trim();
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public BigDecimal getMemberDisc() {
        return memberDisc;
    }

    public void setMemberDisc(BigDecimal memberDisc) {
        this.memberDisc = memberDisc;
    }

    public BigDecimal getBonusDisc() {
        return bonusDisc;
    }

    public void setBonusDisc(BigDecimal bonusDisc) {
        this.bonusDisc = bonusDisc;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
