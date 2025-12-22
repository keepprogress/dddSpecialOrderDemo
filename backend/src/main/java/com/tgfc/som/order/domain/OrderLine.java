package com.tgfc.som.order.domain;

import com.tgfc.som.order.domain.valueobject.DeliveryDetail;
import com.tgfc.som.order.domain.valueobject.InstallationDetail;
import com.tgfc.som.order.domain.valueobject.LineId;
import com.tgfc.som.order.domain.valueobject.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 訂單行項實體
 *
 * 職責：
 * - 管理單一商品的數量、價格、服務配置
 * - 記錄運送方式與備貨方式
 * - 記錄折扣明細
 */
public class OrderLine {

    // === Identity ===
    private final LineId id;
    private int serialNo;

    // === Product Reference ===
    private final String skuNo;
    private String skuName;

    // === Quantity & Pricing ===
    private int quantity;
    private Money unitPrice;
    private Money actualUnitPrice;
    private TaxType taxType;

    // === Delivery Configuration ===
    private DeliveryMethod deliveryMethod;
    private StockMethod stockMethod;
    private LocalDate deliveryDate;
    private String workTypeId;

    // === Installation & Delivery Details ===
    private InstallationDetail installationDetail;
    private DeliveryDetail deliveryDetail;
    private List<String> installationServiceTypes;
    private Money installationCost;
    private Money deliveryCost;

    // === Discounts ===
    private Money memberDisc;
    private Money bonusDisc;
    private Money couponDisc;

    // === Flags ===
    private boolean posAmtChangePrice;
    private boolean hasFreeInstall;

    /**
     * 建立新的訂單行項
     */
    public OrderLine(
            LineId id,
            int serialNo,
            String skuNo,
            String skuName,
            int quantity,
            Money unitPrice,
            TaxType taxType,
            DeliveryMethod deliveryMethod,
            StockMethod stockMethod
    ) {
        this.id = Objects.requireNonNull(id, "行項編號不可為空");
        this.serialNo = serialNo;
        this.skuNo = Objects.requireNonNull(skuNo, "商品編號不可為空");
        this.skuName = skuName;

        if (quantity <= 0) {
            throw new IllegalArgumentException("商品數量必須大於 0");
        }
        this.quantity = quantity;

        this.unitPrice = Objects.requireNonNull(unitPrice, "單價不可為空");
        this.actualUnitPrice = unitPrice; // 初始值與原價相同
        this.taxType = Objects.requireNonNull(taxType, "稅別不可為空");

        this.deliveryMethod = Objects.requireNonNull(deliveryMethod, "運送方式不可為空");
        this.stockMethod = Objects.requireNonNull(stockMethod, "備貨方式不可為空");

        // 驗證運送方式與備貨方式的相容性
        if (!deliveryMethod.isCompatibleWith(stockMethod)) {
            throw new IllegalArgumentException(
                String.format("運送方式 %s 與備貨方式 %s 不相容",
                    deliveryMethod.getName(), stockMethod.getName())
            );
        }

        // 初始化折扣金額
        this.memberDisc = Money.ZERO;
        this.bonusDisc = Money.ZERO;
        this.couponDisc = Money.ZERO;

        // 初始化安裝與運送
        this.installationDetail = InstallationDetail.none();
        this.deliveryDetail = DeliveryDetail.defaultDelivery();
        this.installationServiceTypes = new ArrayList<>();
        this.installationCost = Money.ZERO;
        this.deliveryCost = Money.ZERO;

        this.posAmtChangePrice = false;
        this.hasFreeInstall = false;
    }

    // === Business Methods ===

    /**
     * 計算小計金額
     */
    public Money getSubtotal() {
        return actualUnitPrice.multiply(quantity);
    }

    /**
     * 計算原價小計
     */
    public Money getOriginalSubtotal() {
        return unitPrice.multiply(quantity);
    }

    /**
     * 計算折扣金額（原價 - 實際價）
     */
    public Money getDiscountAmount() {
        return getOriginalSubtotal().subtract(getSubtotal());
    }

    /**
     * 計算稅額
     */
    public Money getTaxAmount() {
        return getSubtotal().calculateTax(taxType);
    }

    /**
     * 更新數量
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("商品數量必須大於 0");
        }
        this.quantity = newQuantity;
    }

    /**
     * 更新實際單價（套用折扣後）
     */
    public void updateActualUnitPrice(Money newActualUnitPrice) {
        Objects.requireNonNull(newActualUnitPrice, "實際單價不可為空");
        if (!newActualUnitPrice.equals(this.unitPrice)) {
            this.posAmtChangePrice = true;
        }
        this.actualUnitPrice = newActualUnitPrice;
    }

    /**
     * 更新運送方式
     */
    public void updateDeliveryMethod(DeliveryMethod newDeliveryMethod) {
        Objects.requireNonNull(newDeliveryMethod, "運送方式不可為空");
        if (!newDeliveryMethod.isCompatibleWith(this.stockMethod)) {
            throw new IllegalArgumentException(
                String.format("運送方式 %s 與備貨方式 %s 不相容",
                    newDeliveryMethod.getName(), this.stockMethod.getName())
            );
        }
        this.deliveryMethod = newDeliveryMethod;
    }

    /**
     * 更新備貨方式
     */
    public void updateStockMethod(StockMethod newStockMethod) {
        Objects.requireNonNull(newStockMethod, "備貨方式不可為空");
        if (!this.deliveryMethod.isCompatibleWith(newStockMethod)) {
            throw new IllegalArgumentException(
                String.format("運送方式 %s 與備貨方式 %s 不相容",
                    this.deliveryMethod.getName(), newStockMethod.getName())
            );
        }
        this.stockMethod = newStockMethod;
    }

    /**
     * 設定會員折扣
     */
    public void setMemberDisc(Money memberDisc) {
        this.memberDisc = memberDisc != null ? memberDisc : Money.ZERO;
    }

    /**
     * 設定紅利折抵
     */
    public void setBonusDisc(Money bonusDisc) {
        this.bonusDisc = bonusDisc != null ? bonusDisc : Money.ZERO;
    }

    /**
     * 設定優惠券折抵
     */
    public void setCouponDisc(Money couponDisc) {
        this.couponDisc = couponDisc != null ? couponDisc : Money.ZERO;
    }

    /**
     * 設定工種
     */
    public void setWorkTypeId(String workTypeId) {
        this.workTypeId = workTypeId;
    }

    /**
     * 設定預計出貨日
     */
    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    /**
     * 設定免安折扣標記
     */
    public void setHasFreeInstall(boolean hasFreeInstall) {
        this.hasFreeInstall = hasFreeInstall;
    }

    /**
     * 設定安裝明細
     */
    public void setInstallationDetail(InstallationDetail detail) {
        this.installationDetail = detail != null ? detail : InstallationDetail.none();
        this.workTypeId = detail != null ? detail.workTypeId() : null;
        if (detail != null && detail.hasInstallation()) {
            this.installationServiceTypes = new ArrayList<>(detail.serviceTypes());
            this.installationCost = detail.getTotalCost();
        }
    }

    /**
     * 設定運送明細
     */
    public void setDeliveryDetail(DeliveryDetail detail) {
        this.deliveryDetail = detail != null ? detail : DeliveryDetail.defaultDelivery();
        if (detail != null) {
            this.deliveryMethod = detail.method();
            this.deliveryCost = detail.deliveryCost();
            if (detail.scheduledDate() != null) {
                this.deliveryDate = detail.scheduledDate();
            }
        }
    }

    /**
     * 新增安裝服務類型
     */
    public void addInstallationServiceType(String serviceType) {
        if (serviceType != null && !installationServiceTypes.contains(serviceType)) {
            installationServiceTypes.add(serviceType);
        }
    }

    /**
     * 移除安裝服務類型
     */
    public void removeInstallationServiceType(String serviceType) {
        installationServiceTypes.remove(serviceType);
    }

    /**
     * 設定安裝費用
     */
    public void setInstallationCost(Money cost) {
        this.installationCost = cost != null ? cost : Money.ZERO;
    }

    /**
     * 設定運送費用
     */
    public void setDeliveryCost(Money cost) {
        this.deliveryCost = cost != null ? cost : Money.ZERO;
    }

    /**
     * 計算安裝費用總計
     */
    public Money getInstallationTotal() {
        return installationCost;
    }

    /**
     * 計算運送費用總計
     */
    public Money getDeliveryTotal() {
        return deliveryCost;
    }

    /**
     * 是否有安裝服務
     */
    public boolean hasInstallation() {
        return installationDetail != null && installationDetail.hasInstallation();
    }

    /**
     * 檢查是否為直送商品
     */
    public boolean isDirectShipment() {
        return deliveryMethod == DeliveryMethod.DIRECT_SHIPMENT;
    }

    /**
     * 檢查是否為應稅商品
     */
    public boolean isTaxable() {
        return taxType == TaxType.TAXABLE;
    }

    /**
     * 計算總折扣金額
     */
    public Money getTotalDiscount() {
        return memberDisc.add(bonusDisc).add(couponDisc);
    }

    // === Getters ===

    public LineId getId() {
        return id;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public String getSkuNo() {
        return skuNo;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getActualUnitPrice() {
        return actualUnitPrice;
    }

    public TaxType getTaxType() {
        return taxType;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public StockMethod getStockMethod() {
        return stockMethod;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public String getWorkTypeId() {
        return workTypeId;
    }

    public Money getMemberDisc() {
        return memberDisc;
    }

    public Money getBonusDisc() {
        return bonusDisc;
    }

    public Money getCouponDisc() {
        return couponDisc;
    }

    public boolean isPosAmtChangePrice() {
        return posAmtChangePrice;
    }

    public boolean isHasFreeInstall() {
        return hasFreeInstall;
    }

    public InstallationDetail getInstallationDetail() {
        return installationDetail;
    }

    public DeliveryDetail getDeliveryDetail() {
        return deliveryDetail;
    }

    public List<String> getInstallationServiceTypes() {
        return List.copyOf(installationServiceTypes);
    }

    public Money getInstallationCost() {
        return installationCost;
    }

    public Money getDeliveryCost() {
        return deliveryCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLine orderLine = (OrderLine) o;
        return Objects.equals(id, orderLine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderLine{" +
               "id=" + id +
               ", serialNo=" + serialNo +
               ", skuNo='" + skuNo + '\'' +
               ", skuName='" + skuName + '\'' +
               ", quantity=" + quantity +
               ", unitPrice=" + unitPrice +
               ", actualUnitPrice=" + actualUnitPrice +
               ", deliveryMethod=" + deliveryMethod +
               ", stockMethod=" + stockMethod +
               '}';
    }
}
