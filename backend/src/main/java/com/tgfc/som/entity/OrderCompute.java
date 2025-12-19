package com.tgfc.som.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 訂單試算實體 (TBL_ORDER_COMPUTE)
 *
 * 對應資料庫表：TBL_ORDER_COMPUTE
 */
public class OrderCompute {
    private Long id;
    private String orderId;
    private String computeType;
    private String computeName;
    private BigDecimal totalPrice;
    private BigDecimal discount;
    private BigDecimal actTotalPrice;
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId == null ? null : orderId.trim();
    }

    public String getComputeType() {
        return computeType;
    }

    public void setComputeType(String computeType) {
        this.computeType = computeType == null ? null : computeType.trim();
    }

    public String getComputeName() {
        return computeName;
    }

    public void setComputeName(String computeName) {
        this.computeName = computeName == null ? null : computeName.trim();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getActTotalPrice() {
        return actTotalPrice;
    }

    public void setActTotalPrice(BigDecimal actTotalPrice) {
        this.actTotalPrice = actTotalPrice;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
