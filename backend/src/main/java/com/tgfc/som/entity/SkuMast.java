package com.tgfc.som.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品主檔實體 (TBL_SKU_MAST)
 *
 * 對應資料庫表：TBL_SKU_MAST
 */
public class SkuMast {
    private String skuNo;
    private String skuName;
    private String category;
    private String taxType;
    private BigDecimal marketPrice;
    private BigDecimal registeredPrice;
    private BigDecimal posPrice;
    private BigDecimal cost;
    private String allowSales;
    private String holdOrder;
    private String isSystemSku;
    private String isNegativeSku;
    private String freeDelivery;
    private String freeDeliveryShipping;
    private String allowDirectShipment;
    private String allowHomeDelivery;
    private Date createdAt;
    private Date updatedAt;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType == null ? null : taxType.trim();
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(BigDecimal marketPrice) {
        this.marketPrice = marketPrice;
    }

    public BigDecimal getRegisteredPrice() {
        return registeredPrice;
    }

    public void setRegisteredPrice(BigDecimal registeredPrice) {
        this.registeredPrice = registeredPrice;
    }

    public BigDecimal getPosPrice() {
        return posPrice;
    }

    public void setPosPrice(BigDecimal posPrice) {
        this.posPrice = posPrice;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getAllowSales() {
        return allowSales;
    }

    public void setAllowSales(String allowSales) {
        this.allowSales = allowSales == null ? null : allowSales.trim();
    }

    public String getHoldOrder() {
        return holdOrder;
    }

    public void setHoldOrder(String holdOrder) {
        this.holdOrder = holdOrder == null ? null : holdOrder.trim();
    }

    public String getIsSystemSku() {
        return isSystemSku;
    }

    public void setIsSystemSku(String isSystemSku) {
        this.isSystemSku = isSystemSku == null ? null : isSystemSku.trim();
    }

    public String getIsNegativeSku() {
        return isNegativeSku;
    }

    public void setIsNegativeSku(String isNegativeSku) {
        this.isNegativeSku = isNegativeSku == null ? null : isNegativeSku.trim();
    }

    public String getFreeDelivery() {
        return freeDelivery;
    }

    public void setFreeDelivery(String freeDelivery) {
        this.freeDelivery = freeDelivery == null ? null : freeDelivery.trim();
    }

    public String getFreeDeliveryShipping() {
        return freeDeliveryShipping;
    }

    public void setFreeDeliveryShipping(String freeDeliveryShipping) {
        this.freeDeliveryShipping = freeDeliveryShipping == null ? null : freeDeliveryShipping.trim();
    }

    public String getAllowDirectShipment() {
        return allowDirectShipment;
    }

    public void setAllowDirectShipment(String allowDirectShipment) {
        this.allowDirectShipment = allowDirectShipment == null ? null : allowDirectShipment.trim();
    }

    public String getAllowHomeDelivery() {
        return allowHomeDelivery;
    }

    public void setAllowHomeDelivery(String allowHomeDelivery) {
        this.allowHomeDelivery = allowHomeDelivery == null ? null : allowHomeDelivery.trim();
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

    // 業務方法
    public boolean isAllowSales() {
        return "Y".equals(allowSales);
    }

    public boolean isHoldOrder() {
        return "Y".equals(holdOrder);
    }

    public boolean isSystemSku() {
        return "Y".equals(isSystemSku);
    }

    public boolean isNegativeSku() {
        return "Y".equals(isNegativeSku);
    }

    public boolean isFreeDelivery() {
        return "Y".equals(freeDelivery);
    }

    public boolean isFreeDeliveryShipping() {
        return "Y".equals(freeDeliveryShipping);
    }

    public boolean canDirectShipment() {
        return "Y".equals(allowDirectShipment);
    }

    public boolean canHomeDelivery() {
        return "Y".equals(allowHomeDelivery);
    }
}
